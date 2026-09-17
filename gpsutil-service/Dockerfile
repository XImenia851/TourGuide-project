# ---- Stage 1: build ----
FROM eclipse-temurin:8-jdk AS build
WORKDIR /app

# Copy only build config first - this layer is cached and reused as long as
# dependencies don't change, so code edits don't force a full re-download.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY libs ./libs
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# Now copy the actual source code and build for real.
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# ---- Stage 2: run ----
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]