# -------- BUILD STAGE --------
FROM gradle:8-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle build -x test

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
