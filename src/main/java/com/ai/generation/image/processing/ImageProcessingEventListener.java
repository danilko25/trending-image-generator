package com.ai.generation.image.processing;

import com.ai.generation.image.processing.dto.ImageProcessingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ImageProcessingEventListener {

    private final ImageProcessingExecutor imageProcessingExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncImageProcessing(ImageProcessingEvent event) {
        imageProcessingExecutor.execute(event.imageProcessingId());
    }
}
