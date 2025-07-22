package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

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
  private TournamentFormat tournamentFormat;
  @OneToMany(cascade = CascadeType.PERSIST)
  private List<Round>      rounds;
  private int              nbSeeds;
  @OneToMany(cascade = CascadeType.PERSIST)
  private List<PlayerPair> playerPairs;
  private String startDate; // @todo find a better format than String ?
  private String endDate; // @todo find a better format than String ?

}
