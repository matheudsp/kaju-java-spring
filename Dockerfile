FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (this layer can be cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Use a smaller JRE image for the runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Default environment variables (will be overridden by docker-compose)
ENV SECRET_KEY=default_secret_key_for_development_only
ENV WHATSAPP_API_TOKEN=your_whatsapp_api_token
ENV WHAPI_PHONE_ID=your_whapi_phone_id
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/kaju_db
ENV SPRING_DATASOURCE_USERNAME=myuser
ENV SPRING_DATASOURCE_PASSWORD=secret


# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]