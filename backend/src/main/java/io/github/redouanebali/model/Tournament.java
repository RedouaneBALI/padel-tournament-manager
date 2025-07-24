package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
  private String           city;
  private String           club;
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat tournamentFormat;
  @OneToMany
  private List<Round>      rounds;
  private int              nbSeeds;
  @OneToMany(orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<PlayerPair> playerPairs;
  private String           startDate; // @todo find a better format than String ?
  private String           endDate; // @todo find a better format than String ?
  private int              nbMaxPairs;

}
