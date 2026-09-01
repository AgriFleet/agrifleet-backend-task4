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

    private static final String[] CRITERIA_NAMES = {
            "cost",
            "distance",
            "hp",
            "rating"
    };

    /*
     * TOPSIS:
     *
     * cost     -> lower is better
     * distance -> lower is better
     * hp       -> higher is better
     * rating   -> higher is better
     */
    private static final boolean[] IS_BENEFICIAL = {
            false,
            false,
            true,
            true
    };

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final VehicleCompatibilityService compatibilityService;
    private final DecisionRunRepository decisionRunRepository;

    public DecisionService(
            VehicleRepository vehicleRepository,
            BookingRepository bookingRepository,
            VehicleCompatibilityService compatibilityService,
            DecisionRunRepository decisionRunRepository) {

        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.compatibilityService = compatibilityService;
        this.decisionRunRepository = decisionRunRepository;
    }

    public RankingResponseDto rankVehiclesForBooking(
            long bookingId,
            RankRequestDto request) {

        long start = System.nanoTime();

        /*
         * REAL BOOKING FROM DATABASE
         */
        Booking booking =
                bookingRepository.findById(bookingId);

        /*
         * Crop compatibility is calculated by backend.
         */
        List<String> compatibleTypes =
                compatibilityService.compatibleTypesFor(
                        booking.getCropType()
                );

        /*
         * REAL VEHICLES FROM DATABASE
         */
        List<Vehicle> candidates =
                vehicleRepository.findAvailableByType(
                        compatibleTypes
                );

        if (candidates.isEmpty()) {

            throw new IllegalStateException(
                    "No available vehicles compatible with crop type: "
                            + booking.getCropType()
            );
        }

        /*
         * Build TOPSIS decision matrix.
         *
         * Columns:
         * [cost, distance, hp, rating]
         */
        double[][] matrix =
                new double[
                        candidates.size()
                        ][CRITERIA_NAMES.length];

        double[] distances =
                new double[candidates.size()];

        for (int i = 0;
             i < candidates.size();
             i++) {

            Vehicle vehicle =
                    candidates.get(i);

            /*
             * Distance is calculated using
             * REAL GPS coordinates from DB.
             */
            double distanceKm =
                    GeoUtils.haversineKm(
                            vehicle.getCurrentLat(),
                            vehicle.getCurrentLng(),
                            booking.getFarmLat(),
                            booking.getFarmLng()
                    );

            distances[i] = distanceKm;

            /*
             * REAL vehicle data from DB.
             */
            matrix[i][0] =
                    vehicle.hourlyRate();

            matrix[i][1] =
                    distanceKm;

            matrix[i][2] =
                    vehicle.horsepower();

            matrix[i][3] =
                    vehicle.getRating();
        }

        double[] normalizedWeights =
                normalizeWeights(request);

        /*
         * ACTUAL TOPSIS ALGORITHM
         */
        TopsisEngine.TopsisResult result =
                TopsisEngine.rank(
                        matrix,
                        normalizedWeights,
                        IS_BENEFICIAL
                );

        List<RankedCandidateDto> ranked =
                new ArrayList<>(
                        candidates.size()
                );

        for (TopsisEngine.AlternativeScore score
                : result.scores()) {

            Vehicle vehicle =
                    candidates.get(score.index());

            ranked.add(
                    new RankedCandidateDto(
                            vehicle.getVehicleId(),
                            vehicle.getVehicleType(),
                            vehicle.hourlyRate(),
                            distances[score.index()],
                            vehicle.horsepower(),
                            vehicle.getRating(),
                            score.sPlus(),
                            score.sMinus(),
                            score.closeness(),
                            score.rank()
                    )
            );
        }

        ranked.sort(
                (a, b) ->
                        Integer.compare(
                                a.finalRank(),
                                b.finalRank()
                        )
        );

        Map<String, Double> weightsMap =
                new LinkedHashMap<>();

        for (int i = 0;
             i < CRITERIA_NAMES.length;
             i++) {

            weightsMap.put(
                    CRITERIA_NAMES[i],
                    normalizedWeights[i]
            );
        }

        /*
         * Persist result using EXISTING database tables.
         */
        long decisionRunId =
                decisionRunRepository.saveRun(
                        booking.getFarmerId(),
                        bookingId,
                        weightsMap,
                        normalizedWeights,
                        result.idealBest(),
                        result.idealWorst(),
                        CRITERIA_NAMES
                );

        decisionRunRepository.saveCandidates(
                decisionRunId,
                ranked
        );

        double elapsedMs =
                (System.nanoTime() - start)
                        / 1_000_000.0;

        return new RankingResponseDto(
                decisionRunId,
                bookingId,
                weightsMap,
                ranked,
                elapsedMs
        );
    }

    private double[] normalizeWeights(
            RankRequestDto request) {

        double[] raw = {
                request.getCostWeight(),
                request.getDistanceWeight(),
                request.getHorsepowerWeight(),
                request.getRatingWeight()
        };

        double sum = 0.0;

        for (double weight : raw) {

            if (weight < 0.0 || weight > 1.0) {

                throw new IllegalArgumentException(
                        "Each TOPSIS weight must be between 0 and 1"
                );
            }

            sum += weight;
        }

        /*
         * Do not silently turn an invalid all-zero
         * preference vector into equal weights.
         */
        if (sum <= 0.0) {

            throw new IllegalArgumentException(
                    "At least one TOPSIS weight must be greater than zero"
            );
        }

        double[] normalized =
                new double[raw.length];

        for (int i = 0;
             i < raw.length;
             i++) {

            normalized[i] =
                    raw[i] / sum;
        }

        return normalized;
    }
}