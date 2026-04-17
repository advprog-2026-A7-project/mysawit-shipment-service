FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN chown spring:spring /app/app.jar

EXPOSE 8084
ENV SERVER_PORT=8084
ENV JAVA_OPTS=""
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false -Dsun.net.inetaddr.ttl=60"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness" || exit 1

USER spring:spring
ENTRYPOINT ["sh","-c","exec java $JAVA_TOOL_OPTIONS $JAVA_OPTS -jar /app/app.jar"]
