########## Build stage ##########
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
# ⬇️ important: repackage pour produire un JAR exécutable
RUN mvn -q -DskipTests package spring-boot:repackage

# Copie le jar final en un nom stable
RUN JAR="$(ls target/*.jar | head -n1)" && cp "$JAR" /app/app.jar

########## Run stage ##########
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
