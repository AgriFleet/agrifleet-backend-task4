package com.agrifleet.decision.dto;

public record RankedCandidateDto(
        long vehicleId,
        String vehicleType,
        double hourlyRate,
        double distanceKm,
        int horsepower,
        double ratingScore,
        double separationSPlus,
        double separationSMinus,
        double relativeClosenessC,
        int finalRank
) {}
