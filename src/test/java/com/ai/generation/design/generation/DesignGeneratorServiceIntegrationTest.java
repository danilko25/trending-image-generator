package com.ai.generation.design.generation;

import com.ai.generation.design.DesignRepository;
import com.ai.generation.design.generation.dto.GenerateDesignRequest;
import com.ai.generation.design.generation.dto.GenerateDesignResponse;
import com.ai.generation.imageprompt.ImagePrompt;
import com.ai.generation.imageprompt.ImagePromptRepository;
import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.imagestyle.repository.ImageStyleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class DesignGeneratorServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
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
    private DesignGeneratorService designGeneratorService;

    @Autowired
    private ImagePromptRepository imagePromptRepository;

    @Autowired
    private ImageStyleRepository imageStyleRepository;

    @Autowired
    private DesignRepository designRepository;

    @MockitoBean
    private DesignGeneratorClient designGeneratorClient;

    @Test
    void shouldGenerateAndSaveDesign() {
        // given
        // test data

        var style = new ImageStyle();
        style.setName("Test style");
        style.setSystemPrompt("Prompt for test style");
        style.setActive(true);

        imageStyleRepository.save(style);

        var prompt = new ImagePrompt();
        prompt.setStyle(style);

        imagePromptRepository.save(prompt);

        var generatedUrl = "https://example.com/generated-image.png";

        when(designGeneratorClient.generateImage(any(GenerateDesignRequest.class)))
                .thenReturn(new GenerateDesignResponse(generatedUrl, prompt.getId()));

        // when
        var result = designGeneratorService.createImage(prompt.getId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getImagePath()).isEqualTo(generatedUrl);

        var savedDesign = designRepository.findById(result.getId());

        assertThat(savedDesign).isPresent();
        assertThat(savedDesign.get().getImagePath())
                .isEqualTo(generatedUrl);
        assertThat(savedDesign.get().getPrompt().getId())
                .isEqualTo(prompt.getId());
    }
}