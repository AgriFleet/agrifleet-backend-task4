package com.agrifleet.decision.repository;

import com.agrifleet.decision.entity.Vehicle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VehicleRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<Vehicle> mapper;

    public VehicleRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;

        this.mapper = (ResultSet rs, int rowNum) -> {
            Vehicle v = new Vehicle();

            v.setVehicleId(rs.getLong("vehicle_id"));
            v.setOwnerId(rs.getLong("owner_id"));
            v.setVehicleType(rs.getString("vehicle_type"));

            try {
                v.setSpecs(objectMapper.readTree(rs.getString("specs")));
                v.setPricing(objectMapper.readTree(rs.getString("pricing")));
            } catch (Exception e) {
                throw new SQLException(
                        "Failed to parse vehicle JSON columns for vehicle_id="
                                + v.getVehicleId(), e
                );
            }

            v.setRating(rs.getDouble("rating"));
            v.setCurrentLat(rs.getDouble("current_lat"));
            v.setCurrentLng(rs.getDouble("current_lng"));
            v.setAvailabilityStatus(rs.getString("availability_status"));

            return v;
        };
    }

    public List<Vehicle> findAvailableByType(List<String> compatibleTypes) {

        String placeholders = String.join(
                ",",
                compatibleTypes.stream()
                        .map(t -> "?")
                        .toList()
        );

        String sql =
                "SELECT * FROM vehicles " +
                        "WHERE availability_status = 'AVAILABLE' " +
                        "AND vehicle_type IN (" + placeholders + ")";

        return jdbc.query(
                sql,
                mapper,
                compatibleTypes.toArray()
        );
    }

    public List<Vehicle> findAllAvailable() {
        return jdbc.query(
                "SELECT * FROM vehicles " +
                        "WHERE availability_status = 'AVAILABLE'",
                mapper
        );
    }

    public Vehicle findById(long vehicleId) {

        List<Vehicle> results = jdbc.query(
                "SELECT * FROM vehicles WHERE vehicle_id = ?",
                mapper,
                vehicleId
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vehicle not found: " + vehicleId
            );
        }

        return results.get(0);
    }
}