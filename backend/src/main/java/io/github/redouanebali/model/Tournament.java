package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
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
  private String           city;
  private String           club;
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat tournamentFormat;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<Round>      rounds      = new ArrayList<>();
  private int              nbSeeds;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<PlayerPair> playerPairs = new ArrayList<>();
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        startDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        endDate;
  private int              nbMaxPairs;
  // for group stage
  private int              nbPools;
  private int              nbPairsPerPool;
  private int              nbQualifiedByPool;

}
