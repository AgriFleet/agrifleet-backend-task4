# AgriFleet Task 4: Intelligent Decision Service

The AgriFleet Task 4 service provides decision-support functions for agricultural machinery dispatch. It ranks available vehicles for a booking using the TOPSIS multi-criteria decision-making method and predicts harvest-delay risk using stored booking and operational data.

## Responsibilities

- TOPSIS-based vehicle ranking for a booking
- Configurable cost, distance, horsepower, and rating weights
- Persistence of ranking runs and ranked candidates
- Harvest-delay risk prediction using a decision-tree engine
- Persistence and retrieval of delay-prediction history
- JSON REST API for frontend and service integration

## Technology

- Java 17
- Spring Boot `3.3.2`
- Spring Web
- Spring JDBC
- SQLite
- Jackson Databind for JSON columns
- Jakarta Bean Validation
- Maven

The service uses plain JDBC instead of JPA/Hibernate. This keeps SQLite access explicit and avoids ORM dialect compatibility issues.

## Prerequisites

- Java 17 or newer
- Maven 3.9 or newer
- The shared AgriFleet SQLite database with the required booking, vehicle, and decision-support data

## Running Locally

From this directory, start the service with Maven.

Windows PowerShell:

```powershell
mvn spring-boot:run
```

Linux or macOS:

```bash
mvn spring-boot:run
```

The service starts on [http://localhost:8084](http://localhost:8084). Its context path is `/api/v1`, so the full API base URL is:

```text
http://localhost:8084/api/v1
```

To build and run the packaged application:

```powershell
mvn clean package
java -jar target/decision-service-1.0.0.jar
```

## Configuration

The default settings are in `src/main/resources/application.properties`:

```properties
server.port=8084
server.servlet.context-path=/api/v1
agrifleet.db.path=agrifleet.db
spring.datasource.url=jdbc:sqlite:${agrifleet.db.path}
```

By default, the service opens `agrifleet.db` from its working directory. Override the database path or port when needed:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8084 --agrifleet.db.path=C:/data/AgriFleet.db"
```

The database must contain the source operational data and the tables used for decision runs, ranked candidates, risk inputs, and prediction history.

## API Endpoints

The controller allows cross-origin requests from the frontend. All endpoints use the `/api/v1/decision` prefix.

### TOPSIS Vehicle Ranking

```text
POST /api/v1/decision/decision-se/topsis/rank?bookingId={bookingId}
```

Ranks eligible vehicles for the supplied booking. The four weights are required and each must be between `0.0` and `1.0`.

Example request:

```bash
curl -X POST "http://localhost:8084/api/v1/decision/decision-se/topsis/rank?bookingId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "costWeight": 0.25,
    "distanceWeight": 0.25,
    "horsepowerWeight": 0.25,
    "ratingWeight": 0.25
  }'
```

The request also accepts `cost`, `distance`, `hp`, and `rating` as JSON aliases for the corresponding weight fields.

A successful response contains the decision run ID, booking ID, normalized weights, ranked candidates, and execution time:

```json
{
  "decisionRunId": 12,
  "bookingId": 1,
  "normalizedWeights": {
    "cost": 0.25,
    "distance": 0.25,
    "horsepower": 0.25,
    "rating": 0.25
  },
  "rankedCandidates": [],
  "executionTimeMs": 8.42
}
```

### Decision Run History

```text
GET /api/v1/decision/decision/topsis/runs
```

Returns stored TOPSIS decision runs.

### Ranked Candidates

```text
GET /api/v1/decision/decision/topsis/candidates?runId={runId}
```

Returns the ranked candidates for a stored decision run.

### Harvest-Delay Prediction

```text
POST /api/v1/decision/decision/delays/predict?bookingId={bookingId}
```

Generates a delay-risk prediction from the latest stored risk-input data for the booking.

Example response:

```json
{
  "bookingId": 1,
  "predictedRiskTier": "MEDIUM",
  "confidenceScore": 0.82,
  "fieldAcres": 12.5,
  "rainProbability": 0.35,
  "vehicleBreakdownHistory": 1
}
```

### Delay-Prediction History

```text
GET /api/v1/decision/decision/delays/history
```

Returns stored harvest-delay predictions.

## Project Structure

```text
src/main/java/com/agrifleet/decision/
  controller/   REST API endpoints
  dto/          Request and response objects
  engine/       TOPSIS and decision-tree calculation engines
  entity/       Decision-support entities
  repository/   JDBC persistence operations
  service/      Application services
src/main/resources/
  application.properties
```

## Testing

Run the test suite with:

```powershell
mvn test
```

The tests cover the TOPSIS ranking engine and the decision-tree risk engine.

## Related Services

The AgriFleet frontend expects this service at `http://localhost:8084/api/v1` by default. The core service supplies shared bookings and vehicles, while the other task services provide routing, allocation, network analysis, and tour optimization.
