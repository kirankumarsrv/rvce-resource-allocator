# -------- BUILD STAGE --------
FROM gradle:8-jdk17 AS builder

WORKDIR /app
COPY . .

# Update certificates and retry apt-get to handle network issues
RUN set -ex; \
    for i in {1..3}; do \
        apt-get update && apt-get install -y ca-certificates && break || sleep 5; \
    done; \
    rm -rf /var/lib/apt/lists/*

RUN gradle clean build -x test --no-daemon

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
