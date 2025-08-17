package io.github.redouanebali.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
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

  @JsonProperty("currentRoundStage")
  public Stage getCurrentRoundStage() {
    if (rounds == null || rounds.isEmpty()) {
      return null;
    }

    for (int i = rounds.size() - 1; i >= 0; i--) {
      Round r = rounds.get(i);
      if (r.getGames() == null || r.getGames().isEmpty()) {
        continue;
      }
      boolean hasAssigned = r.getGames().stream()
                             .anyMatch(g -> g.getTeamA() != null || g.getTeamB() != null);
      if (hasAssigned) {
        return r.getStage();
      }
    }

    return rounds.getFirst().getStage();
  }

}
