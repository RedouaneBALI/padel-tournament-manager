package io.github.redouanebali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PadelTournamentManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PadelTournamentManagerApplication.class, args);
  }
}

// H2 Local: mvn spring-boot:run -Dspring-boot.run.profiles=h2
// Cloud SQL local : export $(grep -v '^#' .env.local | xargs) mvn spring-boot:run -Dspring-boot.run.profiles=prod-sql

// TAG=rev-$(date +%Y%m%d-%H%M%S)
// gcloud builds submit --tag us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG .
// gcloud run deploy padel-backend \
//  --region us-central1 \
//  --image us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG \
//  --set-env-vars=SPRING_PROFILES_ACTIVE=prod-sql,SUPER_ADMINS=bali.redouane@gmail.com \
//  --update-secrets=DB_NAME=DB_NAME:latest,DB_USER=DB_USER:latest,DB_PASSWORD=DB_PASSWORD:latest
// SPRING_PROFILES_ACTIVE=prod-sql \
//SPRING_DATASOURCE_URL=jdbc:postgresql://35.202.206.125:5432/padel_db \
//SPRING_DATASOURCE_USERNAME=postgres \
//SPRING_DATASOURCE_PASSWORD='YRfab1@U' \
//mvn spring-boot:run \
//  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"