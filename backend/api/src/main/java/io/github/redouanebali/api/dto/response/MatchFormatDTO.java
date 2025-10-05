package io.github.redouanebali.api.dto.response;

import lombok.Data;

@Data
public class MatchFormatDTO {

  private int     numberOfSetsToWin;
  private int     gamesPerSet;
  private boolean advantage;
  private boolean superTieBreakInFinalSet;
}
