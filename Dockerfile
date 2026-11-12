# step 1: build
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /jumpbeyondfriends

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

# step 2: runtime
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build /jumpbeyondfriends/target/jumpbeyondfriends-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
