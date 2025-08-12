package io.github.redouanebali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PadelTournamentManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PadelTournamentManagerApplication.class, args);
  }
}

// TAG=rev-$(date +%Y%m%d-%H%M%S)
// gcloud builds submit --tag us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG .
// gcloud run deploy padel-backend --region us-central1 --image us-central1-docker.pkg.dev/deft-computing-468317-v5/spring-images/padel-backend:$TAG --set-env-vars=SUPER_ADMINS=bali.redouane@gmail.com
