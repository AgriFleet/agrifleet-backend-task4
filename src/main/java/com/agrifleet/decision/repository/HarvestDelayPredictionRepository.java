package com.agrifleet.decision.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class HarvestDelayPredictionRepository {

    private final JdbcTemplate jdbc;

    public HarvestDelayPredictionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long save(
            long bookingId,
            double fieldAcres,
            double rainProbability,
            int breakdownHistory,
            String riskTier,
            double confidence) {

        jdbc.update(
                "INSERT INTO harvest_delay_predictions " +
                        "(booking_id, field_acres, rain_probability, " +
                        "vehicle_breakdown_history, predicted_risk_tier, " +
                        "confidence_score) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",

                bookingId,
                fieldAcres,
                rainProbability,
                breakdownHistory,
                riskTier,
                confidence
        );

        Long id = jdbc.queryForObject(
                "SELECT last_insert_rowid()",
                Long.class
        );

        if (id == null) {
            throw new IllegalStateException(
                    "Failed to obtain prediction ID"
            );
        }

        return id;
    }

    /*
     * Existing prediction history from the original database.
     */
    public List<Map<String, Object>> findAll() {

        return jdbc.queryForList(
                "SELECT " +
                        "prediction_id AS predictionId, " +
                        "booking_id AS bookingId, " +
                        "field_acres AS fieldAcres, " +
                        "rain_probability AS rainProbability, " +
                        "vehicle_breakdown_history AS vehicleBreakdownHistory, " +
                        "predicted_risk_tier AS predictedRiskTier, " +
                        "confidence_score AS confidenceScore, " +
                        "created_at AS createdAt " +
                        "FROM harvest_delay_predictions " +
                        "ORDER BY prediction_id DESC"
        );
    }

    /*
     * Get the most recent stored risk-input record for a booking.
     */
    public Map<String, Object> findLatestByBookingId(
            long bookingId) {

        List<Map<String, Object>> results =
                jdbc.queryForList(
                        "SELECT " +
                                "prediction_id AS predictionId, " +
                                "booking_id AS bookingId, " +
                                "field_acres AS fieldAcres, " +
                                "rain_probability AS rainProbability, " +
                                "vehicle_breakdown_history AS vehicleBreakdownHistory " +
                                "FROM harvest_delay_predictions " +
                                "WHERE booking_id = ? " +
                                "ORDER BY prediction_id DESC " +
                                "LIMIT 1",
                        bookingId
                );

        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }
}