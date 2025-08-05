package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
  private String           city;
  private String           club;
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat tournamentFormat;
  @OneToMany
  @OrderBy("stage ASC")
  private Set<Round>       rounds;
  private int              nbSeeds;
  @OneToMany(orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<PlayerPair> playerPairs;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  @DateTimeFormat(pattern = "dd/MM/yyyy")
  private LocalDate        startDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  @DateTimeFormat(pattern = "dd/MM/yyyy")
  private LocalDate        endDate;
  private int              nbMaxPairs;
  // for group stage
  private int              nbPools;
  private int              nbPairsPerPool;
  private int              nbQualifiedByPool;

}
