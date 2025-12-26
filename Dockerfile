# Use a Maven image with JDK installed
FROM maven:3.9.3-eclipse-temurin-21

# Set the working directory
WORKDIR /app

# Copy your project files
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# Run the app
CMD ["java", "-jar", "target/your-app.jar"]
