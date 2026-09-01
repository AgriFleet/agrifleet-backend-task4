package com.agrifleet.decision.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Filters the fleet down to vehicle types that can actually service a given
 * crop, before TOPSIS ranks them. This keeps the decision matrix meaningful
 * (ranking a boom sprayer against a combine harvester for a paddy booking
 * would be comparing apples to oranges) and gives you a concrete,
 * explainable "Constraints" section for Chapter 2 of the individual report.
 */
@Service
public class VehicleCompatibilityService {

    private static final Map<String, List<String>> CROP_TO_VEHICLE_TYPES = Map.of(
            "PADDY", List.of("COMBINE_HARVESTER"),
            "CORN", List.of("COMBINE_HARVESTER", "4WD_TRACTOR"),
            "WHEAT", List.of("COMBINE_HARVESTER"),
            "SUGARCANE", List.of("4WD_TRACTOR"),
            "HAY", List.of("SQUARE_BALER", "4WD_TRACTOR")
    );

    private static final List<String> DEFAULT_TYPES = List.of("COMBINE_HARVESTER", "4WD_TRACTOR");

    public List<String> compatibleTypesFor(String cropType) {
        return CROP_TO_VEHICLE_TYPES.getOrDefault(cropType == null ? "" : cropType.toUpperCase(), DEFAULT_TYPES);
    }
}
