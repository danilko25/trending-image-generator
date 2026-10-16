package com.ai.generation.design;

import com.ai.generation.common.SearchRequest;
import com.ai.generation.design.dto.DesignCardDto;
import com.ai.generation.design.dto.DesignCreateDto;
import com.ai.generation.design.dto.DesignShortDetailsDto;
import com.ai.generation.design.dto.DesignFullDetailsDto;
import com.ai.generation.design.dto.projections.DesignFullDetailsProjection;
import com.ai.generation.image.processing.dto.ProcessedImageProjection;
import com.ai.generation.design.dto.projections.DesignShortDetailsProjection;
import com.ai.generation.image.processing.ProcessedImageRepository;
import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.imageprompt.ImagePromptRepository;
import com.ai.generation.imageprompt.ImagePromptService;
import com.ai.generation.imageprompt.dto.ImagePromptSupportingTagsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DesignService {

    private final DesignRepository designRepository;
    private final ProcessedImageRepository processedImageRepository;
    private final ImagePromptService imagePromptService;
    private final ImagePromptRepository imagePromptRepository;

    public Design findById(UUID id) {
        return designRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Design.class, id));
    }

    public Design findByIdWithProcessedImages(UUID id) {
        return designRepository.findByIdWithProcessedImages(id)
                .orElseThrow(() -> new EntityNotFoundException(Design.class, id));
    }

    @Transactional
    public Design createDesign(DesignCreateDto creationDto) {
        var prompt = imagePromptService.getById(creationDto.promptId());
        return designRepository.save(new Design(prompt, creationDto.url()));
    }

    public Page<DesignCardDto> getDesignCards(SearchRequest request) {
        Pageable pageable = request.getPageable(DesignCardDto.getSortingParam(request.getSortBy()).getName());

        return designRepository.findAll(pageable).map(DesignCardDto::new);
    }

    public DesignFullDetailsDto getFullDetailsById(UUID id) {
        var designProjection = designRepository.findById(id, DesignFullDetailsProjection.class)
                .orElseThrow(() -> new EntityNotFoundException(Design.class, id));
        var supportedTags = imagePromptRepository.findAllById(designProjection.getPromptId(), ImagePromptSupportingTagsProjection.class);
        return new DesignFullDetailsDto(designProjection, supportedTags);
    }

    public DesignShortDetailsDto getShortDetailsById(UUID id) {
        var designProjection = designRepository.findById(id, DesignShortDetailsProjection.class)
                .orElseThrow(() -> new EntityNotFoundException(Design.class, id));
        var supportedTags = imagePromptRepository.findAllById(designProjection.getPromptId(), ImagePromptSupportingTagsProjection.class);
        var processedImages = processedImageRepository.findAllByDesignId(designProjection.getId(), ProcessedImageProjection.class);
        return new DesignShortDetailsDto(designProjection, supportedTags, processedImages);

    }
}
