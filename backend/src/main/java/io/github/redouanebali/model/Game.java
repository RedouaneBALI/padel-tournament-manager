package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "teama_id")
  private PlayerPair teamA;

  @ManyToOne
  @JoinColumn(name = "teamb_id")
  private PlayerPair teamB;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Score score;

  @ManyToOne
  @JoinColumn(name = "format_id")
  private MatchFormat format;

  @Enumerated(EnumType.STRING)
  @Column(name = "winner_side")
  private TeamSide winnerSide;

  @DateTimeFormat(pattern = "HH:mm")
  @JsonFormat(pattern = "HH:mm")
  @Column(name = "scheduled_time")
  private LocalTime scheduledTime;

  private String court;

  @ManyToOne
  @JoinColumn(name = "pool_id")
  private Pool pool;

  public Game(MatchFormat format) {
    this.format = format;
  }

  public boolean isFinished() {
    // BYE handling using PlayerPair.isBye()
    boolean aBye = teamA != null && teamA.isBye();
    boolean bBye = teamB != null && teamB.isBye();

    // Both BYE → consider finished (no meaningful winner)
    if (aBye && bBye) {
      return true;
    }
    // Exactly one BYE with a real opponent present → finished (auto-qualify the non-BYE side)
    if ((aBye && teamB != null && !bBye) || (bBye && teamA != null && !aBye)) {
      return true;
    }

    // Fall back to score-based completion
    if (score == null || score.getSets().isEmpty()) {
      return false;
    }

    int     teamAWonSets = 0;
    int     teamBWonSets = 0;
    int     setsToWin    = format.getNumberOfSetsToWin();
    int     pointsPerSet = format.getGamesPerSet();
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
    // Resolve BYE first using PlayerPair.isBye()
    boolean aBye = teamA != null && teamA.isBye();
    boolean bBye = teamB != null && teamB.isBye();
    if (aBye && !bBye) {
      return teamB;
    }
    if (bBye && !aBye) {
      return teamA;
    }
    if (aBye && bBye) {
      return null;
    }

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
      PlayerPair winner = getWinner();
      if (winner == null) {
        this.winnerSide = null;
      } else {
        this.winnerSide = winner.equals(teamA) ? TeamSide.TEAM_A : TeamSide.TEAM_B;
      }
    } else {
      this.winnerSide = null;
    }
  }

}
