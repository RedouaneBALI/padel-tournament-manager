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
   * Retourne le stage du tableau principal correspondant au nombre d'équipes, ou le stage juste au-dessus si le nombre ne correspond pas exactement à
   * une borne. Qualification stages (Q1..Q3) et GROUPS ne sont pas gérés ici.
   *
   * @param teams nombre d'équipes
   * @return le stage correspondant ou le stage juste au-dessus
   * @throws IllegalArgumentException si aucun stage n'est trouvé
   */
  public static Stage fromNbTeams(int teams) {
    // Parcourir les stages du plus grand au plus petit
    for (Stage stage : new Stage[]{FINAL, SEMIS, QUARTERS, R16, R32, R64}) {
      if (teams <= stage.nbTeams && stage.nbTeams > 0) {
        return stage;
      }
    }
    throw new IllegalArgumentException("Unsupported number of teams for main draw: " + teams);
  }

  /**
   * Indique si ce stage correspond au premier tour du tableau principal du tournoi.
   *
   * @param mainDrawSize la taille du tableau principal (ex: 32, 64)
   * @return true si ce stage est le premier tour du tableau principal
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
