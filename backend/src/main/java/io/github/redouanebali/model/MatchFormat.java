package io.github.redouanebali.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchFormat {

  private int     numberOfSetsToWin; // 1 or 2
  private int     pointsPerSet; // 4, 6 or 9
  private boolean superTieBreakInFinalSet;
}
