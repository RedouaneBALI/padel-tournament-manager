package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Stage {

  GROUPS("Groups", 0, false, 0),
  Q1("Qualifications 1", 0, true, 1),
  Q2("Qualifications 2", 0, true, 2),
  Q3("Qualifications 3", 0, true, 3),
  R64("Round of 64", 64, false, 4),
  R32("Round of 32", 32, false, 5),
  R16("Round of 16", 16, false, 6),
  QUARTERS("Quarter-finals", 8, false, 7),
  SEMIS("Semi-finals", 4, false, 8),
  FINAL("Final", 2, false, 9);

  private final String  label;
  private final int     nbTeams;
  private final boolean isQualification;
  private final int     order;

  /**
   * Map a 1-based qualification index to the corresponding qualification stage. Supported values: 1 -> Q1, 2 -> Q2, 3 -> Q3.
   *
   * @param index 1-based qualification round index
   * @return the corresponding qualification Stage
   * @throws IllegalArgumentException if the index is not in [1..3]
   */
  public static Stage fromQualifIndex(int index) {
    return switch (index) {
      case 1 -> Q1;
      case 2 -> Q2;
      case 3 -> Q3;
      default -> throw new IllegalArgumentException("App supports up to Q3 as per specs: index=" + index);
    };
  }

  /**
   * Returns the main draw stage corresponding to the number of teams, or the stage just above if the number doesn't exactly match a boundary.
   * Qualification stages (Q1..Q3) and GROUPS are not handled here.
   *
   * @param teams number of teams
   * @return the corresponding stage or the stage just above
   * @throws IllegalArgumentException if no stage is found
   */
  public static Stage fromNbTeams(int teams) {
    // Traverse stages from largest to smallest
    for (Stage stage : new Stage[]{FINAL, SEMIS, QUARTERS, R16, R32, R64}) {
      if (teams <= stage.nbTeams && stage.nbTeams > 0) {
        return stage;
      }
    }
    throw new IllegalArgumentException("Unsupported number of teams for main draw: " + teams);
  }

  /**
   * Indicates if this stage corresponds to the first round of the tournament's main draw.
   *
   * @param mainDrawSize the size of the main draw (e.g. 32, 64)
   * @return true if this stage is the first round of the main draw
   */
  public boolean isMainDraw(int mainDrawSize) {
    try {
      Stage mainDrawStage = fromNbTeams(mainDrawSize);
      return this == mainDrawStage;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
