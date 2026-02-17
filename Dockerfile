# ---- Build stage ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle --no-daemon clean build -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
