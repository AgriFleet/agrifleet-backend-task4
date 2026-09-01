package com.agrifleet.decision.service;

import com.agrifleet.decision.dto.RiskPredictionResponseDto;
import com.agrifleet.decision.engine.DecisionTreeRiskEngine;
import com.agrifleet.decision.entity.Booking;
import com.agrifleet.decision.repository.BookingRepository;
import com.agrifleet.decision.repository.HarvestDelayPredictionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class RiskPredictionService {

    private final BookingRepository bookingRepository;
    private final HarvestDelayPredictionRepository predictionRepository;
    private final DecisionTreeRiskEngine tree = new DecisionTreeRiskEngine(/*maxDepth*/ 5, /*minSamplesSplit*/ 6);

    public RiskPredictionService(BookingRepository bookingRepository,
                                 HarvestDelayPredictionRepository predictionRepository) {
        this.bookingRepository = bookingRepository;
        this.predictionRepository = predictionRepository;
    }

    @PostConstruct
    void trainTree() {
        DecisionTreeRiskEngine.TrainingSet data =
                DecisionTreeRiskEngine.generateSyntheticTrainingData(600, /*seed*/ 42L);
        tree.fit(data.features, data.labels);
    }

    /**
     * @param rainProbability          0.0 - 1.0, typically sourced from a weather API by another
     *                                  team member's module; passed in here rather than fetched to
     *                                  keep Task 4 decoupled from Task 1/external integrations.
     * @param vehicleBreakdownHistory  count of prior breakdowns for the assigned vehicle (0 if unknown)
     */
    public RiskPredictionResponseDto predictAndPersist(long bookingId, double rainProbability, int vehicleBreakdownHistory) {
        Booking booking = bookingRepository.findById(bookingId);

        double[] features = {booking.getAcreage(), rainProbability, vehicleBreakdownHistory};
        DecisionTreeRiskEngine.Prediction prediction = tree.predict(features);

        predictionRepository.save(bookingId, booking.getAcreage(), rainProbability,
                vehicleBreakdownHistory, prediction.riskTier, prediction.confidence);

        return new RiskPredictionResponseDto(
                bookingId, prediction.riskTier, prediction.confidence,
                booking.getAcreage(), rainProbability, vehicleBreakdownHistory);
    }
}
