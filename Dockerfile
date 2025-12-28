# =========================
# 1) Build stage
# =========================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first to cache dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy sources
COPY src ./src

# Build the jar (skip tests for faster image builds)
RUN mvn -B clean package -DskipTests

# =========================
# 2) Runtime stage
# =========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy fat jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose HTTP port
EXPOSE 8080

# No explicit profile: config comes from application.yaml + env vars
ENTRYPOINT ["java", "-jar", "app.jar"]
