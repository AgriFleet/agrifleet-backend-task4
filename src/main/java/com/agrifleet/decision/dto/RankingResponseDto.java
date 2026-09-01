package com.agrifleet.decision.dto;

import java.util.List;
import java.util.Map;

public record RankingResponseDto(
        long decisionRunId,
        long bookingId,
        Map<String, Double> normalizedWeights,
        List<RankedCandidateDto> rankedCandidates,
        double executionTimeMs
) {}
