package com.ai.generation.design.dto;

import com.ai.generation.design.dto.projections.DesignFullDetailsProjection;
import com.ai.generation.imageprompt.dto.ImagePromptSupportingTagsProjection;
import com.ai.generation.imagestyle.dto.ImageStyleLinkDto;
import com.ai.generation.niche.dto.NicheLinkDto;
import lombok.Data;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Data
public class DesignFullDetailsDto {
    private UUID id;
    private NicheLinkDto niche;
    private ImageStyleLinkDto style;
    private String imageURL;
    private String title;
    private String mainTag;
    private List<String> supportingTags;
    private String description;
    private String prompt;
    private String negativePrompt;

    public DesignFullDetailsDto(DesignFullDetailsProjection projection, List<ImagePromptSupportingTagsProjection> supportingTagsProjections) {
        this.id = projection.getId();
        if (projection.getPromptNicheId() != null) {
            this.niche = new NicheLinkDto(projection.getPromptNicheId(), projection.getPromptNicheKeyword());
        }
        if (projection.getPromptStyleId() != null) {
            this.style = new ImageStyleLinkDto(projection.getPromptStyleId(), projection.getPromptStyleName());
        }
        this.imageURL = projection.getImagePath();
        this.title = projection.getPromptTitle();
        this.mainTag = projection.getPromptMainTag();
        this.description = projection.getPromptDescription();
        this.prompt = projection.getPromptValue();
        this.negativePrompt = projection.getPromptNegativeValue();
        this.supportingTags = supportingTagsProjections.stream()
                .map(ImagePromptSupportingTagsProjection::getSupportingTags)
                .collect(toList());
    }
}
