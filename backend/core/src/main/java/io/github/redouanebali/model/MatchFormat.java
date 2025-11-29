package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MatchFormat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Min(1)
  private int     numberOfSetsToWin       = 2;        // Ex: 2 for a match with 2 winning sets
  @Min(1)
  private int     gamesPerSet             = 6;        // Ex: 6 games to win a set
  private boolean superTieBreakInFinalSet = true;    // true if the 3rd set is a super tie-break
  private boolean advantage               = false;     // true = advantage game, false = no-ad
  @Min(1)
  private int     tieBreakAt              = 6;        // Score at which tie-break starts (gamesPerSet or gamesPerSet-1)

  public MatchFormat(Long id, int numberOfSetsToWin, int gamesPerSet, boolean superTieBreakInFinalSet, boolean advantage) {
    this.id                      = id;
    this.numberOfSetsToWin       = numberOfSetsToWin;
    this.gamesPerSet             = gamesPerSet;
    this.superTieBreakInFinalSet = superTieBreakInFinalSet;
    this.advantage               = advantage;
    this.tieBreakAt              = gamesPerSet; // By default, tie-break at the same score as gamesPerSet
  }

  public MatchFormat(Long id, int numberOfSetsToWin, int gamesPerSet, boolean superTieBreakInFinalSet, boolean advantage, int tieBreakAt) {
    this.id                      = id;
    this.numberOfSetsToWin       = numberOfSetsToWin;
    this.gamesPerSet             = gamesPerSet;
    this.superTieBreakInFinalSet = superTieBreakInFinalSet;
    this.advantage               = advantage;
    setTieBreakAt(tieBreakAt);
  }

  public void setTieBreakAt(int tieBreakAt) {
    if (tieBreakAt != gamesPerSet && tieBreakAt != gamesPerSet - 1) {
      throw new IllegalArgumentException(
          "tieBreakAt must be either gamesPerSet (" + gamesPerSet + ") or gamesPerSet-1 (" + (gamesPerSet - 1) + ")"
      );
    }
    this.tieBreakAt = tieBreakAt;
  }

  public void setGamesPerSet(int gamesPerSet) {
    this.gamesPerSet = gamesPerSet;
    // Adjust tieBreakAt to match the new gamesPerSet value
    this.tieBreakAt = gamesPerSet;
  }
}
