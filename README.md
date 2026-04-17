# mysawit-shipment-service

Spring Boot (Java + Gradle) microservice for MySawit.

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
  - `SPRING_PROFILES_ACTIVE`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## Deployment Baseline
- CD uploads the application source to EC2, builds the Docker image on the EC2 host, then runs the container there.
- Required GitHub repository variables:
  - `STAGING_PORT`
  - `REMOTE_APP_DIR`
  - `SHIPMENT_HOST_PORT`
- Required GitHub repository secrets:
  - `STAGING_HOST`
  - `STAGING_USER`
  - `STAGING_SSH_KEY`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`
  - `CORS_ORIGINS`
- EC2 host prerequisites:
  - Docker installed and usable by the deploy user or through `sudo`
