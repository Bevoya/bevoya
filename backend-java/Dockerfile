# Build stage: compile and package with the Maven wrapper (tests run in CI, not here).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home bevoya
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER bevoya
# Small Render instances have little memory: cap the heap and use the light collector.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"
# Render sets PORT; application.properties reads it (default 8080).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
