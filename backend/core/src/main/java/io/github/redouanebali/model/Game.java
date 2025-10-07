package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "teama_id")
  private PlayerPair teamA;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "teamb_id")
  private PlayerPair teamB;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Score score;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "format_id")
  private MatchFormat format;

  @Enumerated(EnumType.STRING)
  private TeamSide winnerSide;

  @DateTimeFormat(pattern = "HH:mm")
  @JsonFormat(pattern = "HH:mm")
  private LocalTime scheduledTime;

  private String court;

  @ManyToOne
  @JoinColumn(name = "pool_id")
  private Pool pool;

  public Game(MatchFormat format) {
    if (format == null) {
      throw new IllegalArgumentException("Le format du match ne peut pas être null");
    }
    this.format = format;
  }

  public boolean isFinished() {
    if (isByeFinished()) {
      return true;
    }
    if (!hasValidScore()) {
      return false;
    }
    int[] setsWon   = calculateSetsWon();
    int   setsToWin = format.getNumberOfSetsToWin();
    return setsWon[0] >= setsToWin || setsWon[1] >= setsToWin;
  }

  private boolean isByeFinished() {
    boolean aBye = teamA != null && teamA.isBye();
    boolean bBye = teamB != null && teamB.isBye();
    if (aBye && bBye) {
      return true;
    }
    if (aBye && teamB != null) {
      return true;
    }
    return bBye && teamA != null;
  }

  private boolean hasValidScore() {
    return score != null && score.getSets() != null && !score.getSets().isEmpty();
  }

  private int[] calculateSetsWon() {
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
    return new int[]{teamAWonSets, teamBWonSets};
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
    PlayerPair byeWinner = resolveByeWinner();
    if (byeWinner != null) {
      return byeWinner;
    }

    if (!isFinished()) {
      return null;
    }

    return determineWinnerByScore();
  }

  private PlayerPair resolveByeWinner() {
    boolean aBye = teamA != null && teamA.isBye();
    boolean bBye = teamB != null && teamB.isBye();

    if (aBye && bBye) {
      return teamA;
    }
    if (aBye) {
      return teamB;
    }
    if (bBye) {
      return teamA;
    }
    return null;
  }

  private PlayerPair determineWinnerByScore() {
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

  @Override
  public String toString() {
    return String.format(
        "%s vs %s",
        teamA != null ? teamA.toString() : "?",
        teamB != null ? teamB.toString() : "?"
    );
  }

}
