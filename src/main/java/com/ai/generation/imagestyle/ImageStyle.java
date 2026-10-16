package com.ai.generation.imagestyle;

import com.ai.generation.imagestyle.dto.ImageStyleCreateUpdateDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class ImageStyle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(length = 2000)
    private String systemPrompt;
    private boolean active;

    public ImageStyle(ImageStyleCreateUpdateDto createDto) {
        name = createDto.getName();
        systemPrompt = createDto.getSystemPrompt();
        active =  true;
    }
}
