# Use JDK 21
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Build the project
RUN mvn clean package -DskipTests

# Run the application
CMD ["java", "-jar", "target/realtime-chat-app.jar"]
