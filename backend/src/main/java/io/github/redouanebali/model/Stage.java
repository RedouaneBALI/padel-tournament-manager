package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Stage {

  GROUPS("Groupes", 0, false, 0),
  Q1("Qualifications 1", 256, true, 1),
  Q2("Qualifications 2", 128, true, 2),
  Q3("Qualifications 3", 128, true, 3),
  R64("1/32 de finale", 64, false, 4),
  R32("1/16 de finale", 32, false, 5),
  R16("1/8 de finale", 16, false, 6),
  QUARTERS("Quart de finale", 8, false, 7),
  SEMIS("Demi-finale", 4, false, 8),
  FINAL("Finale", 2, false, 9),
  WINNER("Vainqueur", 1, false, 10);

  private final String  label;
  private final int     nbTeams;
  private final boolean isQualification;
  private final int     order;

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

  public int getOrder() {
    return order;
  }
}
