package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchFormat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Min(1)
  private int     numberOfSetsToWin       = 2;        // Ex: 2 for a match with 2 winning sets
  @Min(1)
  private int     gamesPerSet             = 6;        // Ex: 6 games to win a set
  private boolean superTieBreakInFinalSet = false;    // true if the 3rd set is a super tie-break
  private boolean advantage               = true;     // true = advantage game, false = no-ad

}
