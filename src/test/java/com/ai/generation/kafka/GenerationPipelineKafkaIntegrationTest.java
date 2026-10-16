package com.ai.generation.kafka;

import com.ai.generation.design.DesignRepository;
import com.ai.generation.design.generation.DesignGeneratorClient;
import com.ai.generation.design.generation.dto.GenerateDesignRequest;
import com.ai.generation.design.generation.dto.GenerateDesignResponse;
import com.ai.generation.generation.GenerationRequest;
import com.ai.generation.generation.GenerationService;
import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.entity.GenerationSubtask;
import com.ai.generation.generationtask.entity.GenerationTask;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import com.ai.generation.generationtask.repository.GenerationSubtaskRepository;
import com.ai.generation.generationtask.repository.GenerationTaskRepository;
import com.ai.generation.imageprompt.ChatImagePromptResponse;
import com.ai.generation.imageprompt.ImagePromptRepository;
import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import com.ai.generation.niche.Niche;
import com.ai.generation.niche.repository.NicheRepository;
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
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the Kafka pipeline:
 * task topic -> subtasks -> prompt topic -> image topic -> COMPLETED.
 * External services (ChatClient, image generator) are mocked, everything else is real.
 */
@SpringBootTest
@Testcontainers
class GenerationPipelineKafkaIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("generation_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.1");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // do not miss messages sent before the listener gets its partitions assigned
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        // keep retries fast
        registry.add("spring.kafka.consumer.backoff.ms", () -> "200");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
    }

    @Autowired
    private GenerationService generationService;
    @Autowired
    private KafkaMessageProducer kafkaMessageProducer;
    @Autowired
    private GenerationTaskRepository generationTaskRepository;
    @Autowired
    private GenerationSubtaskRepository generationSubtaskRepository;
    @Autowired
    private DesignRepository designRepository;
    @Autowired
    private ImagePromptRepository imagePromptRepository;
    @Autowired
    private NicheRepository nicheRepository;
    @Autowired
    private ImageStyleRepository imageStyleRepository;

    @MockitoBean
    private ChatClient chatClient;
    @MockitoBean
    private DesignGeneratorClient designGeneratorClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        designRepository.deleteAll();
        generationSubtaskRepository.deleteAll();
        generationTaskRepository.deleteAll();
        imagePromptRepository.deleteAll();
        nicheRepository.deleteAll();
        imageStyleRepository.deleteAll();

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        when(designGeneratorClient.generateImage(any())).thenAnswer(inv -> {
            var request = (GenerateDesignRequest) inv.getArgument(0);
            return new GenerateDesignResponse("https://example.com/" + UUID.randomUUID() + ".png", request.promptId());
        });
    }

    @Test
    void shouldRunFullPipelineForEveryStyle() {
        var style1 = saveStyle("Vector");
        var style2 = saveStyle("Retro");
        when(callSpec.entity(ChatImagePromptResponse.class)).thenReturn(aiResponse());

        generationService.startGeneration(request("cute cat lover", style1, style2));

        awaitTaskStatus(GenerationStatus.COMPLETED);

        var task = singleTask();
        assertThat(task.getFinishedAt()).isNotNull();

        var subtasks = generationSubtaskRepository.findAll();
        assertThat(subtasks).hasSize(2);
        assertThat(subtasks).allSatisfy(subtask -> {
            assertThat(subtask.getGenerationTaskId()).isEqualTo(task.getId());
            assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.COMPLETED);
            assertThat(subtask.getImagePromptId()).isNotNull();
            assertThat(subtask.getDesignId()).isNotNull();
            assertThat(subtask.getFinishedAt()).isNotNull();
        });
        assertThat(subtasks).extracting(GenerationSubtask::getImageStyleId)
                .containsExactlyInAnyOrder(style1.getId(), style2.getId());

        assertThat(nicheRepository.findAll()).extracting(Niche::getKeyword).containsExactly("cute cat lover");
        assertThat(designRepository.count()).isEqualTo(2);
        verify(requestSpec, times(2)).call();
        verify(designGeneratorClient, times(2)).generateImage(any());
    }

    // ---------- duplicate delivery ----------

    @Test
    void shouldIgnoreDuplicateDeliveryOfAlreadyProcessedMessages() {
        var style = saveStyle("Vector");
        when(callSpec.entity(ChatImagePromptResponse.class)).thenReturn(aiResponse());
        generationService.startGeneration(request("cute cat lover", style));
        awaitTaskStatus(GenerationStatus.COMPLETED);

        var taskId = singleTask().getId();
        var subtaskId = generationSubtaskRepository.findAll().getFirst().getId();

        // at-least-once delivery: the same messages arrive again
        kafkaMessageProducer.sendSubtaskTaskGeneration(taskId);
        kafkaMessageProducer.sendImagePromptGeneration(subtaskId);
        kafkaMessageProducer.sendImageGeneration(subtaskId);

        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(generationSubtaskRepository.count()).isEqualTo(1);
            assertThat(designRepository.count()).isEqualTo(1);
            verify(requestSpec, times(1)).call();
            verify(designGeneratorClient, times(1)).generateImage(any());
        });
    }

    // ---------- retries / failures ----------
    // NOTE: both tests below fail with the current listeners. The first attempt moves the subtask to
    // PROMPT_GENERATING and then throws. The retry sees status != PENDING, treats it as a duplicate,
    // returns silently and the subtask stays in PROMPT_GENERATING forever (never reaches DLT).

    @Test
    void shouldRetryAfterTransientAiFailureAndCompleteSubtask() {
        var style = saveStyle("Vector");
        when(callSpec.entity(ChatImagePromptResponse.class))
                .thenThrow(new RuntimeException("temporary model error"))
                .thenReturn(aiResponse());

        generationService.startGeneration(request("cute cat lover", style));

        awaitSingleSubtaskStatus(SubtaskStatus.COMPLETED);
        awaitTaskStatus(GenerationStatus.COMPLETED);
        verify(requestSpec, times(2)).call();
        assertThat(designRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldMarkSubtaskFailedAndFinishTaskWhenAiKeepsFailing() {
        var style = saveStyle("Vector");
        when(callSpec.entity(ChatImagePromptResponse.class)).thenThrow(new RuntimeException("model is down"));

        generationService.startGeneration(request("cute cat lover", style));

        awaitSingleSubtaskStatus(SubtaskStatus.FAILED);

        var subtask = generationSubtaskRepository.findAll().getFirst();
        assertThat(subtask.getErrorMessage()).isNotBlank();
        assertThat(subtask.getFinishedAt()).isNotNull();
        assertThat(subtask.getDesignId()).isNull();
        assertThat(designRepository.count()).isZero();
        verifyNoInteractions(designGeneratorClient);

        // the task must not hang in GENERATING when all of its subtasks are finished
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(singleTask().getFinishedAt()).isNotNull());
    }

    private ImageStyle saveStyle(String name) {
        var style = new ImageStyle();
        style.setName(name + "-" + UUID.randomUUID());
        style.setSystemPrompt("system prompt of " + name);
        style.setActive(true);
        return imageStyleRepository.save(style);
    }

    private GenerationRequest request(String trend, ImageStyle... styles) {
        var request = new GenerationRequest();
        request.setRawTrend(trend);
        request.setFilter(false);
        request.setStyleIds(Set.of(styles).stream().map(ImageStyle::getId).collect(java.util.stream.Collectors.toSet()));
        return request;
    }

    private ChatImagePromptResponse aiResponse() {
        return new ChatImagePromptResponse(
                "a cat with headphones, flat vector",
                "blurry, text",
                "Cat DJ",
                "cat",
                List.of("music", "cute"),
                "Funny cat design"
        );
    }

    private GenerationTask singleTask() {
        var tasks = generationTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        return tasks.getFirst();
    }

    private void awaitTaskStatus(GenerationStatus expected) {
        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertThat(generationTaskRepository.findAll())
                        .extracting(GenerationTask::getStatus)
                        .containsExactly(expected));
    }

    private void awaitSingleSubtaskStatus(SubtaskStatus expected) {
        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertThat(generationSubtaskRepository.findAll())
                        .extracting(GenerationSubtask::getStatus)
                        .containsExactly(expected));
    }
}