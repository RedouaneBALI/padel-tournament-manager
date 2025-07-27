package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Stage {

  Q1("Qualifications 1", 256),
  Q2("Qualifications 2", 128),
  R64("1/32 de finale", 64),
  R32("1/16 de finale", 32),
  R16("1/8 de finale", 16),
  QUARTERS("Quart de finale", 8),
  SEMIS("Demi-finale", 4),
  FINAL("Finale", 2),
  WINNER("Vainqueur", 1);

  private String label;
  private int    nbTeams;

  /**
   * Returns the RoundInfo corresponding to the given number of teams. If no exact match is found, returns the RoundInfo for the next lower round.
   */
  public static Stage fromNbTeams(int teams) {
    Stage result = null;
    for (Stage stage : Stage.values()) {
      if (teams <= stage.getNbTeams()) {
        result = stage;
      }
    }
    return result;
  }

  public Stage next() {
    int     ordinal = this.ordinal();
    Stage[] values  = Stage.values();
    if (ordinal < values.length - 1) {
      return values[ordinal + 1];
    }
    return null; // or throw an exception if needed
  }
}
