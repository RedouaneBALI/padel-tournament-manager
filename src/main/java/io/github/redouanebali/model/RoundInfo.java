package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RoundInfo {

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
  public static RoundInfo fromNbTeams(int teams) {
    RoundInfo result = WINNER; // Default to WINNER if none matches
    for (RoundInfo round : RoundInfo.values()) {
      if (teams >= round.nbTeams) {
        result = round;
        break;
      }
    }
    return result;
  }

  public RoundInfo next() {
    int         ordinal = this.ordinal();
    RoundInfo[] values  = RoundInfo.values();
    if (ordinal < values.length - 1) {
      return values[ordinal + 1];
    }
    return null; // or throw an exception if needed
  }
}
