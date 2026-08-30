package com.agrifleet.decision.repository;

import com.agrifleet.decision.dto.RankedCandidateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class DecisionRunRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DecisionRunRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** Inserts the run header and returns the generated decision_run_id. */
    public long saveRun(long farmerId, long bookingId, Map<String, Double> weights,
                         double[] criteriaOrder, double[] idealBest, double[] idealWorst,
                         String[] criteriaNames) {
        try {
            String weightsJson = objectMapper.writeValueAsString(weights);
            String aPlusJson = objectMapper.writeValueAsString(toNamedMap(criteriaNames, idealBest));
            String aMinusJson = objectMapper.writeValueAsString(toNamedMap(criteriaNames, idealWorst));

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO topsis_decision_runs " +
                                "(farmer_id, booking_id, criteria_weights, ideal_best_vector_a_plus, ideal_worst_vector_a_minus) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, farmerId);
                ps.setLong(2, bookingId);
                ps.setString(3, weightsJson);
                ps.setString(4, aPlusJson);
                ps.setString(5, aMinusJson);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : -1L;
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist TOPSIS decision run", e);
        }
    }

    public void saveCandidates(long decisionRunId, List<RankedCandidateDto> candidates) {
        String sql = "INSERT INTO ranked_fleet_candidates " +
                "(decision_run_id, vehicle_id, hourly_rate, distance_km, horsepower, rating_score, " +
                "separation_s_plus, separation_s_minus, relative_closeness_c, final_rank) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbc.batchUpdate(sql, candidates, candidates.size(), (ps, c) -> {
            ps.setLong(1, decisionRunId);
            ps.setLong(2, c.vehicleId());
            ps.setDouble(3, c.hourlyRate());
            ps.setDouble(4, c.distanceKm());
            ps.setInt(5, c.horsepower());
            ps.setDouble(6, c.ratingScore());
            ps.setDouble(7, c.separationSPlus());
            ps.setDouble(8, c.separationSMinus());
            ps.setDouble(9, c.relativeClosenessC());
            ps.setInt(10, c.finalRank());
        });
    }

    private Map<String, Double> toNamedMap(String[] names, double[] values) {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], values[i]);
        }
        return map;
    }
}
