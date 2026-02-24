# ===== Build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew || true
RUN ./gradlew clean bootJar -x test

# ===== Run stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]