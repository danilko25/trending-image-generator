package com.ai.generation.trendprocessing;

import com.ai.generation.exception.TrendFilteringException;
import com.ai.generation.exception.TrendProcessingException;
import com.ai.generation.niche.Niche;
import com.ai.generation.niche.repository.NicheRepository;
import com.ai.generation.trendprocessing.dto.TrendDecision;
import com.ai.generation.trendprocessing.dto.TrendStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class TrendProcessorAIServiceIntegrationTest {

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
    private TrendProcessorAIService trendProcessorAIService;
    @Autowired
    private NicheRepository nicheRepository;

    @MockitoBean
    private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        nicheRepository.deleteAll();

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void processTrend_shouldCreateNicheFromRawTrendWithoutCallingAiWhenFilterIsOff() {
        var niche = trendProcessorAIService.processTrend("cute cat lover", false);

        assertThat(niche.getId()).isNotNull();
        assertThat(niche.getKeyword()).isEqualTo("cute cat lover");
        assertThat(niche.getCreatedAt()).isNotNull();
        assertThat(nicheRepository.count()).isEqualTo(1);
        verifyNoInteractions(chatClient);
    }

    @Test
    void processTrend_shouldReuseExistingNicheIgnoringCase() {
        var existing = nicheRepository.save(new Niche("Cute Cat Lover"));

        var niche = trendProcessorAIService.processTrend("cute cat lover", false);

        assertThat(niche.getId()).isEqualTo(existing.getId());
        assertThat(nicheRepository.count()).isEqualTo(1);
    }

    @Test
    void processTrend_shouldUseKeywordFromAiForKeptTrend() {
        stubDecision(new TrendDecision("raw cozy trend", TrendStatus.KEPT, "cozy autumn", "already safe"));

        var niche = trendProcessorAIService.processTrend("raw cozy trend", true);

        assertThat(niche.getKeyword()).isEqualTo("cozy autumn");
        assertThat(nicheRepository.findAll()).extracting(Niche::getKeyword).containsExactly("cozy autumn");
        verify(requestSpec).user("raw cozy trend");
    }

    @Test
    void processTrend_shouldSaveGeneralizedKeywordInsteadOfRawTrend() {
        stubDecision(new TrendDecision("Taylor Swift tour", TrendStatus.GENERALIZED,
                "concert obsessed fan energy", "removed celebrity name"));

        var niche = trendProcessorAIService.processTrend("Taylor Swift tour", true);

        assertThat(niche.getKeyword()).isEqualTo("concert obsessed fan energy");
        assertThat(nicheRepository.findAll()).extracting(Niche::getKeyword)
                .containsExactly("concert obsessed fan energy");
    }

    @Test
    void processTrend_shouldReuseExistingNicheWhenAiKeywordAlreadyExists() {
        var existing = nicheRepository.save(new Niche("Concert Obsessed Fan Energy"));
        stubDecision(new TrendDecision("Taylor Swift tour", TrendStatus.GENERALIZED,
                "concert obsessed fan energy", "removed celebrity name"));

        var niche = trendProcessorAIService.processTrend("Taylor Swift tour", true);

        assertThat(niche.getId()).isEqualTo(existing.getId());
        assertThat(nicheRepository.count()).isEqualTo(1);
    }

    @Test
    void processTrend_shouldThrowAndSaveNothingForFilteredTrend() {
        stubDecision(new TrendDecision("Spider-Man costume", TrendStatus.FILTERED, null, "pure IP"));

        assertThatThrownBy(() -> trendProcessorAIService.processTrend("Spider-Man costume", true))
                .isInstanceOf(TrendProcessingException.class);

        assertThat(nicheRepository.count()).isZero();
    }

    @Test
    void processTrend_shouldThrowProcessingExceptionWhenAiReturnsNull() {
        when(callSpec.entity(TrendDecision.class)).thenReturn(null);

        assertThatThrownBy(() -> trendProcessorAIService.processTrend("some trend", true))
                .isInstanceOf(TrendProcessingException.class)
                .hasMessageContaining("some trend");

        assertThat(nicheRepository.count()).isZero();
    }

    @Test
    void processTrend_shouldPropagateAiFailureAndSaveNothing() {
        when(callSpec.entity(TrendDecision.class)).thenThrow(new RuntimeException("model is down"));

        assertThatThrownBy(() -> trendProcessorAIService.processTrend("some trend", true))
                .hasMessageContaining("model is down");

        assertThat(nicheRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void processTrend_shouldRejectDecisionWithoutKeyword(String keyword) {
        stubDecision(new TrendDecision("some trend", TrendStatus.KEPT, keyword, "broken answer"));

        assertThatThrownBy(() -> trendProcessorAIService.processTrend("some trend", true))
                .isInstanceOf(TrendProcessingException.class);

        assertThat(nicheRepository.count()).isZero();
    }

    private void stubDecision(TrendDecision decision) {
        when(callSpec.entity(TrendDecision.class)).thenReturn(decision);
    }
}