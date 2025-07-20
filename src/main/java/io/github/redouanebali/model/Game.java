package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

  private PlayerPair  teamA;
  private PlayerPair  teamB;
  private MatchFormat format;
  private Score       score;

  public Game(PlayerPair teamA, PlayerPair teamB) {
    this.teamA = teamA;
    this.teamB = teamB;
  }

  @Override
  public String toString() {
    if (teamA != null && teamB != null) {
      return teamA.getPlayer1().getName() + "/" + teamA.getPlayer2().getName() + " VS "
             + teamB.getPlayer1().getName() + "/" + teamB.getPlayer2().getName();
    } else if (teamA != null) {
      return teamA.getPlayer1().getName() + "/" + teamA.getPlayer2().getName() + " VS "
             + "?/?";
    } else if (teamB != null) {
      return "?/? VS " + teamB.getPlayer1().getName() + "/" + teamB.getPlayer2().getName();
    }
    return "";
  }

}
