package com.agrifleet.decision.service;

import com.agrifleet.decision.dto.RankRequestDto;
import com.agrifleet.decision.dto.RankedCandidateDto;
import com.agrifleet.decision.dto.RankingResponseDto;
import com.agrifleet.decision.engine.TopsisEngine;
import com.agrifleet.decision.entity.Booking;
import com.agrifleet.decision.entity.Vehicle;
import com.agrifleet.decision.repository.BookingRepository;
import com.agrifleet.decision.repository.DecisionRunRepository;
import com.agrifleet.decision.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DecisionService {

    // Criteria order used consistently across the decision matrix, A+/A-, and persistence.
    private static final String[] CRITERIA_NAMES = {"cost", "distance", "hp", "rating"};
    private static final boolean[] IS_BENEFICIAL = {false, false, true, true}; // cost & distance: lower is better

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final VehicleCompatibilityService compatibilityService;
    private final DecisionRunRepository decisionRunRepository;

    public DecisionService(VehicleRepository vehicleRepository,
                            BookingRepository bookingRepository,
                            VehicleCompatibilityService compatibilityService,
                            DecisionRunRepository decisionRunRepository) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.compatibilityService = compatibilityService;
        this.decisionRunRepository = decisionRunRepository;
    }

    public RankingResponseDto rankVehiclesForBooking(long bookingId, RankRequestDto request) {
        long start = System.nanoTime();

        Booking booking = bookingRepository.findById(bookingId);
        List<String> compatibleTypes = compatibilityService.compatibleTypesFor(booking.getCropType());
        List<Vehicle> candidates = vehicleRepository.findAvailableByType(compatibleTypes);

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No available vehicles compatible with crop type: " + booking.getCropType());
        }

        // Build the M x N decision matrix: rows = vehicles, cols = [cost, distance, hp, rating]
        double[][] matrix = new double[candidates.size()][CRITERIA_NAMES.length];
        double[] distances = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Vehicle v = candidates.get(i);
            double distanceKm = GeoUtils.haversineKm(v.getCurrentLat(), v.getCurrentLng(), booking.getFarmLat(), booking.getFarmLng());
            distances[i] = distanceKm;
            matrix[i][0] = v.hourlyRate();
            matrix[i][1] = distanceKm;
            matrix[i][2] = v.horsepower();
            matrix[i][3] = v.getRating();
        }

        double[] normalizedWeights = normalizeWeights(request);
        TopsisEngine.TopsisResult result = TopsisEngine.rank(matrix, normalizedWeights, IS_BENEFICIAL);

        List<RankedCandidateDto> ranked = new ArrayList<>(candidates.size());
        for (TopsisEngine.AlternativeScore score : result.scores()) {
            Vehicle v = candidates.get(score.index());
            ranked.add(new RankedCandidateDto(
                    v.getVehicleId(),
                    v.getVehicleType(),
                    v.hourlyRate(),
                    distances[score.index()],
                    v.horsepower(),
                    v.getRating(),
                    score.sPlus(),
                    score.sMinus(),
                    score.closeness(),
                    score.rank()
            ));
        }
        ranked.sort((a, b) -> Integer.compare(a.finalRank(), b.finalRank()));

        Map<String, Double> weightsMap = new LinkedHashMap<>();
        for (int i = 0; i < CRITERIA_NAMES.length; i++) {
            weightsMap.put(CRITERIA_NAMES[i], normalizedWeights[i]);
        }

        long decisionRunId = decisionRunRepository.saveRun(
                booking.getFarmerId(), bookingId, weightsMap,
                normalizedWeights, result.idealBest(), result.idealWorst(), CRITERIA_NAMES);
        decisionRunRepository.saveCandidates(decisionRunId, ranked);

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new RankingResponseDto(decisionRunId, bookingId, weightsMap, ranked, elapsedMs);
    }

    /** Normalizes the four raw preference sliders (0-1 each) so they sum to 1.0. */
    private double[] normalizeWeights(RankRequestDto request) {
        double[] raw = {
                request.getCostWeight(),
                request.getDistanceWeight(),
                request.getHorsepowerWeight(),
                request.getRatingWeight()
        };
        double sum = 0.0;
        for (double w : raw) sum += w;
        if (sum <= 0.0) {
            // Fall back to equal weighting rather than dividing by zero.
            return new double[]{0.25, 0.25, 0.25, 0.25};
        }
        double[] normalized = new double[raw.length];
        for (int i = 0; i < raw.length; i++) normalized[i] = raw[i] / sum;
        return normalized;
    }
}
