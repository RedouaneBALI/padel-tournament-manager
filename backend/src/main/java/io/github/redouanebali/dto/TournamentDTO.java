package io.github.redouanebali.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.model.TournamentLevel;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class TournamentDTO {

  private Long             id;
  private String           ownerId;
  private String           name;
  private List<Round>      rounds;
  private List<PlayerPair> playerPairs;
  private String           description;
  private String           city;
  private String           club;
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat tournamentFormat;
  private int              nbSeeds;
  private LocalDate        startDate;
  private LocalDate        endDate;
  private int              nbMaxPairs;
  private int              nbPools;
  private int              nbPairsPerPool;
  private int              nbQualifiedByPool;

  @JsonProperty("isEditable")
  private boolean editable;
}
