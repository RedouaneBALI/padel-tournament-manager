package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

  private PlayerPair  teamA;
  private PlayerPair  teamB;
  private MatchFormat format;
  private Score       score;

}
