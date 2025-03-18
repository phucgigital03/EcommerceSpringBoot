# Build stage
#use an official Maven image to build spring boot app
FROM maven:3.8.4-openjdk-17 AS builder

#set the working directory
WORKDIR /app

#Copy pom.xml file and install dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

#Copy source code and build application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
#use an official OpenJDK image to run application
FROM openjdk:17-jdk-slim

#set the working directory
WORKDIR /app

#Copy the built JAR file from build stage
COPY --from=builder /app/target/eCommerceUdemy-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/eCommerceUdemy-0.0.1-SNAPSHOT.jar"]