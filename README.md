# mysawit-shipment-service

Spring Boot (Kotlin + Gradle) microservice for MySawit.

## Run (local)
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew bootRun
```

Runs at: http://localhost:8084

## Health
- GET /actuator/health

Quick check:
```bash
curl http://localhost:8084/actuator/health
```

## OpenAPI Skeleton
- File: `src/main/resources/openapi/shipment-api.yaml`
- Contains milestone-level shipment endpoint list and minimal request/response schemas.

## Environment Config
- Example env file: `.env.example`
- Main variables:
  - `SERVER_PORT`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
