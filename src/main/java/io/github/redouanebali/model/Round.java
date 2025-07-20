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

  RoundName   name;
  List<Game>  games = new ArrayList<>();
  MatchFormat matchFormat;

}
