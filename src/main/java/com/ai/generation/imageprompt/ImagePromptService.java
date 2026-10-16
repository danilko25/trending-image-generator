package com.ai.generation.imageprompt;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.exception.ImagePromptGenerationException;
import com.ai.generation.imagestyle.ImageStyleService;
import com.ai.generation.niche.NicheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePromptService {

    private final ChatClient chatClient;
    private final NicheService nicheService;
    private final ImageStyleService imageStyleService;
    private final ImagePromptRepository imagePromptRepository;

    public ImagePrompt getById(UUID id) {
        return imagePromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ImagePrompt.class, id));
    }

    public ImagePrompt getWithStyleById(UUID id) {
        return imagePromptRepository.findWithStyleById(id)
                .orElseThrow(() -> new EntityNotFoundException(ImagePrompt.class, id));
    }

    public ImagePrompt generateImagePrompt(UUID imageStyleId, UUID nicheId) {

        var imageStyle = imageStyleService.getById(imageStyleId);
        var niche = nicheService.getById(nicheId);

        try {
            log.info("Sending request with niche {} and style {} to AI", niche.getKeyword(), imageStyle.getName());
            var chatResponse = chatClient
                    .prompt()
                    .system(imageStyle.getSystemPrompt())
                    .user(niche.getKeyword())
                    .call()
                    .entity(ChatImagePromptResponse.class);

            if (chatResponse == null) {
                throw new ImagePromptGenerationException("Service return null object");
            }
            log.info("Successfully generated prompt for niche {} and style {}, result: {}", niche.getKeyword(), imageStyle.getName(), chatResponse.promptText());
            return imagePromptRepository.save(new ImagePrompt(chatResponse, niche, imageStyle));
        } catch (Exception e) {
            throw new ImagePromptGenerationException("Error occurs on image prompt generation niche: " + niche.getKeyword() + " style: " + imageStyle.getName(), e);
        }
    }
}
