# Use Java 21 JDK
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# Run the app
CMD ["java", "-jar", "target/realtime-chat-app.jar"]
