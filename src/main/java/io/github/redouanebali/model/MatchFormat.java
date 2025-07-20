package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchFormat {
  private int numberOfSetsToWin; // 1 or 2
  private int pointsPerSet; // 4, 6 or 9
  private boolean superTieBreakInFinalSet;
}
