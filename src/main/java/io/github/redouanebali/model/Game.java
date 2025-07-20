package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long        id;
  @Transient
  private PlayerPair  teamA;
  @Transient
  private PlayerPair  teamB;
  @Transient
  private MatchFormat format;
  @Transient
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
