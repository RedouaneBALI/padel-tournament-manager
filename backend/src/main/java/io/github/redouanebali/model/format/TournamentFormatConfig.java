package io.github.redouanebali.model.format;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonDeserialize(builder = TournamentFormatConfig.TournamentFormatConfigBuilder.class)
public class TournamentFormatConfig {

  // Commun à tous les formats
  private Integer mainDrawSize; // Ex: 32, 64
  private Integer nbSeeds;      // Ex: 8, 16

  // Pour GROUPS_KO
  private Integer nbPools;             // Ex: 4, 8
  private Integer nbPairsPerPool;      // Ex: 3, 4
  private Integer nbQualifiedByPool;   // Ex: 1, 2

  // Pour QUALIF_KNOCKOUT
  private Integer preQualDrawSize;     // Ex: 16
  private Integer nbQualifiers;        // Ex: 4
  private Integer nbSeedsQualify;      // Ex: 8

  public int getNbMaxPairs(TournamentFormat format) {
    switch (format) {
      case KNOCKOUT -> {
        return mainDrawSize;
      }
      case GROUPS_KO -> {
        return nbPairsPerPool * nbPairsPerPool;
      }
      case QUALIF_MAIN -> {
        return mainDrawSize + preQualDrawSize;
      }
    }
    return 0;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class TournamentFormatConfigBuilder {

  }


}