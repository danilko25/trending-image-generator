package com.ai.generation.image.processing;

import com.ai.generation.design.Design;
import com.ai.generation.exception.DesignGenerationException;
import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.image.processing.dto.ImageProcessingEvent;
import com.ai.generation.image.processing.entity.ProcessedImage;
import com.ai.generation.image.processing.entity.ProcessingStatus;
import com.ai.generation.image.processing.entity.ProcessingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

import static com.ai.generation.image.processing.entity.ProcessingStatus.COMPLETED;
import static com.ai.generation.image.processing.entity.ProcessingStatus.FAILED;
import static com.ai.generation.image.processing.entity.ProcessingStatus.PENDING;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedImageService {
    private final ProcessedImageRepository processedImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProcessedImage findById(UUID id) {
        return processedImageRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException(ProcessedImage.class, id));
    }

    public ProcessedImage findByTypeForDesign(UUID designId, ProcessingType type) {
        return processedImageRepository.findByDesignIdAndType(designId, type)
                .orElseThrow(() -> new DesignGenerationException(
                        "Processed image with type " + type + " doesn't exist for design " + designId
                ));
    }

    @Transactional
    public ProcessedImage findOrCreate(Design design, ProcessingType type) {
        return design.getProcessedImages().stream()
                .filter(image -> type.equals(image.getType()))
                .findFirst()
                .orElseGet(() -> create(design, type));
    }

    @Transactional
    public void setStatus(UUID id, ProcessingStatus status) {
        var processedImage = findById(id);
        processedImage.setStatus(status);
    }

    @Transactional
    public ProcessedImage create(Design design, ProcessingType type) {
        var processedImage = new ProcessedImage();
        processedImage.setDesign(design);
        processedImage.setType(type);
        processedImage.setStatus(PENDING);

        var savedImage = processedImageRepository.save(processedImage);
        design.getProcessedImages().add(savedImage);

        return savedImage;
    }

    @Transactional
    public void startProcessing(Design design, ProcessingType type) {
        var processedImage = findOrCreate(design, type);

        checkStatus(processedImage);

        eventPublisher.publishEvent(
                new ImageProcessingEvent(processedImage.getId())
        );
    }

    private void checkStatus(ProcessedImage processedImage) {
        if (processedImage.getStatus() != PENDING && processedImage.getStatus() != FAILED) {
            throw new DesignGenerationException(
                    "Process for image: " + processedImage.getId() + " cannot be started in current status: " + processedImage.getStatus()
            );
        }
    }

    @Transactional
    public void complete(UUID id, String resultUrl) {
        var image = findById(id);
        image.setStatus(COMPLETED);
        image.setResultUrl(resultUrl);
        processedImageRepository.save(image);
        log.info("Processing completed for image: {}", image.getId());
    }

    @Transactional
    public void fail(UUID id, Throwable error) {
        var image = findById(id);
        image.setStatus(FAILED);
        image.setErrorMessage(error.getMessage());
        processedImageRepository.save(image);
        log.error("Processing failed for image: {}", image.getId(), error);
    }
}
