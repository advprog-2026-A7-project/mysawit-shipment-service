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
- CD runs on GitHub-hosted Actions, builds the JAR there, uploads a small release bundle to EC2 over SSH, then rebuilds only the runtime image on EC2.
- The EC2 container uses `--network host` so outbound Supabase Direct Connection traffic uses the instance's native dual-stack networking instead of Docker bridge IPv6.
- Public app access still stays on the EC2 Elastic IP over IPv4 at port `8084`.
- Required GitHub repository variables:
  - `SHIPMENT_HOST_PORT`
- Optional GitHub repository variables:
  - `STAGING_PORT` defaults to `22`
  - `REMOTE_APP_DIR` defaults to `/home/<staging-user>/apps/mysawit-shipment-service`
  - `JWT_EXPIRATION` defaults to `86400000`
  - `JAVA_OPTS` defaults to `-Xms256m -Xmx768m`
  - `JAVA_TOOL_OPTIONS` defaults to `-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false -Dsun.net.inetaddr.ttl=60`
- Required GitHub repository secrets:
  - `STAGING_HOST`
  - `STAGING_USER`
  - `STAGING_SSH_KEY`
  - `SPRING_DATASOURCE_URL_DIRECT` for the Supabase Direct Connection JDBC URL over IPv6
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`
  - `CORS_ORIGINS`
- Optional GitHub repository secrets:
  - `SPRING_DATASOURCE_URL` as a fallback secret name during migration if you have not renamed it yet
- EC2 host prerequisites:
  - Ubuntu host with global IPv6 enabled on the instance
  - Docker installed and usable by the deploy user or through `sudo`
  - Port `8084` open on the EC2 security group for inbound IPv4 traffic
  - Outbound IPv6 allowed to the Supabase direct database endpoint on TCP `5432`

## Deployment Checks
- Host IPv6 presence is verified before the container is restarted.
- The Supabase hostname is resolved on the EC2 host and must return at least one IPv6 address.
- A TCP connection test to the Supabase hostname on port `5432` runs from EC2 before the new container starts.
- Spring readiness is checked at `http://127.0.0.1:8084/actuator/health/readiness` after deploy, and rollback to the previous image is attempted if startup fails.
