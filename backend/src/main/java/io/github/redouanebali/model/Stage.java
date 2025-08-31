package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Stage {

  GROUPS("Groupes", 0, false, 0),
  Q1("Qualifications 1", 0, true, 1),
  Q2("Qualifications 2", 0, true, 2),
  Q3("Qualifications 3", 0, true, 3),
  R64("1/32 de FINAL", 64, false, 4),
  R32("1/16 de FINAL", 32, false, 5),
  R16("1/8 de FINAL", 16, false, 6),
  QUARTERS("Quart de FINAL", 8, false, 7),
  SEMIS("Demi-FINAL", 4, false, 8),
  FINAL("FINAL", 2, false, 9);

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
   * Map a power-of-two number of teams to the corresponding main-draw stage. Qualification stages (Q1..Q3) and GROUPS are not handled here.
   *
   * @param teams a power of two (64, 32, 16, 8, 4, 2)
   * @return the corresponding Stage (R64..FINAL)
   * @throws IllegalArgumentException if the value is not supported
   */
  public static Stage fromNbTeams(int teams) {
    return switch (teams) {
      case 64 -> R64;
      case 32 -> R32;
      case 16 -> R16;
      case 8 -> QUARTERS;
      case 4 -> SEMIS;
      case 2 -> FINAL;
      default -> throw new IllegalArgumentException("Unsupported number of teams for main draw: " + teams);
    };
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
