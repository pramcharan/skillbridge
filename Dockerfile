# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the application, skipping tests to speed up the build (tests should be run in CI)
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built jar file from the build stage
COPY --from=build /app/target/skillbridge-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the jar file with memory constraints for Render free tier (512MB RAM)
# We use -Xmx384m to leave room for the OS and JRE overhead.
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar", "--spring.profiles.active=prod", "--server.port=${PORT:8080}"]
