package com.ai.generation.imageprompt;

import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.niche.Niche;
import lombok.Data;

import java.util.List;

@Data
public class ImagePromptCreateDto {
    private String value;
    private String negativeValue;
    private Niche niche;
    private ImageStyle imageStyle;
    private String title;
    private String mainTag;
    private List<String> supportingTags;
    private String description;

    public ImagePromptCreateDto(ChatImagePromptResponse chatResponse, Niche niche, ImageStyle imageStyle) {
        this.value = chatResponse.promptText();
        this.negativeValue = chatResponse.negativePrompt();
        this.niche = niche;
        this.imageStyle = imageStyle;
        this.title = chatResponse.title();
        this.mainTag = chatResponse.mainTag();
        this.supportingTags = chatResponse.supportedTags();
        this.description = chatResponse.description();
    }

}
