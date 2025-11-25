package io.github.redouanebali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PadelTournamentManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PadelTournamentManagerApplication.class, args);
  }
}

// H2 Local: mvn spring-boot:run -Dspring-boot.run.profiles=h2
// Cloud SQL local : export $(grep -v '^#' .env.local | xargs) mvn spring-boot:run -Dspring-boot.run.profiles=prod-sql

// TAG=rev-$(date +%Y%m%d-%H%M%S)
// gcloud builds submit --tag us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG .
// Déploiement avec H2 (pour tests)
// gcloud run deploy padel-backend --region us-central1 --image us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG --set-env-vars SPRING_PROFILES_ACTIVE=h2,SUPER_ADMINS=bali.redouane@gmail.com
// Déploiement avec PostgreSQL (production)
// gcloud run deploy padel-backend --region us-central1 --image us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG --set-env-vars SPRING_PROFILES_ACTIVE=prod-sql,SUPER_ADMINS=bali.redouane@gmail.com --update-secrets DB_HOST=DB_HOST:latest,DB_PORT=DB_PORT:latest,DB_NAME=DB_NAME:latest,DB_USER=DB_USER:latest,DB_PASSWORD=DB_PASSWORD:latest



