package io.github.redouanebali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PadelTournamentManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PadelTournamentManagerApplication.class, args);
  }
}

// gcloud run services logs read padel-backend --region us-central1 --limit 200