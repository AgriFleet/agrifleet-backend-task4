package com.agrifleet.decision.service;

import com.agrifleet.decision.dto.RiskPredictionResponseDto;
import com.agrifleet.decision.engine.DecisionTreeRiskEngine;
import com.agrifleet.decision.entity.Booking;
import com.agrifleet.decision.repository.BookingRepository;
import com.agrifleet.decision.repository.HarvestDelayPredictionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RiskPredictionService {

    private final BookingRepository bookingRepository;
    private final HarvestDelayPredictionRepository predictionRepository;

    private final DecisionTreeRiskEngine tree =
            new DecisionTreeRiskEngine(5, 6);

    public RiskPredictionService(
            BookingRepository bookingRepository,
            HarvestDelayPredictionRepository predictionRepository) {

        this.bookingRepository = bookingRepository;
        this.predictionRepository = predictionRepository;
    }

    @PostConstruct
    void trainTree() {

        DecisionTreeRiskEngine.TrainingSet data =
                DecisionTreeRiskEngine.generateSyntheticTrainingData(
                        600,
                        42L
                );

        tree.fit(
                data.features,
                data.labels
        );
    }

    /*
     * Existing frontend calls:
     *
     * POST /decision/delays/predict?bookingId=X
     *
     * Therefore the backend obtains the available prediction
     * input values from the existing database.
     */
    public RiskPredictionResponseDto predictFromStoredData(
            long bookingId) {

        Booking booking =
                bookingRepository.findById(bookingId);

        Map<String, Object> stored =
                predictionRepository.findLatestByBookingId(
                        bookingId
                );

        double rainProbability;
        int breakdownHistory;

        if (stored != null) {

            Object rain =
                    stored.get("rainProbability");

            Object breakdown =
                    stored.get("vehicleBreakdownHistory");

            rainProbability =
                    rain instanceof Number
                            ? ((Number) rain).doubleValue()
                            : 0.0;

            breakdownHistory =
                    breakdown instanceof Number
                            ? ((Number) breakdown).intValue()
                            : 0;

        } else {

            /*
             * There is no risk-input record for this booking
             * in the original database.
             *
             * We cannot invent a weather/telemetry value.
             * The neutral value is therefore used only when
             * the original DB contains no record.
             */
            rainProbability = 0.0;
            breakdownHistory = 0;
        }

        return predictAndPersist(
                booking,
                rainProbability,
                breakdownHistory
        );
    }

    /*
     * Core prediction method.
     */
    private RiskPredictionResponseDto predictAndPersist(
            Booking booking,
            double rainProbability,
            int vehicleBreakdownHistory) {

        double[] features = {
                booking.getAcreage(),
                rainProbability,
                vehicleBreakdownHistory
        };

        DecisionTreeRiskEngine.Prediction prediction =
                tree.predict(features);

        predictionRepository.save(
                booking.getBookingId(),
                booking.getAcreage(),
                rainProbability,
                vehicleBreakdownHistory,
                prediction.riskTier,
                prediction.confidence
        );

        return new RiskPredictionResponseDto(
                booking.getBookingId(),
                prediction.riskTier,
                prediction.confidence,
                booking.getAcreage(),
                rainProbability,
                vehicleBreakdownHistory
        );
    }
}