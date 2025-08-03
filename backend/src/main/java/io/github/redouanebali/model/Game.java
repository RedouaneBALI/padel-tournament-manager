package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

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

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime scheduledTime;

  private String court;

  public Game(MatchFormat format) {
    this.format = format;
  }

  public boolean isFinished() {
    if (score == null || score.getSets().isEmpty()) {
      return false;
    }

    int     teamAWonSets = 0;
    int     teamBWonSets = 0;
    int     setsToWin    = format.getNumberOfSetsToWin();
    int     pointsPerSet = format.getPointsPerSet();
    boolean superTie     = format.isSuperTieBreakInFinalSet();

    for (int i = 0; i < score.getSets().size(); i++) {
      SetScore set        = score.getSets().get(i);
      boolean  isFinalSet = (i == score.getSets().size() - 1) && (teamAWonSets == setsToWin - 1) && (teamBWonSets == setsToWin - 1);

      if (isFinalSet && superTie) {
        int a = set.getTeamAScore();
        int b = set.getTeamBScore();
        if ((a >= 10 || b >= 10) && Math.abs(a - b) >= 2) {
          if (a > b) {
            teamAWonSets++;
          } else {
            teamBWonSets++;
          }
        }
      } else {
        if (isSetWonBy(set.getTeamAScore(), set.getTeamBScore(), pointsPerSet)) {
          teamAWonSets++;
        } else if (isSetWonBy(set.getTeamBScore(), set.getTeamAScore(), pointsPerSet)) {
          teamBWonSets++;
        }
      }
    }

    return teamAWonSets >= setsToWin || teamBWonSets >= setsToWin;
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
