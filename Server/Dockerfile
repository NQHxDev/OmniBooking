# Stage 1: Cache dependencies
FROM maven:3.9.16-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Stage 2: Build source code
FROM maven:3.9.16-eclipse-temurin-21 AS builder
WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 3: Run JRE
FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
