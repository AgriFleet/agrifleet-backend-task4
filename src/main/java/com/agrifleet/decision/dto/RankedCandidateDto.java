package com.agrifleet.decision.dto;

/** Immutable record representing one ranked vehicle candidate. */
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
