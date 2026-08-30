package com.agrifleet.decision.repository;

import com.agrifleet.decision.entity.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Booking> mapper = (rs, rowNum) -> {
        Booking b = new Booking();
        b.setBookingId(rs.getLong("booking_id"));
        b.setFarmerId(rs.getLong("farmer_id"));
        b.setFarmLat(rs.getDouble("farm_lat"));
        b.setFarmLng(rs.getDouble("farm_lng"));
        b.setAcreage(rs.getDouble("acreage"));
        b.setCropType(rs.getString("crop_type"));
        b.setRequiredWindowStart(rs.getString("required_window_start"));
        b.setRequiredWindowEnd(rs.getString("required_window_end"));
        b.setBookingStatus(rs.getString("booking_status"));
        return b;
    };

    public Booking findById(long bookingId) {
        List<Booking> results = jdbc.query("SELECT * FROM bookings WHERE booking_id = ?", mapper, bookingId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        return results.get(0);
    }
}
