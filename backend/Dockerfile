########## Build stage ##########
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copie tous les POM (parent + modules)
COPY pom.xml .
COPY app/pom.xml ./app/
COPY core/pom.xml ./core/

# Télécharge les dépendances
RUN mvn -q -DskipTests dependency:go-offline

# Copie les sources
COPY core/src ./core/src
COPY app/src ./app/src

# Build : compile core d'abord, puis package app
RUN mvn -q -DskipTests clean install

# Copie le jar final en un nom stable
RUN cp app/target/*.jar /app/app.jar

########## Run stage ##########
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
