# Gebruik een kant-en-klare Java 21 omgeving
FROM eclipse-temurin:21-jre-jammy

# Werkmap in de container
WORKDIR /app

# Kopieer de gebouwde jar (die we zo gaan maken) naar de container
COPY target/*.jar app.jar

# Start de Spring Boot applicatie op
ENTRYPOINT ["java", "-jar", "app.jar"]