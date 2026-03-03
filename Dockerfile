# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom first (for caching dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build jar (skip tests)
RUN mvn clean package -Dmaven.test.skip=true

# ---------- Run stage ----------
FROM eclipse-temurin:21-jdk-jammy AS runner

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/match-history-manager-0.0.1-SNAPSHOT.jar ./app.jar

# Expose port
EXPOSE 8000

# Run app
ENTRYPOINT ["java","-jar","/app/app.jar"]
