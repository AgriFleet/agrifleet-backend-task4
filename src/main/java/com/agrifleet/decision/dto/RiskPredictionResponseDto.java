package com.agrifleet.decision.dto;

public record RiskPredictionResponseDto(
        long bookingId,
        String predictedRiskTier,
        double confidenceScore,
        double fieldAcres,
        double rainProbability,
        int vehicleBreakdownHistory
) {}
