# ---- Stage 1: build ----
FROM eclipse-temurin:8-jdk AS build
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# ---- Stage 2: run ----
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]