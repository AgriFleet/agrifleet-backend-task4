package com.agrifleet.decision.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HarvestDelayPredictionRepository {

    private final JdbcTemplate jdbc;

    public HarvestDelayPredictionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long save(long bookingId, double fieldAcres, double rainProbability,
                     int breakdownHistory, String riskTier, double confidence) {
        jdbc.update(
                "INSERT INTO harvest_delay_predictions " +
                        "(booking_id, field_acres, rain_probability, vehicle_breakdown_history, predicted_risk_tier, confidence_score) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                bookingId, fieldAcres, rainProbability, breakdownHistory, riskTier, confidence);
        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }
}
