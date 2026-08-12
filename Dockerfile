# Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Custom mvnw downloads Maven with curl/unzip
RUN apk add --no-cache curl unzip

COPY . .
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
