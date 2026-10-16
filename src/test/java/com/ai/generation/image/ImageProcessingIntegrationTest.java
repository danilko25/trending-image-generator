package com.ai.generation.image;

import com.ai.generation.design.Design;
import com.ai.generation.design.DesignRepository;
import com.ai.generation.exception.DesignGenerationException;
import com.ai.generation.image.background.RemoveBackgroundClient;
import com.ai.generation.image.background.RemoveBackgroundService;
import com.ai.generation.image.background.dto.RemoveBackgroundRequest;
import com.ai.generation.image.processing.ProcessedImageRepository;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.processing.entity.ProcessedImage;
import com.ai.generation.image.processing.entity.ProcessingStatus;
import com.ai.generation.image.processing.entity.ProcessingType;
import com.ai.generation.image.upscaling.ImageUpscaleClient;
import com.ai.generation.image.upscaling.ImageUpscaleService;
import com.ai.generation.image.upscaling.dto.UpscaleRequest;
import com.ai.generation.imageprompt.ImagePrompt;
import com.ai.generation.imageprompt.ImagePromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class ImageProcessingIntegrationTest {

    private static final String ORIGINAL_URL = "https://example.com/original.png";
    private static final String NO_BG_URL = "https://example.com/no-bg.png";
    private static final String UPSCALED_URL = "https://example.com/upscaled.png";

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("generation_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RemoveBackgroundService removeBackgroundService;
    @Autowired
    private ImageUpscaleService imageUpscaleService;
    @Autowired
    private DesignRepository designRepository;
    @Autowired
    private ImagePromptRepository imagePromptRepository;
    @Autowired
    private ProcessedImageRepository processedImageRepository;
    @Autowired
    @Qualifier("apiConnectionLimiter")
    private Semaphore apiConnectionLimiter;

    @Value("${parallel.connections.count}")
    private int initialPermits;

    @MockitoBean
    private RemoveBackgroundClient removeBackgroundClient;
    @MockitoBean
    private ImageUpscaleClient imageUpscaleClient;

    @BeforeEach
    void resetLimiter() {
        // the semaphore is a shared singleton: a leak in one test must not hang the others
        apiConnectionLimiter.drainPermits();
        apiConnectionLimiter.release(initialPermits);
    }

    // ---------- remove background ----------

    @Test
    void removeBackground_shouldCompleteAndSaveResultUrl() {
        var design = createDesign();
        mockRemoveBackgroundSuccess();

        var dto = removeBackgroundService.removeBackground(design.getId());

        assertThat(dto.getId()).isEqualTo(design.getId());
        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);

        var processed = find(design.getId(), ProcessingType.REMOVE_BACKGROUND);
        assertThat(processed.getResultUrl()).isEqualTo(NO_BG_URL);
        verify(removeBackgroundClient).removeBackground(
                argThat(r -> ORIGINAL_URL.equals(r.imagePath())));
    }

    @Test
    void removeBackground_shouldMarkFailedWhenClientThrows() {
        var design = createDesign();
        when(removeBackgroundClient.removeBackground(any()))
                .thenThrow(new RuntimeException("fal.ai is down"));

        removeBackgroundService.removeBackground(design.getId());

        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.FAILED);
        var processed = find(design.getId(), ProcessingType.REMOVE_BACKGROUND);
        assertThat(processed.getErrorMessage()).contains("fal.ai is down");
        assertThat(processed.getResultUrl()).isNull();
    }

    @Test
    void removeBackground_shouldAllowRetryAfterFailure() {
        var design = createDesign();
        when(removeBackgroundClient.removeBackground(any()))
                .thenThrow(new RuntimeException("temporary error"))
                .thenAnswer(inv -> new ProcessedImageResponse(
                        ((RemoveBackgroundRequest) inv.getArgument(0)).imageProcessingId(), NO_BG_URL));

        removeBackgroundService.removeBackground(design.getId());
        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.FAILED);
        var firstId = find(design.getId(), ProcessingType.REMOVE_BACKGROUND).getId();

        removeBackgroundService.removeBackground(design.getId());
        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);

        var processed = find(design.getId(), ProcessingType.REMOVE_BACKGROUND);
        assertThat(processed.getId()).isEqualTo(firstId); // reused, not duplicated
        assertThat(processed.getResultUrl()).isEqualTo(NO_BG_URL);
    }

    @Test
    void removeBackground_shouldRejectRestartWhenAlreadyCompleted() {
        var design = createDesign();
        mockRemoveBackgroundSuccess();
        removeBackgroundService.removeBackground(design.getId());
        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);

        assertThatThrownBy(() -> removeBackgroundService.removeBackground(design.getId()))
                .isInstanceOf(DesignGenerationException.class)
                .hasMessageContaining("cannot be started");
    }

    // ---------- upscale ----------

    @Test
    void upscale_shouldCompleteAndSaveResultUrl() {
        var design = createDesign();
        mockUpscaleSuccess();

        imageUpscaleService.upscaleOriginalImage(design.getId());

        awaitStatus(design.getId(), ProcessingType.UPSCALE, ProcessingStatus.COMPLETED);
        assertThat(find(design.getId(), ProcessingType.UPSCALE).getResultUrl()).isEqualTo(UPSCALED_URL);
        verify(imageUpscaleClient).upscale(argThat(r -> ORIGINAL_URL.equals(r.imagePath())));
    }

    @Test
    void upscale_shouldMarkFailedWhenClientThrows() {
        var design = createDesign();
        when(imageUpscaleClient.upscale(any())).thenThrow(new RuntimeException("upscale error"));

        imageUpscaleService.upscaleOriginalImage(design.getId());

        awaitStatus(design.getId(), ProcessingType.UPSCALE, ProcessingStatus.FAILED);
        assertThat(find(design.getId(), ProcessingType.UPSCALE).getErrorMessage())
                .contains("upscale error");
    }

    // ---------- upscale of removed background ----------

    @Test
    void upscaleRemovedBackground_shouldFailWhenBackgroundWasNotRemovedYet() {
        var design = createDesign();

        imageUpscaleService.upscaleRemovedBackgroundImage(design.getId());

        awaitStatus(design.getId(), ProcessingType.UPSCALE_REMOVE_BACKGROUND, ProcessingStatus.FAILED);
        assertThat(find(design.getId(), ProcessingType.UPSCALE_REMOVE_BACKGROUND).getErrorMessage())
                .contains("REMOVE_BACKGROUND");
        verifyNoInteractions(imageUpscaleClient);
    }

    @Test
    void upscaleRemovedBackground_shouldUpscaleResultOfBackgroundRemoval() {
        var design = createDesign();
        mockRemoveBackgroundSuccess();
        mockUpscaleSuccess();

        removeBackgroundService.removeBackground(design.getId());
        awaitStatus(design.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);

        imageUpscaleService.upscaleRemovedBackgroundImage(design.getId());
        awaitStatus(design.getId(), ProcessingType.UPSCALE_REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);

        // upscale must take the image without background, not the original
        verify(imageUpscaleClient).upscale(argThat(r -> NO_BG_URL.equals(r.imagePath())));
        verify(imageUpscaleClient, never()).upscale(argThat(r -> ORIGINAL_URL.equals(r.imagePath())));
        assertThat(find(design.getId(), ProcessingType.UPSCALE_REMOVE_BACKGROUND).getResultUrl())
                .isEqualTo(UPSCALED_URL);
    }

    // ---------- semaphore ----------

    private static final String FAIL_URL = "https://example.com/fail.png";

    @Test
    void apiConnectionLimiter_shouldReleaseAllPermitsAfterSuccessAndFailure() {
        var okDesign = createDesign(ORIGINAL_URL);
        var failDesign = createDesign(FAIL_URL);
        mockRemoveBackgroundSuccess();
        doAnswer(inv -> {
            var request = (UpscaleRequest) inv.getArgument(0);
            if (FAIL_URL.equals(request.imagePath())) {
                throw new RuntimeException("boom");
            }
            return new ProcessedImageResponse(request.designId(), UPSCALED_URL);
        }).when(imageUpscaleClient).upscale(any());

        removeBackgroundService.removeBackground(okDesign.getId());
        imageUpscaleService.upscaleOriginalImage(okDesign.getId());                // success
        imageUpscaleService.upscaleOriginalImage(failDesign.getId());              // client throws
        imageUpscaleService.upscaleRemovedBackgroundImage(failDesign.getId());     // no removed bg

        awaitStatus(okDesign.getId(), ProcessingType.REMOVE_BACKGROUND, ProcessingStatus.COMPLETED);
        awaitStatus(okDesign.getId(), ProcessingType.UPSCALE, ProcessingStatus.COMPLETED);
        awaitStatus(failDesign.getId(), ProcessingType.UPSCALE, ProcessingStatus.FAILED);
        awaitStatus(failDesign.getId(), ProcessingType.UPSCALE_REMOVE_BACKGROUND, ProcessingStatus.FAILED);

        // Fails until finally { release() } is added to both upscale processors
        assertThat(apiConnectionLimiter.availablePermits()).isEqualTo(initialPermits);
    }

    // ---------- helpers ----------

    private Design createDesign() {
        return createDesign(ORIGINAL_URL);
    }

    private Design createDesign(String url) {
        var prompt = new ImagePrompt();
        prompt.setTitle("test title");
        prompt.setCreatedAt(Instant.now());
        prompt = imagePromptRepository.save(prompt);
        return designRepository.save(new Design(prompt, url));
    }

    private void mockRemoveBackgroundSuccess() {
        when(removeBackgroundClient.removeBackground(any())).thenAnswer(inv ->
                new ProcessedImageResponse(
                        ((RemoveBackgroundRequest) inv.getArgument(0)).imageProcessingId(), NO_BG_URL));
    }

    private void mockUpscaleSuccess() {
        when(imageUpscaleClient.upscale(any())).thenAnswer(inv ->
                new ProcessedImageResponse(
                        ((UpscaleRequest) inv.getArgument(0)).designId(), UPSCALED_URL));
    }

    private ProcessedImage find(UUID designId, ProcessingType type) {
        return processedImageRepository.findByDesignIdAndType(designId, type).orElseThrow();
    }

    private void awaitStatus(UUID designId, ProcessingType type, ProcessingStatus expected) {
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(
                        processedImageRepository.findByDesignIdAndType(designId, type)
                                .map(ProcessedImage::getStatus))
                        .contains(expected));
    }
}