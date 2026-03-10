# Use lightweight Java 17 runtime (matches pom.xml java.version)
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy the packaged JAR produced by: mvn clean package -DskipTests
COPY target/Wild-Beyond-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
