package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Round {

  private RoundInfo        info;
  private List<PlayerPair> playerPairs = new ArrayList<>();
  private List<Game>       games       = new ArrayList<>();
  private MatchFormat      matchFormat;

  public Round(RoundInfo roundInfo) {
    this.info = roundInfo;
  }

}
