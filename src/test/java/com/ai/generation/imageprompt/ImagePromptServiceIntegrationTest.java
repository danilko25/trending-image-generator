package com.ai.generation.imageprompt;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.exception.ImagePromptGenerationException;
import com.ai.generation.imageprompt.dto.ImagePromptSupportingTagsProjection;
import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import com.ai.generation.niche.Niche;
import com.ai.generation.niche.repository.NicheRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class ImagePromptServiceIntegrationTest {

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
    private ImagePromptService imagePromptService;
    @Autowired
    private ImagePromptRepository imagePromptRepository;
    @Autowired
    private ImageStyleRepository imageStyleRepository;
    @Autowired
    private NicheRepository nicheRepository;

    @MockitoBean
    private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    private ImageStyle style;
    private Niche niche;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        style = new ImageStyle();
        style.setName("style-" + UUID.randomUUID());
        style.setSystemPrompt("You are a prompt engineer for vector art");
        style.setActive(true);
        style = imageStyleRepository.save(style);

        niche = nicheRepository.save(new Niche("cozy autumn drinks " + UUID.randomUUID()));
    }

    // ---------- generateImagePrompt ----------

    @Test
    void generateImagePrompt_shouldSaveAllFieldsFromAiResponse() {
        when(callSpec.entity(ChatImagePromptResponse.class)).thenReturn(aiResponse());

        var result = imagePromptService.generateImagePrompt(style.getId(), niche.getId());

        assertThat(result.getId()).isNotNull();
        var saved = imagePromptRepository.findWithStyleById(result.getId()).orElseThrow();
        assertThat(saved.getValue()).isEqualTo("a cup of pumpkin latte, flat vector");
        assertThat(saved.getNegativeValue()).isEqualTo("blurry, text");
        assertThat(saved.getTitle()).isEqualTo("Cozy Latte");
        assertThat(saved.getMainTag()).isEqualTo("latte");
        assertThat(saved.getDescription()).isEqualTo("Warm autumn mug design");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStyle().getId()).isEqualTo(style.getId());

        var tags = imagePromptRepository.findAllById(saved.getId(), ImagePromptSupportingTagsProjection.class);
        assertThat(tags).extracting(ImagePromptSupportingTagsProjection::getSupportingTags)
                .containsExactlyInAnyOrder("autumn", "coffee", "cozy");
    }

    @Test
    void generateImagePrompt_shouldSendStylePromptAsSystemAndNicheKeywordAsUser() {
        when(callSpec.entity(ChatImagePromptResponse.class)).thenReturn(aiResponse());

        imagePromptService.generateImagePrompt(style.getId(), niche.getId());

        verify(requestSpec).system(style.getSystemPrompt());
        verify(requestSpec).user(niche.getKeyword());
    }

    @Test
    void generateImagePrompt_shouldThrowAndSaveNothingWhenAiReturnsNull() {
        when(callSpec.entity(ChatImagePromptResponse.class)).thenReturn(null);
        var before = imagePromptRepository.count();

        assertThatThrownBy(() -> imagePromptService.generateImagePrompt(style.getId(), niche.getId()))
                .isInstanceOf(ImagePromptGenerationException.class)
                .hasMessageContaining(niche.getKeyword())
                .hasMessageContaining(style.getName());

        assertThat(imagePromptRepository.count()).isEqualTo(before);
    }

    @Test
    void generateImagePrompt_shouldWrapAiExceptionAndSaveNothing() {
        when(callSpec.entity(ChatImagePromptResponse.class)).thenThrow(new RuntimeException("model timeout"));
        var before = imagePromptRepository.count();

        assertThatThrownBy(() -> imagePromptService.generateImagePrompt(style.getId(), niche.getId()))
                .isInstanceOf(ImagePromptGenerationException.class)
                .hasRootCauseMessage("model timeout");

        assertThat(imagePromptRepository.count()).isEqualTo(before);
    }

    @Test
    void generateImagePrompt_shouldThrowNotFoundForUnknownStyleWithoutCallingAi() {
        assertThatThrownBy(() -> imagePromptService.generateImagePrompt(UUID.randomUUID(), niche.getId()))
                .isInstanceOf(EntityNotFoundException.class);

        verify(chatClient, org.mockito.Mockito.never()).prompt();
    }

    @Test
    void generateImagePrompt_shouldThrowNotFoundForUnknownNiche() {
        assertThatThrownBy(() -> imagePromptService.generateImagePrompt(style.getId(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- getById / getWithStyleById ----------

    @Test
    void getById_shouldReturnSavedPromptOrThrowNotFound() {
        var prompt = savePrompt();

        assertThat(imagePromptService.getById(prompt.getId()).getId()).isEqualTo(prompt.getId());
        assertThatThrownBy(() -> imagePromptService.getById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getWithStyleById_shouldFetchStyleEagerly() {
        var prompt = savePrompt();

        var result = imagePromptService.getWithStyleById(prompt.getId());

        // loaded outside of a transaction, so the style must already be initialized by the join fetch
        assertThat(Hibernate.isInitialized(result.getStyle())).isTrue();
        assertThat(result.getStyle().getSystemPrompt()).isEqualTo(style.getSystemPrompt());
        assertThatThrownBy(() -> imagePromptService.getWithStyleById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- helpers ----------

    private ChatImagePromptResponse aiResponse() {
        return new ChatImagePromptResponse(
                "a cup of pumpkin latte, flat vector",
                "blurry, text",
                "Cozy Latte",
                "latte",
                List.of("autumn", "coffee", "cozy"),
                "Warm autumn mug design"
        );
    }

    private ImagePrompt savePrompt() {
        var prompt = new ImagePrompt(aiResponse(), niche, style);
        return imagePromptRepository.save(prompt);
    }
}
