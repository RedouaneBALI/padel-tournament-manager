package io.github.redouanebali.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

  private Long             id;
  private String           name;
  private String           description;
  private String           City;
  private String           Club;
  private Gender           Gender;
  private TournamentLevel  Level;
  private List<Round>      rounds;
  private List<PlayerPair> playerPairs;

}
