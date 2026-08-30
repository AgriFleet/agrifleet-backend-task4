# AgriFleet - Task 4: Intelligent Decision Module

TOPSIS-based machinery ranking + decision-tree harvest delay risk prediction,
built as an independent Spring Boot module against the shared AgriFleet
SQLite database.

## Why these choices

- **Plain JDBC (`JdbcTemplate`), not JPA/Hibernate.** SQLite's Hibernate
  dialect support is unreliable and not worth the risk this close to the
  deadline. `JdbcTemplate` is simple, predictable, and the algorithm's
  correctness doesn't depend on the persistence layer anyway.
- **One shared `agrifleet.db` file**, not five separate service databases.
  The spec's per-module DB names (`decision_service_db` etc.) are a
  microservice-style naming convention worth mentioning in your System
  Architecture chapter, but for a single SQLite file shared by a 10-person
  student team, one physical file is far less error-prone. Tables are
  already logically separated by name.
- **Decision tree for risk prediction**, not just TOPSIS everywhere. The
  spec's own Task 4 comparison table lists TOPSIS vs k-NN/Decision Tree - we
  actually build the decision tree branch so you have two genuinely
  implemented algorithms to discuss, compare, and benchmark, not one
  algorithm plus literature review.

## Project layout

```
src/main/java/com/agrifleet/decision/
  engine/      TopsisEngine, DecisionTreeRiskEngine   <- pure algorithms, no Spring
  entity/      Vehicle, Booking                        <- plain POJOs
  repository/  JdbcTemplate-based DAOs
  dto/         request/response records
  service/     DecisionService, RiskPredictionService, VehicleCompatibilityService, GeoUtils
  controller/  DecisionController, GlobalExceptionHandler
db/init.sql    standalone dev DB (subset of the team's full schema)
```

## Running it

1. **Get a database file.**
   - If the team's shared `agrifleet.db` (from `AgroFleet_script.sql`) is
     ready, copy it to the project's parent directory (see
     `agrifleet.db.path` in `application.properties`), or edit that path.
   - Otherwise, for now:
     ```bash
     sqlite3 ../agrifleet.db < db/init.sql
     ```
     This creat es a private dev copy with just the tables/rows Task 4 needs,
     using the same sample data as the shared script.

2. **Run the service:**
   ```bash
   mvn spring-boot:run
   ```
   Starts on `http://localhost:8084`.

3. **Try it:**
   ```bash
   curl -X POST http://localhost:8084/api/decision/rank/1 \
     -H "Content-Type: application/json" \
     -d '{"costWeight":0.35,"distanceWeight":0.25,"horsepowerWeight":0.20,"ratingWeight":0.20}'

   curl -X POST "http://localhost:8084/api/decision/risk/1?rainProbability=0.75&breakdownHistory=1"
   ```

## ⚠️ A note on the SQL script's sample TOPSIS data

The `ranked_fleet_candidates` seed rows in `AgroFleet_script.sql` (decision_run_id=1)
imply an order of vehicle 2 > 1 > 8 > 3. Hand-verifying the actual TOPSIS formula
against those same 4 vehicles and weights gives **2 > 1 > 3 > 8** instead - vehicle 3
(cheapest, at $95/hr) legitimately outranks vehicle 8 once cost carries 35% of the
weight, despite vehicle 8's better horsepower and rating. The seed script's numbers
appear to be illustrative placeholders rather than a verified computation. `TopsisEngineTest`
asserts against the hand-verified numbers, not the seed data - if you cite the
seed data anywhere in your report, flag this discrepancy rather than presenting
it as validated output.

## Tests / benchmarks

```bash
mvn test
```
`TopsisEngineTest.benchmarkAcrossInputSizes` and the decision-tree tests
print timing you can paste directly into Chapter 8 (Experimental
Performance Evaluation). For memory profiling, run with
`-Xlog:gc` or profile via VisualVM/JFR and capture peak heap during the
benchmark loop.

## API summary

| Method | Path | Body / Params | Returns |
|---|---|---|---|
| POST | `/api/decision/rank/{bookingId}` | `{costWeight, distanceWeight, horsepowerWeight, ratingWeight}` (0-1 each) | Ranked candidate list + TOPSIS internals (S+, S-, C) |
| POST | `/api/decision/risk/{bookingId}?rainProbability=&breakdownHistory=` | query params | Predicted risk tier + confidence |

## Next steps for the pair of you

1. Point `agrifleet.db.path` at the real shared file once it exists.
2. Decide who owns `VehicleCompatibilityService`'s crop→vehicle-type map -
   it's a guess right now; check it against what Task 2 (allocation) assumes
   so the two modules agree on compatibility rules.
3. Wire the React dashboard (see `agrifleet-decision-dashboard.jsx`) into
   your team's actual frontend routing/auth.
4. Once Task 1 (routing) is live, consider swapping the haversine
   straight-line distance here for their actual road-network distance - note
   this as a "future enhancement" in Chapter 9 either way, it's a legitimate
   discussion point regardless of which you ship.
