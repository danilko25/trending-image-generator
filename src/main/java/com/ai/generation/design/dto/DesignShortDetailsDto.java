package com.ai.generation.design.dto;

import com.ai.generation.image.processing.entity.ProcessedImage;
import com.ai.generation.image.processing.entity.ProcessingStatus;
import com.ai.generation.image.processing.dto.ProcessedImageProjection;
import com.ai.generation.design.dto.projections.DesignShortDetailsProjection;
import com.ai.generation.design.Design;
import com.ai.generation.image.processing.entity.ProcessingType;
import com.ai.generation.imageprompt.dto.ImagePromptSupportingTagsProjection;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Data
public class DesignShortDetailsDto {
    private UUID id;
    private String imageURL;
    private String upscaledImagePath;
    private String removedBackgroundImagePath;
    private String upscaledRemovedBackgroundImagePath;
    private String title;
    private String mainTag;
    private List<String> supportingTags;
    private String description;

    private ProcessingStatus upscaleStatus;
    private ProcessingStatus removeBackgroundStatus;
    private ProcessingStatus upscaleRemoveBackgroundStatus;

    public DesignShortDetailsDto(DesignShortDetailsProjection projection,
                                 List<ImagePromptSupportingTagsProjection> supportingTagsProjections,
                                 List<ProcessedImageProjection> processedImageProjections) {
        this.id = projection.getId();
        this.imageURL = projection.getImagePath();
        this.title = projection.getPromptTitle();
        this.mainTag = projection.getPromptMainTag();
        this.description = projection.getPromptDescription();
        this.supportingTags = supportingTagsProjections.stream()
                .map(ImagePromptSupportingTagsProjection::getSupportingTags)
                .collect(toList());

        var processedImagesMap = processedImageProjections.stream()
                .collect(toMap(ProcessedImageProjection::getType, Function.identity()));

        var upscaled = processedImagesMap.get(ProcessingType.UPSCALE);
        if (upscaled != null) {
            this.upscaledImagePath = upscaled.getResultUrl();
            this.upscaleStatus = upscaled.getStatus();
        }
        var removedBackground = processedImagesMap.get(ProcessingType.REMOVE_BACKGROUND);
        if (removedBackground != null) {
            this.removedBackgroundImagePath = removedBackground.getResultUrl();
            this.removeBackgroundStatus = removedBackground.getStatus();
        }
        var upscaledRemovedBackground = processedImagesMap.get(ProcessingType.UPSCALE_REMOVE_BACKGROUND);
        if (upscaledRemovedBackground != null) {
            this.upscaledRemovedBackgroundImagePath = upscaledRemovedBackground.getResultUrl();
            this.upscaleRemoveBackgroundStatus = upscaledRemovedBackground.getStatus();
        }
    }

    public DesignShortDetailsDto(Design design) {
        this.id = design.getId();
        this.imageURL = design.getImagePath();
        this.title = design.getPrompt().getTitle();
        this.mainTag = design.getPrompt().getMainTag();
        this.supportingTags = design.getPrompt().getSupportingTags();
        this.description = design.getPrompt().getDescription();

        var processedImagesMap = design.getProcessedImages().stream().collect(toMap(ProcessedImage::getType, Function.identity()));

        var upscaled = processedImagesMap.get(ProcessingType.UPSCALE);
        if (upscaled != null) {
            this.upscaledImagePath = upscaled.getResultUrl();
            this.upscaleStatus = upscaled.getStatus();
        }
        var removedBackground = processedImagesMap.get(ProcessingType.REMOVE_BACKGROUND);
        if (removedBackground != null) {
            this.removedBackgroundImagePath = removedBackground.getResultUrl();
            this.removeBackgroundStatus = removedBackground.getStatus();
        }
        var upscaledRemovedBackground = processedImagesMap.get(ProcessingType.UPSCALE_REMOVE_BACKGROUND);
        if (upscaledRemovedBackground != null) {
            this.upscaledRemovedBackgroundImagePath = upscaledRemovedBackground.getResultUrl();
            this.upscaleRemoveBackgroundStatus = upscaledRemovedBackground.getStatus();
        }
    }
}
