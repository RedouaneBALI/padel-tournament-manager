package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  private PlayerPair teamA;

  @ManyToOne
  private PlayerPair teamB;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Score score;

  @ManyToOne
  private MatchFormat format;

  private TeamSide winnerSide;

  public Game(MatchFormat format) {
    this.format = format;
  }

  public boolean isFinished() {
    int teamAWonSets = 0;
    int teamBWonSets = 0;

    if (score == null) {
      return false;
    }

    for (SetScore set : score.getSets()) {
      if (isSetWonBy(set.getTeamAScore(), set.getTeamBScore(), format.getPointsPerSet())) {
        teamAWonSets++;
      } else if (isSetWonBy(set.getTeamBScore(), set.getTeamAScore(), format.getPointsPerSet())) {
        teamBWonSets++;
      }
    }

    if (teamAWonSets >= format.getNumberOfSetsToWin() || teamBWonSets >= format.getNumberOfSetsToWin()) {
      return true;
    }

    if (format.isSuperTieBreakInFinalSet()
        && teamAWonSets == format.getNumberOfSetsToWin() - 1
        && teamBWonSets == format.getNumberOfSetsToWin() - 1) {
      int superTieA = score.getSets().getLast().getTeamAScore();
      int superTieB = score.getSets().getLast().getTeamBScore();

      return (superTieA >= 10 || superTieB >= 10) && Math.abs(superTieA - superTieB) >= 2;
    }

    return false;
  }

  public boolean isSetWonBy(int teamScore, int opponentScore, int maxPointsPerSet) {
    if (teamScore < maxPointsPerSet) {
      return false;
    }

    // If both teams reached maxPointsPerSet - 1, play up to maxPointsPerSet + 1
    int tieThreshold = maxPointsPerSet - 1;
    if (opponentScore >= tieThreshold) {
      return teamScore == maxPointsPerSet + 1 && (teamScore - opponentScore) >= 1;
    }

    // Otherwise, 2 points difference needed after reaching maxPointsPerSet
    return (teamScore - opponentScore) >= 2;
  }


  public PlayerPair getWinner() {
    if (!isFinished()) {
      return null;
    }

    int setsWonByA = 0;
    int setsWonByB = 0;

    for (SetScore set : score.getSets()) {
      if (set.getTeamAScore() > set.getTeamBScore()) {
        setsWonByA++;
      } else if (set.getTeamBScore() > set.getTeamAScore()) {
        setsWonByB++;
      }
    }

    return setsWonByA > setsWonByB ? teamA : teamB;
  }

  public void setScore(Score score) {
    this.score = score;
    if (this.isFinished()) {
      this.winnerSide = getWinner().equals(teamA) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
    } else {
      this.winnerSide = null;
    }
  }

}
