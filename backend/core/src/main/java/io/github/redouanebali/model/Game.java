package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.validation.constraints.NotNull;
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
  @NotNull
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

  @Column(name = "created_by")
  private String createdBy;

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

    if (score != null && score.isForfeit()) {
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
    SetCounter counter           = new SetCounter();
    int        setsToWin         = format.getNumberOfSetsToWin();
    int        pointsPerSet      = format.getGamesPerSet();
    boolean    superTie          = format.isSuperTieBreakInFinalSet();
    int        lastSetIndex      = score.getSets().size() - 1;
    int        expectedTotalSets = 2 * setsToWin - 1;

    for (int i = 0; i < score.getSets().size(); i++) {
      SetScore set        = score.getSets().get(i);
      boolean  isFinalSet = (i == lastSetIndex) && superTie && (score.getSets().size() == expectedTotalSets);

      if (isFinalSet) {
        evaluateFinalSet(set, counter);
      } else {
        evaluateSetWinner(set, pointsPerSet, counter);
      }
    }

    return new int[]{counter.teamAWonSets, counter.teamBWonSets};
  }

  private void evaluateFinalSet(SetScore set, SetCounter counter) {
    Integer tieA = set.getTieBreakTeamA();
    Integer tieB = set.getTieBreakTeamB();

    if (tieA != null && tieB != null && isSuperTieBreakWon(tieA, tieB)) {
      if (tieA > tieB) {
        counter.teamAWonSets++;
      } else {
        counter.teamBWonSets++;
      }
    }
  }

  private boolean isSuperTieBreakWon(int tieA, int tieB) {
    return (tieA >= 10 || tieB >= 10) && Math.abs(tieA - tieB) >= 2;
  }

  private void evaluateSetWinner(SetScore set, int pointsPerSet, SetCounter counter) {
    int winner = evaluateRegularSet(set, pointsPerSet);
    if (winner == 1) {
      counter.teamAWonSets++;
    } else if (winner == 2) {
      counter.teamBWonSets++;
    }
  }

  private int evaluateRegularSet(SetScore set, int pointsPerSet) {
    int a             = set.getTeamAScore();
    int b             = set.getTeamBScore();
    int tieBreakScore = format.getTieBreakAt();

    // If both teams reach tieBreakScore, the tie-break is triggered
    // The winner must reach tieBreakScore + 1 with at least 1 game difference
    if (a >= tieBreakScore && b >= tieBreakScore) {
      if (a >= tieBreakScore + 1 && a > b) {
        return 1; // Team A won
      }
      if (b >= tieBreakScore + 1 && b > a) {
        return 2; // Team B won
      }
      return 0; // Tie-break in progress
    }

    // Classic logic before tie-break
    if (isSetWonBy(set.getTeamAScore(), set.getTeamBScore(), pointsPerSet)) {
      return 1; // Team A won
    }
    if (isSetWonBy(set.getTeamBScore(), set.getTeamAScore(), pointsPerSet)) {
      return 2; // Team B won
    }
    return 0; // No winner yet
  }

  public boolean isSetWonBy(int teamScore, int opponentScore, int maxPointsPerSet) {
    int tieBreakScore = format.getTieBreakAt();

    // If maxPointsPerSet is not reached, the set is not won
    if (teamScore < maxPointsPerSet) {
      return false;
    }

    // If both teams have reached tieBreakScore, we are in tie-break mode
    // In this case, the victory only requires having 1 more game (handled by evaluateRegularSet)
    if (teamScore >= tieBreakScore && opponentScore >= tieBreakScore) {
      return false; // The tie-break will be handled by evaluateRegularSet
    }

    // Otherwise, classic victory: having reached maxPointsPerSet with 2 games difference
    return (teamScore - opponentScore) >= 2;
  }

  public PlayerPair getWinner() {
    PlayerPair byeWinner = resolveByeWinner();
    if (byeWinner != null) {
      return byeWinner;
    }

    // Check for forfeit winner
    if (score != null && score.isForfeit()) {
      return resolveForfeitWinner();
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

  /**
   * Determines the winner when a forfeit occurred. The team that did NOT forfeit wins.
   *
   * @return the winning team (opposite of the team that forfeited)
   */
  private PlayerPair resolveForfeitWinner() {
    if (score.getForfeitedBy() == null) {
      // If forfeitedBy is not specified, cannot determine winner
      return null;
    }

    return score.getForfeitedBy() == TeamSide.TEAM_A ? teamB : teamA;
  }

  private PlayerPair determineWinnerByScore() {
    int[] setsWon = calculateSetsWon();
    if (setsWon[0] > setsWon[1]) {
      return teamA;
    }
    if (setsWon[1] > setsWon[0]) {
      return teamB;
    }
    return null;
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

  private static class SetCounter {

    int teamAWonSets = 0;
    int teamBWonSets = 0;
  }

}
