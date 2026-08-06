# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3-eclipse-temurin-25 AS builder

WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app
# Create the uploads directory the app writes plant images to
RUN mkdir -p uploads
COPY --from=builder /app/target/plants-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
