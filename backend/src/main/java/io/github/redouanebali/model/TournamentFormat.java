package io.github.redouanebali.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TournamentFormat {
  KNOCKOUT("Tableau à élimination direct"),
  GROUP_STAGE("Phase de poules");

  private String label;
}
