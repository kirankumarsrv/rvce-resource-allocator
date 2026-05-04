# -------- BUILD STAGE --------
FROM gradle:8-jdk17 AS builder

WORKDIR /app
COPY . .

# Update certificates to avoid TLS handshake issues
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

RUN gradle clean build -x test --no-daemon

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
