package com.ai.generation.design;

import com.ai.generation.image.processing.entity.ProcessedImage;
import com.ai.generation.imageprompt.ImagePrompt;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
public class Design {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private ImagePrompt prompt;
    private String imagePath;
    private Instant createdAt;

    @OneToMany(fetch = FetchType.LAZY)
    private List<ProcessedImage> processedImages;

    public Design(ImagePrompt imagePrompt, String url) {
        this.prompt = imagePrompt;
        this.imagePath = url;
        this.createdAt = Instant.now();
    }
}
