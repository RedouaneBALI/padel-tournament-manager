package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Score {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "score_id")
  @OrderColumn(name = "order_index") // persists list order
  private List<SetScore> sets = new ArrayList<>();

  // Indicates if the match ended by forfeit/retirement
  private boolean forfeit = false;

  // Indicates which team forfeited (TEAM_A or TEAM_B)
  @Enumerated(EnumType.STRING)
  private TeamSide forfeitedBy;

  // Current game points (live game score), can be null if not used
  @Enumerated(EnumType.STRING)
  private GamePoint currentGamePointA;

  @Enumerated(EnumType.STRING)
  private GamePoint currentGamePointB;

  // Tie-break points (used only in tie-break or super tie-break games)
  private Integer tieBreakPointA;
  private Integer tieBreakPointB;

  // Historique pour undo multi-niveaux (chaînage)
  private transient Score previousScore;

  public static Score fromString(String scoreStr) {
    Score score = new Score();
    if (scoreStr == null || scoreStr.isBlank()) {
      return score;
    }
    String[] setStrings = scoreStr.trim().split("[,\\s]+");
    for (String setStr : setStrings) {
      String[] parts = setStr.split("-");
      if (parts.length == 2) {
        try {
          int teamAScore = Integer.parseInt(parts[0]);
          int teamBScore = Integer.parseInt(parts[1]);
          score.addSetScore(teamAScore, teamBScore);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid score format: " + setStr, e);
        }
      } else {
        throw new IllegalArgumentException("Invalid set score format: " + setStr);
      }
    }
    return score;
  }

  /**
   * Creates a forfeit score indicating which team forfeited. The current score (if any) is preserved.
   *
   * @param forfeitedBy the side that forfeited (TEAM_A or TEAM_B)
   * @return a Score marked as forfeit
   */
  public static Score forfeit(TeamSide forfeitedBy) {
    Score score = new Score();
    score.setForfeit(true);
    score.setForfeitedBy(forfeitedBy);
    return score;
  }

  /**
   * Creates a forfeit score with partial score preserved.
   *
   * @param partialScore the score at the moment of forfeit
   * @param forfeitedBy the side that forfeited
   * @return a Score with partial results marked as forfeit
   */
  public static Score forfeitWithPartialScore(Score partialScore, TeamSide forfeitedBy) {
    if (partialScore == null) {
      return forfeit(forfeitedBy);
    }
    partialScore.setForfeit(true);
    partialScore.setForfeitedBy(forfeitedBy);
    return partialScore;
  }

  public void saveToHistory() {
    Score prev = this.deepCopy();
    prev.setPreviousScore(this.getPreviousScore()); // chaînage via setter
    this.setPreviousScore(prev);
  }

  public boolean canUndo() {
    return previousScore != null;
  }

  public Score deepCopy() {
    Score copy = new Score();
    copy.copyFrom(this);
    copy.previousScore = this.previousScore;
    return copy;
  }

  private void copyFrom(Score source) {
    this.sets              = source.getSets().stream()
                                   .map(s -> new SetScore(
                                       s.getTeamAScore(),
                                       s.getTeamBScore(),
                                       s.getTieBreakTeamA(),
                                       s.getTieBreakTeamB()
                                   ))
                                   .collect(Collectors.toList());
    this.forfeit           = source.isForfeit();
    this.forfeitedBy       = source.getForfeitedBy();
    this.currentGamePointA = source.getCurrentGamePointA();
    this.currentGamePointB = source.getCurrentGamePointB();
    this.tieBreakPointA    = source.getTieBreakPointA();
    this.tieBreakPointB    = source.getTieBreakPointB();
  }

  public void undo() {
    if (canUndo()) {
      this.copyFrom(getPreviousScore());
      this.setPreviousScore(getPreviousScore() != null ? getPreviousScore().getPreviousScore() : null);
    }
  }

  /**
   * Marks this score as forfeit.
   *
   * @param forfeitedBy the side that forfeited
   */
  public void markAsForfeit(TeamSide forfeitedBy) {
    this.forfeit     = true;
    this.forfeitedBy = forfeitedBy;
  }

  public void addSetScore(int teamAScore, int teamBScore) {
    sets.add(new SetScore(teamAScore, teamBScore));
  }

  public void addSetScore(int teamAScore, int teamBScore, Integer tieBreakAScore, Integer tieBreakBScore) {
    sets.add(new SetScore(teamAScore, teamBScore, tieBreakAScore, tieBreakBScore));
  }

  @Override
  public String toString() {
    String setsStr = sets.stream()
                         .map(set -> set.getTeamAScore() + "-" + set.getTeamBScore())
                         .collect(Collectors.joining(" "));

    String gamePoints = "";
    // Display tie-break points if present (priority over GamePoint)
    if (tieBreakPointA != null && tieBreakPointB != null) {
      gamePoints = " (" + tieBreakPointA + "-" + tieBreakPointB + ")";
    } else if (currentGamePointA != null && currentGamePointB != null) {
      gamePoints = " (" + currentGamePointA.getDisplay() + "-" + currentGamePointB.getDisplay() + ")";
    }

    if (forfeit) {
      String forfeitInfo = forfeitedBy != null ? " (Forfeit by " + forfeitedBy + ")" : " (Forfeit)";
      return setsStr.isEmpty() ? forfeitInfo.trim() : setsStr + forfeitInfo;
    }

    return setsStr + (gamePoints.isEmpty() ? "" : " " + gamePoints);
  }

}
