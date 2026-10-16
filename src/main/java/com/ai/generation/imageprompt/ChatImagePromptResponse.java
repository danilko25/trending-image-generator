package com.ai.generation.imageprompt;

import java.util.List;

public record ChatImagePromptResponse(String promptText, String negativePrompt, String title, String mainTag, List<String> supportedTags, String description) {
}
