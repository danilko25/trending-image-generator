package com.ai.generation.imagestyle;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.imagestyle.dto.ImageStyleCreateUpdateDto;
import com.ai.generation.imagestyle.dto.ImageStyleDropDownItemDto;
import com.ai.generation.imagestyle.dto.ImageStyleSearchRequest;
import com.ai.generation.imagestyle.dto.ImageStyleTableItemDto;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ImageStyleServiceIntegrationTest {

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
    private ImageStyleService imageStyleService;
    @Autowired
    private ImageStyleRepository imageStyleRepository;

    @BeforeEach
    void cleanUp() {
        // Flyway migrations may seed styles; start every test from an empty table
        imageStyleRepository.deleteAll();
    }

    // ---------- getById / getDetailsById ----------

    @Test
    void getById_shouldReturnStyleOrThrowNotFound() {
        var style = saveStyle("Retro", true);

        assertThat(imageStyleService.getById(style.getId()).getName()).isEqualTo("Retro");
        assertThatThrownBy(() -> imageStyleService.getById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getDetailsById_shouldMapProjectionIncludingActiveFlag() {
        var style = saveStyle("Retro", false);

        var details = imageStyleService.getDetailsById(style.getId());

        assertThat(details.getId()).isEqualTo(style.getId());
        assertThat(details.getName()).isEqualTo("Retro");
        assertThat(details.getSystemPrompt()).isEqualTo("prompt of Retro");
        assertThat(details.isActive()).isFalse();
        assertThatThrownBy(() -> imageStyleService.getDetailsById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_shouldPersistStyleAndReturnResponse() {
        var response = imageStyleService.create(dto("Vector", "vector prompt", true));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Vector");
        assertThat(response.getSystemPrompt()).isEqualTo("vector prompt");
        assertThat(imageStyleRepository.findById(response.getId())).isPresent();
    }

    @Test
    void create_shouldAlwaysCreateActiveStyleIgnoringDtoFlag() {
        var response = imageStyleService.create(dto("Vector", "vector prompt", false));

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void create_shouldRejectDuplicateName() {
        imageStyleService.create(dto("Vector", "p1", true));

        assertThatThrownBy(() -> imageStyleService.create(dto("Vector", "p2", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void update_shouldChangeAllFieldsAndPersistThem() {
        var style = saveStyle("Old name", true);

        var response = imageStyleService.update(style.getId(), dto("New name", "new prompt", false));

        assertThat(response.getName()).isEqualTo("New name");
        var saved = imageStyleRepository.findById(style.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("New name");
        assertThat(saved.getSystemPrompt()).isEqualTo("new prompt");
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void update_shouldThrowNotFoundForUnknownId() {
        assertThatThrownBy(() -> imageStyleService.update(UUID.randomUUID(), dto("x", "y", true)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_shouldRejectNameOfAnotherStyle() {
        saveStyle("Taken", true);
        var style = saveStyle("Mine", true);

        assertThatThrownBy(() -> imageStyleService.update(style.getId(), dto("Taken", "p", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void changeActive_shouldDisableAndEnableStyle() {
        var style = saveStyle("Retro", true);

        var disabled = imageStyleService.changeActive(style.getId(), false);
        assertThat(disabled.isActive()).isFalse();
        assertThat(imageStyleRepository.findById(style.getId()).orElseThrow().isActive()).isFalse();

        var enabled = imageStyleService.changeActive(style.getId(), true);
        assertThat(enabled.isActive()).isTrue();
        assertThat(imageStyleRepository.findById(style.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void changeActive_shouldThrowNotFoundForUnknownId() {
        assertThatThrownBy(() -> imageStyleService.changeActive(UUID.randomUUID(), true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void resolveImageStyles_shouldReturnRequestedStylesEvenIfInactive() {
        var active = saveStyle("Active", true);
        var inactive = saveStyle("Inactive", false);
        saveStyle("Other", true);

        var result = imageStyleService.resolveImageStyles(Set.of(active.getId(), inactive.getId()));

        assertThat(result).extracting(ImageStyle::getName).containsExactlyInAnyOrder("Active", "Inactive");
    }

    @Test
    void resolveImageStyles_shouldReturnOnlyActiveWhenIdsEmptyOrNull() {
        saveStyle("Active 1", true);
        saveStyle("Active 2", true);
        saveStyle("Inactive", false);

        assertThat(imageStyleService.resolveImageStyles(Set.of()))
                .extracting(ImageStyle::getName).containsExactlyInAnyOrder("Active 1", "Active 2");
        assertThat(imageStyleService.resolveImageStyles(null))
                .extracting(ImageStyle::getName).containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void resolveImageStyles_shouldSilentlyIgnoreUnknownIds() {
        var style = saveStyle("Known", true);

        var result = imageStyleService.resolveImageStyles(Set.of(style.getId(), UUID.randomUUID()));

        assertThat(result).extracting(ImageStyle::getName).containsExactly("Known");
    }

    @Test
    void getTablePage_shouldReturnAllWithoutFilters() {
        saveStyle("Alpha", true);
        saveStyle("Beta", false);

        var page = imageStyleService.getTablePage(search(Sort.Direction.ASC));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getTablePage_shouldFilterByNameCaseInsensitiveContains() {
        saveStyle("Alpha Vector", true);
        saveStyle("Beta Vector", true);
        saveStyle("Gamma Retro", true);
        var request = search(Sort.Direction.ASC);
        request.setName("vEcToR");

        var page = imageStyleService.getTablePage(request);

        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName)
                .containsExactly("Alpha Vector", "Beta Vector");
    }

    @Test
    void getTablePage_shouldFilterByActive() {
        saveStyle("Alpha", true);
        saveStyle("Beta", false);
        var request = search(Sort.Direction.ASC);
        request.setActive(false);

        var page = imageStyleService.getTablePage(request);

        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName).containsExactly("Beta");
    }

    @Test
    void getTablePage_shouldCombineNameAndActiveFilters() {
        saveStyle("Alpha Vector", true);
        saveStyle("Beta Vector", false);
        saveStyle("Gamma Retro", false);
        var request = search(Sort.Direction.ASC);
        request.setName("vector");
        request.setActive(false);

        var page = imageStyleService.getTablePage(request);

        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName).containsExactly("Beta Vector");
    }

    @Test
    void getTablePage_shouldFallBackToNameSortForUnknownSortField() {
        saveStyle("Gamma", true);
        saveStyle("Alpha", true);
        saveStyle("Beta", true);
        var request = search(Sort.Direction.ASC);
        request.setSortBy("nonsense");

        var page = imageStyleService.getTablePage(request);

        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName)
                .containsExactly("Alpha", "Beta", "Gamma");
    }

    @Test
    void getTablePage_shouldSortDescendingByDefault() {
        saveStyle("Alpha", true);
        saveStyle("Gamma", true);

        var page = imageStyleService.getTablePage(new ImageStyleSearchRequest());

        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName)
                .containsExactly("Gamma", "Alpha");
    }

    @Test
    void getTablePage_shouldPaginate() {
        saveStyle("Alpha", true);
        saveStyle("Beta", true);
        saveStyle("Gamma", true);
        var request = search(Sort.Direction.ASC);
        request.setPageSize(2);
        request.setPage(1);

        var page = imageStyleService.getTablePage(request);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ImageStyleTableItemDto::getName).containsExactly("Gamma");
    }

    @Test
    void getDropDownPage_shouldAlwaysSortByNameAscending() {
        saveStyle("Gamma", true);
        saveStyle("Alpha", true);
        saveStyle("Beta", true);
        var request = search(Sort.Direction.DESC); // must be overridden by the service

        var page = imageStyleService.getDropDownPage(request);

        assertThat(page.getContent()).extracting(ImageStyleDropDownItemDto::getName)
                .containsExactly("Alpha", "Beta", "Gamma");
    }

    @Test
    void getDropDownPage_shouldFilterByNameWhenProvided() {
        saveStyle("Alpha Vector", true);
        saveStyle("Gamma Retro", true);
        var request = new ImageStyleSearchRequest();
        request.setName("retro");

        var page = imageStyleService.getDropDownPage(request);

        assertThat(page.getContent()).extracting(ImageStyleDropDownItemDto::getName)
                .containsExactly("Gamma Retro");
    }

    @Test
    void getDropDownPage_shouldIncludeInactiveStyles() {
        saveStyle("Active", true);
        saveStyle("Inactive", false);

        var page = imageStyleService.getDropDownPage(new ImageStyleSearchRequest());

        assertThat(page.getContent()).extracting(ImageStyleDropDownItemDto::getName)
                .containsExactly("Active", "Inactive");
    }

    @Test
    void getDropDownPage_shouldPaginate() {
        saveStyle("Alpha", true);
        saveStyle("Beta", true);
        saveStyle("Gamma", true);
        var request = new ImageStyleSearchRequest();
        request.setPageSize(2);

        var page = imageStyleService.getDropDownPage(request);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(ImageStyleDropDownItemDto::getName)
                .containsExactly("Alpha", "Beta");
    }

    private ImageStyle saveStyle(String name, boolean active) {
        var style = new ImageStyle();
        style.setName(name);
        style.setSystemPrompt("prompt of " + name);
        style.setActive(active);
        return imageStyleRepository.save(style);
    }

    private ImageStyleCreateUpdateDto dto(String name, String systemPrompt, boolean active) {
        var dto = new ImageStyleCreateUpdateDto();
        dto.setName(name);
        dto.setSystemPrompt(systemPrompt);
        dto.setActive(active);
        return dto;
    }

    private ImageStyleSearchRequest search(Sort.Direction direction) {
        var request = new ImageStyleSearchRequest();
        request.setDirection(direction);
        return request;
    }
}