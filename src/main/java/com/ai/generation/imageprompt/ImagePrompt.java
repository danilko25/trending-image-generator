package com.ai.generation.imageprompt;

import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.niche.Niche;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class ImagePrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Niche niche;

    @ManyToOne(fetch = FetchType.LAZY)
    private ImageStyle style;

    @Column(length = 1500)
    private String value;
    @Column(length = 500)
    private String negativeValue;

    private String title;
    private String mainTag;

    @ElementCollection
    private List<String> supportingTags;

    @Column(length = 1000)
    private String description;

    private Instant createdAt;

    public ImagePrompt(ImagePromptCreateDto createDto) {
        this.niche = createDto.getNiche();
        this.style = createDto.getImageStyle();
        this.value = createDto.getValue();
        this.negativeValue = createDto.getNegativeValue();
        this.title = createDto.getTitle();
        this.mainTag = createDto.getMainTag();
        this.supportingTags = createDto.getSupportingTags();
        this.description = createDto.getDescription();
        this.createdAt = Instant.now();
    }

    public ImagePrompt(ChatImagePromptResponse chatResponse, Niche niche, ImageStyle imageStyle) {
        this.niche = niche;
        this.style = imageStyle;
        this.value = chatResponse.promptText();
        this.negativeValue = chatResponse.negativePrompt();
        this.title = chatResponse.title();
        this.mainTag = chatResponse.mainTag();
        this.supportingTags = chatResponse.supportedTags();
        this.description = chatResponse.description();
        this.createdAt = Instant.now();
    }
}
