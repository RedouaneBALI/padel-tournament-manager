package io.github.redouanebali.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long             id;
  private String           name;
  private String           description;
  private String           City;
  private String           Club;
  private Gender           Gender;
  private TournamentLevel  Level;
  @ElementCollection
  private List<Round>      rounds;
  private int              nbSeeds;
  @ElementCollection
  private List<PlayerPair> playerPairs;

}
