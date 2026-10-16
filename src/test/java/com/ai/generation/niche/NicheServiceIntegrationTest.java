package com.ai.generation.niche;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.niche.dto.SearchNicheRequest;
import com.ai.generation.niche.dto.SearchNicheResponse;
import com.ai.generation.niche.repository.NicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class NicheServiceIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = T0.plus(Duration.ofDays(1));
    private static final Instant T2 = T0.plus(Duration.ofDays(2));

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
    private NicheService nicheService;
    @Autowired
    private NicheRepository nicheRepository;

    @BeforeEach
    void cleanUp() {
        nicheRepository.deleteAll();
    }

    @Test
    void getById_shouldReturnNicheOrThrowNotFound() {
        var niche = saveNiche("cat lover", T0);

        assertThat(nicheService.getById(niche.getId()).getKeyword()).isEqualTo("cat lover");
        assertThatThrownBy(() -> nicheService.getById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getDetailsById_shouldMapProjection() {
        var niche = saveNiche("cat lover", T0);

        var details = nicheService.getDetailsById(niche.getId());

        assertThat(details.getId()).isEqualTo(niche.getId());
        assertThat(details.getKeyword()).isEqualTo("cat lover");
        assertThat(details.getCreatedAt()).isNotNull();
        assertThat(details.getLastUsedAt()).isNull();
        assertThatThrownBy(() -> nicheService.getDetailsById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void searchNiches_shouldReturnAllWithoutFilters() {
        saveThreeNiches();

        var page = nicheService.searchNiches(request());

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void searchNiches_shouldFilterByKeywordCaseInsensitiveContains() {
        saveThreeNiches();
        var request = request();
        request.setKeyword("CAT");

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("cat lover", "cat cafe");
    }

    @Test
    void searchNiches_shouldIncludeBoundaryForCreatedAfter() {
        saveThreeNiches();
        var request = request();
        request.setCreatedAfter(T1);

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("dog walker", "cat cafe");
    }

    @Test
    void searchNiches_shouldIncludeBoundaryForCreatedBefore() {
        saveThreeNiches();
        var request = request();
        request.setCreatedBefore(T1);

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("cat lover", "dog walker");
    }

    @Test
    void searchNiches_shouldCombineKeywordAndDateRange() {
        saveThreeNiches();
        var request = request();
        request.setKeyword("cat");
        request.setCreatedAfter(T1);
        request.setCreatedBefore(T2);

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword).containsExactly("cat cafe");
    }

    @Test
    void searchNiches_shouldReturnEmptyPageWhenNothingMatches() {
        saveThreeNiches();
        var request = request();
        request.setKeyword("unicorn");

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void searchNiches_shouldSortByCreatedAtDescendingByDefault() {
        saveThreeNiches();

        var page = nicheService.searchNiches(new SearchNicheRequest());

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("cat cafe", "dog walker", "cat lover");
    }

    @Test
    void searchNiches_shouldFallBackToCreatedAtForUnknownSortField() {
        saveThreeNiches();
        var request = request();
        request.setSortBy("nonsense");

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("cat lover", "dog walker", "cat cafe");
    }

    @Test
    void searchNiches_shouldSortByLastUsedAt() {
        saveNiche("first", T0, T2);
        saveNiche("second", T1, T0);
        saveNiche("third", T2, T1);
        var request = request();
        request.setSortBy("lastUsedAt");

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("second", "third", "first");
    }

    @Test
    void searchNiches_shouldSortByKeyword() {
        saveNiche("banana", T0);
        saveNiche("apple", T1);
        saveNiche("cherry", T2);
        var request = request();
        request.setSortBy("keyword");

        var page = nicheService.searchNiches(request);

        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword)
                .containsExactly("apple", "banana", "cherry");
    }

    @Test
    void searchNiches_shouldPaginate() {
        saveThreeNiches();
        var request = request();
        request.setPageSize(2);
        request.setPage(1);

        var page = nicheService.searchNiches(request);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(SearchNicheResponse::getKeyword).containsExactly("cat cafe");
    }

    @Test
    void searchNiches_shouldMapEntityToResponse() {
        var niche = saveNiche("cat lover", T0, T1);

        var page = nicheService.searchNiches(request());

        assertThat(page.getContent()).hasSize(1);
        var response = page.getContent().getFirst();
        assertThat(response.getId()).isEqualTo(niche.getId());
        assertThat(response.getKeyword()).isEqualTo("cat lover");
        assertThat(response.getCreatedAt()).isEqualTo(T0);
        assertThat(response.getLastUsedAt()).isEqualTo(T1);
    }

    private void saveThreeNiches() {
        saveNiche("cat lover", T0);
        saveNiche("dog walker", T1);
        saveNiche("cat cafe", T2);
    }

    private Niche saveNiche(String keyword, Instant createdAt) {
        return saveNiche(keyword, createdAt, null);
    }

    private Niche saveNiche(String keyword, Instant createdAt, Instant lastUsedAt) {
        var niche = new Niche();
        niche.setKeyword(keyword);
        niche.setCreatedAt(createdAt);
        niche.setLastUsedAt(lastUsedAt);
        return nicheRepository.save(niche);
    }

    private SearchNicheRequest request() {
        var request = new SearchNicheRequest();
        request.setDirection(Sort.Direction.ASC);
        return request;
    }
}