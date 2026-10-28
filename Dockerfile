# Multi-stage root Dockerfile for the employee-management-service backend artifact.
# Prefer building the JAR with Maven first, then packaging.

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /build/target/employee-management-service-*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8081
ENV APP_PORT=8081 LOG_LEVEL=INFO
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8081/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
