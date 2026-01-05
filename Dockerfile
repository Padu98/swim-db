#FROM openjdk:23-jdk-slim
FROM eclipse-temurin:23-jdk

WORKDIR /app

COPY target/dsv_db-0.0.1-SNAPSHOT.jar /app/dsv_db.jar

ENTRYPOINT ["java", "-jar", "dsv_db.jar"]