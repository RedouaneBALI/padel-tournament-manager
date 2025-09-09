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

  @Builder.Default
  private Integer  mainDrawSize      = 0;
  @Builder.Default
  private Integer  nbSeeds           = 0;
  @Builder.Default
  private DrawMode drawMode          = DrawMode.SEEDED;
  @Builder.Default
  private Boolean  staggeredEntry    = false;
  @Builder.Default
  private Integer  nbPools           = 0;
  @Builder.Default
  private Integer  nbPairsPerPool    = 0;
  @Builder.Default
  private Integer  nbQualifiedByPool = 0;
  @Builder.Default
  private Integer  preQualDrawSize   = 0;
  @Builder.Default
  private Integer  nbQualifiers      = 0;
  @Builder.Default
  private Integer  nbSeedsQualify    = 0;

  // @todo add constructors wihth TournamentFormat to set defaults

  public int getNbMaxPairs(TournamentFormat format) {
    switch (format) {
      case KNOCKOUT -> {
        return mainDrawSize;
      }
      case GROUPS_KO -> {
        return nbPairsPerPool * nbPairsPerPool;
      }
      case QUALIF_KO -> {
        return mainDrawSize + preQualDrawSize;
      }
    }
    return 0;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class TournamentFormatConfigBuilder {

  }


}