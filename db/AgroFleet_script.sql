-- =============================================================================
-- AGRIFLEET: INTELLIGENT DECISION SUPPORT & MACHINERY LOGISTICS SYSTEM (IDSS)
-- COMPLETE SQLITE INITIALIZATION & NUMERIC SAMPLE DATA SCRIPT
-- ALL IDENTIFIERS: 1, 2, 3... (INTEGER PRIMARY / FOREIGN KEYS)
-- =============================================================================

PRAGMA foreign_keys = ON;

-- Drop child tables first, then parent tables
DROP TABLE IF EXISTS ga_execution_logs;
DROP TABLE IF EXISTS daily_operator_tours;
DROP TABLE IF EXISTS harvest_delay_predictions;
DROP TABLE IF EXISTS ranked_fleet_candidates;
DROP TABLE IF EXISTS topsis_decision_runs;
DROP TABLE IF EXISTS mst_logistics_backbone;
DROP TABLE IF EXISTS network_bridges_and_cuts;
DROP TABLE IF EXISTS allocated_assignments;
DROP TABLE IF EXISTS allocation_batches;
DROP TABLE IF EXISTS route_execution_cache;
DROP TABLE IF EXISTS road_edges;
DROP TABLE IF EXISTS road_nodes;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS vehicles;

-- =============================================================================
-- 0. SHARED CORE DOMAIN SCHEMA (FLEET & BOOKINGS)
-- =============================================================================

CREATE TABLE vehicles (
    vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id INTEGER NOT NULL,
    vehicle_type TEXT NOT NULL,
    specs TEXT NOT NULL,               -- JSON String: {"hp": 380, "cutterbar_width_m": 7.5, ...}
    pricing TEXT NOT NULL,             -- JSON String: {"hourly_rate": 145.00, "per_acre_rate": 48.00}
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

-- =============================================================================
-- 1. TASK 1: ROUTE OPTIMIZATION SERVICE (route_service_db)
-- =============================================================================

CREATE TABLE road_nodes (
    node_id INTEGER PRIMARY KEY,
    node_name TEXT,
    lat REAL NOT NULL,
    lng REAL NOT NULL,
    elevation_meters REAL DEFAULT 0.0,
    is_farm_gate INTEGER DEFAULT 0,    -- 0 = False, 1 = True
    is_depot INTEGER DEFAULT 0         -- 0 = False, 1 = True
);

CREATE TABLE road_edges (
    edge_id INTEGER PRIMARY KEY AUTOINCREMENT,
    u_node INTEGER NOT NULL,
    v_node INTEGER NOT NULL,
    base_distance_km REAL NOT NULL,
    surface_type TEXT DEFAULT 'PAVED_HIGHWAY',
    max_weight_tonnes REAL DEFAULT 40.0,
    weather_penalty_multiplier REAL DEFAULT 1.0,
    computed_weight REAL NOT NULL,
    FOREIGN KEY (u_node) REFERENCES road_nodes(node_id) ON DELETE CASCADE,
    FOREIGN KEY (v_node) REFERENCES road_nodes(node_id) ON DELETE CASCADE
);

CREATE TABLE route_execution_cache (
    route_id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id INTEGER NOT NULL,
    origin_node INTEGER NOT NULL,
    destination_node INTEGER NOT NULL,
    path_node_sequence TEXT NOT NULL,  -- JSON Array: [1, 2, 3, 4]
    total_distance_km REAL NOT NULL,
    total_travel_time_mins REAL NOT NULL,
    nodes_visited_count INTEGER NOT NULL,
    algorithm_used TEXT NOT NULL,      -- ASTAR, DIJKSTRA
    computed_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

-- =============================================================================
-- 2. TASK 2: RESOURCE ALLOCATION SERVICE (allocation_service_db)
-- =============================================================================

CREATE TABLE allocation_batches (
    batch_id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_type TEXT NOT NULL,          -- SCHEDULED_BATCH, REALTIME_GREEDY
    matrix_dimensions TEXT NOT NULL,
    cost_matrix_payload TEXT NOT NULL, -- JSON 2D Matrix
    candidate_vehicle_ids TEXT NOT NULL,
    candidate_booking_ids TEXT NOT NULL,
    total_network_cost REAL NOT NULL,
    execution_time_ms REAL NOT NULL,
    created_at TEXT DEFAULT (DATETIME('now'))
);

CREATE TABLE allocated_assignments (
    assignment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    booking_id INTEGER NOT NULL,
    deadhead_distance_km REAL NOT NULL,
    estimated_eta TEXT NOT NULL,
    assignment_status TEXT DEFAULT 'CONFIRMED',
    created_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (batch_id) REFERENCES allocation_batches(batch_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- =============================================================================
-- 3. TASK 3: NETWORK ANALYSIS SERVICE (network_service_db)
-- =============================================================================

CREATE TABLE network_bridges_and_cuts (
    cut_id INTEGER PRIMARY KEY AUTOINCREMENT,
    region_id INTEGER NOT NULL,
    u_node INTEGER NOT NULL,
    v_node INTEGER NOT NULL,
    is_bridge INTEGER DEFAULT 1,       -- 0 = False, 1 = True (Tarjan: low[v] > tin[u])
    is_articulation_point INTEGER DEFAULT 0,
    max_tonnage_limit REAL NOT NULL,
    is_severed INTEGER DEFAULT 0,
    isolated_subgraph_nodes TEXT,      -- JSON Array: [3, 4]
    discovered_at TEXT DEFAULT (DATETIME('now'))
);

CREATE TABLE mst_logistics_backbone (
    backbone_id INTEGER PRIMARY KEY AUTOINCREMENT,
    region_id INTEGER NOT NULL,
    mst_edge_list TEXT NOT NULL,       -- JSON Array of Kruskal/DSU edges
    total_backbone_cost REAL NOT NULL,
    last_recalculated TEXT DEFAULT (DATETIME('now'))
);

-- =============================================================================
-- 4. TASK 4: INTELLIGENT DECISION SERVICE (decision_service_db)
-- =============================================================================

CREATE TABLE topsis_decision_runs (
    decision_run_id INTEGER PRIMARY KEY AUTOINCREMENT,
    farmer_id INTEGER NOT NULL,
    booking_id INTEGER NOT NULL,
    criteria_weights TEXT NOT NULL,          -- JSON Object
    ideal_best_vector_a_plus TEXT NOT NULL,  -- JSON Object
    ideal_worst_vector_a_minus TEXT NOT NULL,-- JSON Object
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

-- =============================================================================
-- 5. TASK 5: TOUR & MULTI-JOB OPTIMIZATION SERVICE (optimization_service_db)
-- =============================================================================

CREATE TABLE daily_operator_tours (
    tour_id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id INTEGER NOT NULL,
    operator_id INTEGER NOT NULL,
    tour_date TEXT NOT NULL,
    max_shift_hours REAL NOT NULL,
    total_tour_distance_km REAL NOT NULL,
    total_acreage_harvested REAL NOT NULL,
    optimal_stop_sequence TEXT NOT NULL, -- JSON Array tour permutation
    tour_status TEXT DEFAULT 'OPTIMIZED',
    created_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

CREATE TABLE ga_execution_logs (
    ga_run_id INTEGER PRIMARY KEY AUTOINCREMENT,
    tour_id INTEGER NOT NULL,
    num_farm_stops INTEGER NOT NULL,
    population_size INTEGER NOT NULL,
    generations_count INTEGER NOT NULL,
    crossover_rate REAL NOT NULL,
    mutation_rate REAL NOT NULL,
    initial_distance_km REAL NOT NULL,
    final_distance_km REAL NOT NULL,
    fitness_convergence_curve TEXT NOT NULL, -- JSON Array for plotting
    execution_time_ms REAL NOT NULL,
    created_at TEXT DEFAULT (DATETIME('now')),
    FOREIGN KEY (tour_id) REFERENCES daily_operator_tours(tour_id) ON DELETE CASCADE
);

-- =============================================================================
-- 6. FULL SAMPLE SEED DATA (SEQUENTIAL IDS: 1, 2, 3...)
-- =============================================================================

-- Vehicles (1 to 8)
INSERT INTO vehicles (vehicle_id, owner_id, vehicle_type, specs, pricing, rating, current_lat, current_lng, availability_status) VALUES
(1, 1, 'COMBINE_HARVESTER', '{"hp": 380, "cutterbar_width_m": 7.5, "weight_tonnes": 14.8, "fuel_burn_l_per_hr": 28.5}', '{"hourly_rate": 145.00, "per_acre_rate": 48.00}', 4.92, 8.311400, 80.403700, 'AVAILABLE'),
(2, 1, 'COMBINE_HARVESTER', '{"hp": 290, "cutterbar_width_m": 5.8, "weight_tonnes": 12.2, "fuel_burn_l_per_hr": 22.0}', '{"hourly_rate": 115.00, "per_acre_rate": 42.00}', 4.75, 8.324500, 80.412000, 'AVAILABLE'),
(3, 2, 'COMBINE_HARVESTER', '{"hp": 210, "cutterbar_width_m": 4.2, "weight_tonnes": 9.5, "fuel_burn_l_per_hr": 17.5}',  '{"hourly_rate": 95.00, "per_acre_rate": 36.00}',  4.60, 8.289000, 80.385000, 'AVAILABLE'),
(4, 3, '4WD_TRACTOR',       '{"hp": 140, "pto_hp": 115, "weight_tonnes": 6.1, "fuel_burn_l_per_hr": 12.0}',             '{"hourly_rate": 65.00, "per_acre_rate": 22.00}',  4.88, 8.351000, 80.420000, 'AVAILABLE'),
(5, 3, '4WD_TRACTOR',       '{"hp": 110, "pto_hp": 90, "weight_tonnes": 5.2, "fuel_burn_l_per_hr": 9.8}',               '{"hourly_rate": 55.00, "per_acre_rate": 18.00}',  4.50, 8.338000, 80.395000, 'AVAILABLE'),
(6, 4, 'SQUARE_BALER',      '{"hp": 100, "bale_size_cm": "120x90", "weight_tonnes": 7.8, "fuel_burn_l_per_hr": 14.0}', '{"hourly_rate": 80.00, "per_acre_rate": 28.00}',  4.82, 8.295000, 80.440000, 'AVAILABLE'),
(7, 5, 'BOOM_SPRAYER',      '{"hp": 175, "boom_width_m": 24.0, "tank_capacity_l": 3000, "weight_tonnes": 8.0}',         '{"hourly_rate": 90.00, "per_acre_rate": 16.00}',  4.70, 8.305000, 80.370000, 'AVAILABLE'),
(8, 6, 'COMBINE_HARVESTER', '{"hp": 340, "cutterbar_width_m": 6.8, "weight_tonnes": 13.9, "fuel_burn_l_per_hr": 26.0}', '{"hourly_rate": 135.00, "per_acre_rate": 46.00}', 4.85, 8.365000, 80.450000, 'AVAILABLE');

-- Bookings (1 to 8)
INSERT INTO bookings (booking_id, farmer_id, farm_lat, farm_lng, acreage, crop_type, required_window_start, required_window_end, booking_status) VALUES
(1, 1, 8.335000, 80.445000, 32.50, 'PADDY',     '2026-08-25 07:30:00', '2026-08-25 18:00:00', 'PENDING'),
(2, 2, 8.362000, 80.412000, 18.00, 'PADDY',     '2026-08-25 08:00:00', '2026-08-25 17:00:00', 'PENDING'),
(3, 3, 8.298000, 80.362000, 45.00, 'CORN',      '2026-08-25 07:00:00', '2026-08-25 19:30:00', 'PENDING'),
(4, 4, 8.275000, 80.418000, 12.00, 'WHEAT',     '2026-08-25 09:00:00', '2026-08-25 15:00:00', 'PENDING'),
(5, 5, 8.380000, 80.460000, 60.00, 'SUGARCANE', '2026-08-26 06:30:00', '2026-08-26 19:00:00', 'PENDING'),
(6, 6, 8.315000, 80.485000, 22.00, 'PADDY',     '2026-08-26 08:00:00', '2026-08-26 16:30:00', 'PENDING'),
(7, 7, 8.260000, 80.350000, 15.50, 'CORN',      '2026-08-26 08:30:00', '2026-08-26 17:00:00', 'PENDING'),
(8, 8, 8.342000, 80.380000, 28.00, 'PADDY',     '2026-08-26 07:00:00', '2026-08-26 18:00:00', 'PENDING');

-- Task 1: Road Nodes (1 to 10)
INSERT INTO road_nodes (node_id, node_name, lat, lng, elevation_meters, is_farm_gate, is_depot) VALUES
(1,  'Main Machinery Depot Alpha',   8.311400, 80.403700, 92.0, 0, 1),
(2,  'Junction Medawachchiya Cross', 8.324500, 80.412000, 95.5, 0, 0),
(3,  'Canal Bridge Crossing Alpha',  8.338000, 80.428000, 89.0, 0, 0),
(4,  'Farm Gate Plot 1 (Booking 1)', 8.335000, 80.445000, 87.5, 1, 0),
(5,  'Farm Gate Plot 2 (Booking 2)', 8.362000, 80.412000, 98.0, 1, 0),
(6,  'South Agricultural Bypass',    8.295000, 80.395000, 94.0, 0, 0),
(7,  'Farm Gate Plot 3 (Booking 3)', 8.298000, 80.362000, 91.0, 1, 0),
(8,  'Gravel Road Intersect South',  8.275000, 80.390000, 90.0, 0, 0),
(9,  'Farm Gate Plot 4 (Booking 4)', 8.275000, 80.418000, 88.0, 1, 0),
(10, 'Sub-Depot Beta (East Sector)', 8.365000, 80.450000, 96.0, 0, 1);

-- Task 1: Road Edges (1 to 16)
INSERT INTO road_edges (edge_id, u_node, v_node, base_distance_km, surface_type, max_weight_tonnes, weather_penalty_multiplier, computed_weight) VALUES
(1,  1, 2, 1.75, 'PAVED_HIGHWAY', 45.0, 1.00, 1.750),
(2,  2, 1, 1.75, 'PAVED_HIGHWAY', 45.0, 1.00, 1.750),
(3,  2, 3, 2.20, 'GRAVEL',        28.0, 1.25, 2.750),
(4,  3, 2, 2.20, 'GRAVEL',        28.0, 1.25, 2.750),
(5,  3, 4, 1.90, 'DIRT_TRACK',    16.0, 1.60, 3.040),
(6,  4, 3, 1.90, 'DIRT_TRACK',    16.0, 1.60, 3.040),
(7,  2, 5, 4.20, 'PAVED_HIGHWAY', 40.0, 1.00, 4.200),
(8,  5, 2, 4.20, 'PAVED_HIGHWAY', 40.0, 1.00, 4.200),
(9,  1, 6, 2.10, 'PAVED_HIGHWAY', 45.0, 1.00, 2.100),
(10, 6, 1, 2.10, 'PAVED_HIGHWAY', 45.0, 1.00, 2.100),
(11, 6, 7, 3.70, 'GRAVEL',        25.0, 1.20, 4.440),
(12, 7, 6, 3.70, 'GRAVEL',        25.0, 1.20, 4.440),
(13, 6, 8, 2.30, 'DIRT_TRACK',    18.0, 1.50, 3.450),
(14, 8, 9, 3.10, 'MUDDY_FIELD',   12.0, 2.10, 6.510),
(15, 3, 10, 3.60, 'PAVED_HIGHWAY', 40.0, 1.00, 3.600),
(16, 10, 4, 3.40, 'GRAVEL',        22.0, 1.30, 4.420);

-- Task 1: Route Execution Cache (1 to 4)
INSERT INTO route_execution_cache (route_id, vehicle_id, origin_node, destination_node, path_node_sequence, total_distance_km, total_travel_time_mins, nodes_visited_count, algorithm_used) VALUES
(1, 1, 1, 4, '[1, 2, 3, 4]', 5.85, 14.60, 4, 'ASTAR'),
(2, 1, 1, 4, '[1, 2, 3, 4]', 5.85, 14.60, 9, 'DIJKSTRA'),
(3, 3, 1, 7, '[1, 6, 7]',    5.80, 16.20, 3, 'ASTAR'),
(4, 3, 1, 7, '[1, 6, 7]',    5.80, 16.20, 8, 'DIJKSTRA');

-- Task 2: Allocation Batches (1, 2)
INSERT INTO allocation_batches (batch_id, batch_type, matrix_dimensions, cost_matrix_payload, candidate_vehicle_ids, candidate_booking_ids, total_network_cost, execution_time_ms) VALUES
(1, 'SCHEDULED_BATCH', '4x4', '[[5.85, 7.60, 6.20, 9.40], [4.90, 4.20, 8.10, 8.50], [9.10, 11.20, 5.80, 4.30], [3.40, 6.10, 12.00, 7.80]]', '[1, 2, 3, 8]', '[1, 2, 3, 4]', 18.25, 4.12),
(2, 'REALTIME_GREEDY', '1x1', '[[3.10]]', '[4]', '[8]', 3.10, 0.45);

-- Task 2: Allocated Assignments (1 to 4)
INSERT INTO allocated_assignments (assignment_id, batch_id, vehicle_id, booking_id, deadhead_distance_km, estimated_eta, assignment_status) VALUES
(1, 1, 1, 1, 5.85, '2026-08-25 07:15:00', 'CONFIRMED'),
(2, 1, 2, 2, 4.20, '2026-08-25 07:45:00', 'CONFIRMED'),
(3, 1, 3, 3, 5.80, '2026-08-25 06:40:00', 'CONFIRMED'),
(4, 1, 8, 4, 7.80, '2026-08-25 08:35:00', 'CONFIRMED');

-- Task 3: Network Bridges & Cuts (1 to 3)
INSERT INTO network_bridges_and_cuts (cut_id, region_id, u_node, v_node, is_bridge, is_articulation_point, max_tonnage_limit, is_severed, isolated_subgraph_nodes) VALUES
(1, 101, 2, 3, 1, 1, 28.00, 0, '[3, 4]'),
(2, 102, 6, 7, 1, 0, 25.00, 0, '[7]'),
(3, 102, 8, 9, 1, 0, 12.00, 0, '[9]');

-- Task 3: MST Logistics Backbone (1)
INSERT INTO mst_logistics_backbone (backbone_id, region_id, mst_edge_list, total_backbone_cost) VALUES
(1, 101, '[{"u": 1, "v": 2, "w": 1.75}, {"u": 1, "v": 6, "w": 2.10}, {"u": 2, "v": 3, "w": 2.20}, {"u": 3, "v": 4, "w": 1.90}, {"u": 6, "v": 8, "w": 2.30}, {"u": 8, "v": 9, "w": 3.10}, {"u": 3, "v": 10, "w": 3.60}, {"u": 6, "v": 7, "w": 3.70}, {"u": 2, "v": 5, "w": 4.20}]', 24.85);

-- Task 4: TOPSIS Decision Runs (1)
INSERT INTO topsis_decision_runs (decision_run_id, farmer_id, booking_id, criteria_weights, ideal_best_vector_a_plus, ideal_worst_vector_a_minus) VALUES
(1, 1, 1, '{"cost": 0.35, "distance": 0.25, "hp": 0.20, "rating": 0.20}', '{"cost": 0.115, "distance": 0.082, "hp": 0.125, "rating": 0.104}', '{"cost": 0.185, "distance": 0.188, "hp": 0.075, "rating": 0.085}');

-- Task 4: Ranked Fleet Candidates (1 to 4)
INSERT INTO ranked_fleet_candidates (ranking_id, decision_run_id, vehicle_id, hourly_rate, distance_km, horsepower, rating_score, separation_s_plus, separation_s_minus, relative_closeness_c, final_rank) VALUES
(1, 1, 2, 115.00, 4.20, 290, 4.75, 0.0182, 0.0845, 0.82278, 1),
(2, 1, 1, 145.00, 5.85, 380, 4.92, 0.0345, 0.0720, 0.67605, 2),
(3, 1, 8, 135.00, 7.80, 340, 4.85, 0.0480, 0.0590, 0.55140, 3),
(4, 1, 3, 95.00,  9.10, 210, 4.60, 0.0790, 0.0410, 0.34166, 4);

-- Task 4: Harvest Delay Predictions (1 to 4)
INSERT INTO harvest_delay_predictions (prediction_id, booking_id, field_acres, rain_probability, vehicle_breakdown_history, predicted_risk_tier, confidence_score) VALUES
(1, 1, 32.50, 0.15, 0, 'LOW_RISK',       0.942),
(2, 2, 18.00, 0.20, 1, 'LOW_RISK',       0.890),
(3, 3, 45.00, 0.75, 0, 'MODERATE_RISK',  0.865),
(4, 4, 12.00, 0.85, 3, 'CRITICAL_DELAY', 0.961);

-- Task 5: Daily Operator Tours (1, 2)
INSERT INTO daily_operator_tours (tour_id, vehicle_id, operator_id, tour_date, max_shift_hours, total_tour_distance_km, total_acreage_harvested, optimal_stop_sequence, tour_status) VALUES
(1, 1, 1, '2026-08-25', 10.00, 28.60, 62.50, '[1, 1, 2, 4, 1]', 'OPTIMIZED'),
(2, 8, 2, '2026-08-26', 11.50, 34.20, 82.00, '[10, 5, 6, 8, 10]', 'OPTIMIZED');

-- Task 5: GA Execution Logs (1, 2)
INSERT INTO ga_execution_logs (ga_run_id, tour_id, num_farm_stops, population_size, generations_count, crossover_rate, mutation_rate, initial_distance_km, final_distance_km, fitness_convergence_curve, execution_time_ms) VALUES
(1, 1, 4, 100, 300, 0.85, 0.05, 48.90, 28.60, '[{"gen": 1, "best_dist": 48.9}, {"gen": 25, "best_dist": 39.4}, {"gen": 50, "best_dist": 34.1}, {"gen": 100, "best_dist": 29.8}, {"gen": 200, "best_dist": 28.6}, {"gen": 300, "best_dist": 28.6}]', 182.40),
(2, 2, 4, 120, 400, 0.85, 0.08, 59.40, 34.20, '[{"gen": 1, "best_dist": 59.4}, {"gen": 30, "best_dist": 46.2}, {"gen": 75, "best_dist": 38.5}, {"gen": 150, "best_dist": 35.1}, {"gen": 300, "best_dist": 34.2}, {"gen": 400, "best_dist": 34.2}]', 245.80);