package com.ai.generation.generation;

import com.ai.generation.generationtask.repository.GenerationTaskRepository;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class GenerationServiceIntegrationTest {

    @Value("${task.generation.topic}")
    private String taskGenerationTopic;
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("generation_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer("apache/kafka:3.8.1");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }

    @Autowired
    private GenerationService generationService;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Autowired
    private ImageStyleRepository imageStyleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),

                ConsumerConfig.GROUP_ID_CONFIG,
                "generation-service-integration-test",

                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,

                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,

                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        consumer = new DefaultKafkaConsumerFactory<String, String>(
                properties
        ).createConsumer();

        consumer.subscribe(
                List.of(taskGenerationTopic)
        );

        consumer.poll(Duration.ofSeconds(1));
    }

    @Test
    void shouldSaveTaskAndPublishEventAfterCommit() {
        var request = createRequest();

        generationService.startGeneration(request);

        var tasks = generationTaskRepository.findAll();

        assertThat(tasks).hasSize(1);

        var taskId = tasks.getFirst().getId();

        var records = consumer.poll(Duration.ofSeconds(10));

        assertThat(records).hasSize(1);

        var record = records.iterator().next();

        assertThat(record.value())
                .contains(taskId.toString());
    }


    @AfterEach
    void tearDown() {
        consumer.close();
        generationTaskRepository.deleteAll();
    }

    @Test
    void shouldNotPublishEventWhenTransactionRollsBack() {
        var request = createRequest();

        transactionTemplate.executeWithoutResult(status -> {
            generationService.startGeneration(request);

            status.setRollbackOnly();
        });

        assertThat(generationTaskRepository.findAll())
                .isEmpty();

        var records = consumer.poll(Duration.ofSeconds(2));

        assertThat(records).isEmpty();
    }

    private GenerationRequest createRequest() {
        var res = new GenerationRequest();
        res.setRawTrend("Raw test trend");
        res.setFilter(false);
        res.setStyleIds(Set.of(UUID.randomUUID()));
        return res;
    }
}