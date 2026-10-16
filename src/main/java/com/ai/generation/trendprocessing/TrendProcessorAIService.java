package com.ai.generation.trendprocessing;

import com.ai.generation.exception.TrendFilteringException;
import com.ai.generation.exception.TrendProcessingException;
import com.ai.generation.niche.Niche;
import com.ai.generation.niche.repository.NicheRepository;
import com.ai.generation.trendprocessing.dto.TrendDecision;
import com.ai.generation.trendprocessing.dto.TrendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendProcessorAIService {

    private final ChatClient chatClient;
    private final NicheRepository nicheRepository;

    private static final String PROMPT = """
            You are a content strategist for a print-on-demand merch business. You receive a list of raw trending keywords/categories scraped from Google Trends or similar sources. Your job is to turn them into safe, sellable niche concepts for AI-generated merch designs (t-shirts, mugs, stickers).
            
            For EACH raw trend in the input, output a decision object with these fields:
            - rawTrend: the original input string, unchanged
            - status: one of "KEPT", "GENERALIZED", "FILTERED"
            - keyword: the resulting merch-friendly niche keyword (2-5 words), or null if status is "FILTERED"
            - reason: a short explanation (one sentence) of why this decision was made
            
            Apply these rules in order:
            
            1. **First, check if the raw trend can be used as-is.** If the phrase is already generic (hobbies, animals, activities, moods, professions, visual metaphors, everyday situations) and contains NO direct reference to a protected brand, real person, specific copyrighted work, or tragic event → status = "KEPT".
               - Slightly reword only if the original is awkward or unclear (e.g., fix grammar). Otherwise, keep the rawTrend as keyword.
               - Reason: "already a safe, generic visual concept"
            
            2. **If the raw trend DOES contain a problematic element** (celebrity name, trademark, movie/game title, etc.), try to EXTRACT a broader, brand-free theme from it.
               - If a relatable, non-infringing concept can be derived → status = "GENERALIZED", with the new keyword.
               - Reason must explain the transformation.
               - Examples:
                 - "Taylor Swift tour" → "concert obsessed fan energy"
                 - "iPhone 17 launch" → "phone upgrade addiction"
                 - "avatar airbender" → "elemental magic art"
                 - "Starbucks pumpkin spice" → "cozy autumn drinks aesthetic"
               - If NO meaningful, copyright-safe generalization is possible (the whole value is the IP itself, e.g. "Spider-Man costume") → status = "FILTERED", reason explains why.
            
            3. **Additional FILTERED triggers** (do this only if the trend cannot be kept or generalized):
               - Tragedy, violence, death, natural disasters, sensitive political events.
               - Extremely vague or non-visual (e.g., "weather forecast", "news live").
               - Direct reference to a real living person without any possible archetype extraction.
            
            IMPORTANT GUIDELINES:
            - **Do NOT generalize a trend that is already safe and specific.** Only generalize to escape copyright/trademark issues.
            - Keep keywords concrete and visual — "chess on the tower" is far better than "strategy board games".
            - Every input must have an output, no omissions.
            """;

    public Niche processTrend(String rawTrend, boolean filter) {
        final String trend;
        if (filter) {
            log.info("Start process raw trend: {}", rawTrend);
            var decision = getDecision(rawTrend)
                    .orElseThrow(() -> new TrendProcessingException("AI service failed to process trend: " + rawTrend));

            if (decision.keyword() == null || decision.keyword().isBlank()) {
                throw new TrendProcessingException("AI returned empty keyword for trend: " + rawTrend);
            }
            logDecision(decision);

            if (decision.status() == TrendStatus.FILTERED) {
                throw new TrendProcessingException("Trend does not meet safety criteria");
            }
            trend = decision.keyword();
        } else {
            trend = rawTrend;
        }

        return nicheRepository.findByKeywordIgnoreCase(trend)
                .orElseGet(() -> nicheRepository.save(new Niche(trend)));
    }

    private Optional<TrendDecision> getDecision(String rawTrend) {
        return Optional.ofNullable(chatClient.prompt()
                .system(PROMPT)
                .user(rawTrend)
                .call()
                .entity(TrendDecision.class));
    }

    private void logDecision(TrendDecision decision) {
        switch (decision.status()) {
            case FILTERED -> log.info("Trend FILTERED: raw='{}', reason='{}'", decision.rawTrend(), decision.reason());
            case GENERALIZED -> log.info("Trend GENERALIZED: raw='{}' -> keyword='{}', reason='{}'",
                    decision.rawTrend(), decision.keyword(), decision.reason());
            case KEPT -> log.info("Trend KEPT: raw='{}' -> keyword='{}', reason='{}'",
                    decision.rawTrend(), decision.keyword(), decision.reason());
        }

    }
}
