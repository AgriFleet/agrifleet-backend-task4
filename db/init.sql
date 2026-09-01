-- =============================================================================
-- STANDALONE DEV DB FOR TASK 4 (subset of the full AgroFleet_script.sql)
-- Use this ONLY for local development while the rest of the team's tables
-- aren't ready yet. Once the shared agrifleet.db exists, point
-- agrifleet.db.path at it instead and stop using this file.
--
-- Run with:  sqlite3 agrifleet.db < db/init.sql
-- =============================================================================

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS harvest_delay_predictions;
DROP TABLE IF EXISTS ranked_fleet_candidates;
DROP TABLE IF EXISTS topsis_decision_runs;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS vehicles;

CREATE TABLE vehicles (
    vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id INTEGER NOT NULL,
    vehicle_type TEXT NOT NULL,
    specs TEXT NOT NULL,
    pricing TEXT NOT NULL,
    rating REAL DEFAULT 5.00,
    current_lat REAL NOT NULL,
    current_lng REAL NOT NULL,
    availability_status TEXT DEFAULT 'AVAILABLE',
    created_at TEXT DEFAULT (DATETIME('now')),
    updated_at TEXT DEFAULT (DATETIME('now'))
);

CREATE TABLE bookings (
    booking_id INTEGER PRIMARY KEY AUTOINCREMENT,
    farmer_id INTEGER NOT NULL,
    farm_lat REAL NOT NULL,
    farm_lng REAL NOT NULL,
    acreage REAL NOT NULL,
    crop_type TEXT NOT NULL,
    required_window_start TEXT NOT NULL,
    required_window_end TEXT NOT NULL,
    booking_status TEXT DEFAULT 'PENDING',
    created_at TEXT DEFAULT (DATETIME('now'))
);

CREATE TABLE topsis_decision_runs (
    decision_run_id INTEGER PRIMARY KEY AUTOINCREMENT,
    farmer_id INTEGER NOT NULL,
    booking_id INTEGER NOT NULL,
    criteria_weights TEXT NOT NULL,
    ideal_best_vector_a_plus TEXT NOT NULL,
    ideal_worst_vector_a_minus TEXT NOT NULL,
    created_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

CREATE TABLE ranked_fleet_candidates (
    ranking_id INTEGER PRIMARY KEY AUTOINCREMENT,
    decision_run_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    hourly_rate REAL NOT NULL,
    distance_km REAL NOT NULL,
    horsepower INTEGER NOT NULL,
    rating_score REAL NOT NULL,
    separation_s_plus REAL NOT NULL,
    separation_s_minus REAL NOT NULL,
    relative_closeness_c REAL NOT NULL,
    final_rank INTEGER NOT NULL,
    FOREIGN KEY (decision_run_id) REFERENCES topsis_decision_runs(decision_run_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

CREATE TABLE harvest_delay_predictions (
    prediction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER NOT NULL,
    field_acres REAL NOT NULL,
    rain_probability REAL NOT NULL,
    vehicle_breakdown_history INTEGER DEFAULT 0,
    predicted_risk_tier TEXT NOT NULL,
    confidence_score REAL NOT NULL,
    created_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- Sample data mirrors the shared script so the two of you can develop against
-- realistic numbers immediately.
INSERT INTO vehicles (vehicle_id, owner_id, vehicle_type, specs, pricing, rating, current_lat, current_lng, availability_status) VALUES
(1, 1, 'COMBINE_HARVESTER', '{"hp": 380, "cutterbar_width_m": 7.5, "weight_tonnes": 14.8, "fuel_burn_l_per_hr": 28.5}', '{"hourly_rate": 145.00, "per_acre_rate": 48.00}', 4.92, 8.311400, 80.403700, 'AVAILABLE'),
(2, 1, 'COMBINE_HARVESTER', '{"hp": 290, "cutterbar_width_m": 5.8, "weight_tonnes": 12.2, "fuel_burn_l_per_hr": 22.0}', '{"hourly_rate": 115.00, "per_acre_rate": 42.00}', 4.75, 8.324500, 80.412000, 'AVAILABLE'),
(3, 2, 'COMBINE_HARVESTER', '{"hp": 210, "cutterbar_width_m": 4.2, "weight_tonnes": 9.5, "fuel_burn_l_per_hr": 17.5}',  '{"hourly_rate": 95.00, "per_acre_rate": 36.00}',  4.60, 8.289000, 80.385000, 'AVAILABLE'),
(4, 3, '4WD_TRACTOR',       '{"hp": 140, "pto_hp": 115, "weight_tonnes": 6.1, "fuel_burn_l_per_hr": 12.0}',             '{"hourly_rate": 65.00, "per_acre_rate": 22.00}',  4.88, 8.351000, 80.420000, 'AVAILABLE'),
(5, 3, '4WD_TRACTOR',       '{"hp": 110, "pto_hp": 90, "weight_tonnes": 5.2, "fuel_burn_l_per_hr": 9.8}',               '{"hourly_rate": 55.00, "per_acre_rate": 18.00}',  4.50, 8.338000, 80.395000, 'AVAILABLE'),
(6, 4, 'SQUARE_BALER',      '{"hp": 100, "bale_size_cm": "120x90", "weight_tonnes": 7.8, "fuel_burn_l_per_hr": 14.0}', '{"hourly_rate": 80.00, "per_acre_rate": 28.00}',  4.82, 8.295000, 80.440000, 'AVAILABLE'),
(7, 5, 'BOOM_SPRAYER',      '{"hp": 175, "boom_width_m": 24.0, "tank_capacity_l": 3000, "weight_tonnes": 8.0}',         '{"hourly_rate": 90.00, "per_acre_rate": 16.00}',  4.70, 8.305000, 80.370000, 'AVAILABLE'),
(8, 6, 'COMBINE_HARVESTER', '{"hp": 340, "cutterbar_width_m": 6.8, "weight_tonnes": 13.9, "fuel_burn_l_per_hr": 26.0}', '{"hourly_rate": 135.00, "per_acre_rate": 46.00}', 4.85, 8.365000, 80.450000, 'AVAILABLE');

INSERT INTO bookings (booking_id, farmer_id, farm_lat, farm_lng, acreage, crop_type, required_window_start, required_window_end, booking_status) VALUES
(1, 1, 8.335000, 80.445000, 32.50, 'PADDY',     '2026-08-25 07:30:00', '2026-08-25 18:00:00', 'PENDING'),
(2, 2, 8.362000, 80.412000, 18.00, 'PADDY',     '2026-08-25 08:00:00', '2026-08-25 17:00:00', 'PENDING'),
(3, 3, 8.298000, 80.362000, 45.00, 'CORN',      '2026-08-25 07:00:00', '2026-08-25 19:30:00', 'PENDING'),
(4, 4, 8.275000, 80.418000, 12.00, 'WHEAT',     '2026-08-25 09:00:00', '2026-08-25 15:00:00', 'PENDING');
