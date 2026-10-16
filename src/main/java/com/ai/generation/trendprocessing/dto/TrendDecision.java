package com.ai.generation.trendprocessing.dto;

public record TrendDecision(String rawTrend, TrendStatus status, String keyword, String reason) {
}
