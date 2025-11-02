package io.github.redouanebali.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TournamentLevel {

  P25("P25"),
  P50("P50"),
  P100("P100"),
  P250("P250"),
  P500("P500"),
  P1000("P1000"),
  P1500("P1500"),
  P2000("P2000"),
  AMATEUR("Amateur");

  private String label;

}
