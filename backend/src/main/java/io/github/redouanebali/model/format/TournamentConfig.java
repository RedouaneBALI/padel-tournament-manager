package io.github.redouanebali.model.format;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonDeserialize(builder = TournamentConfig.TournamentConfigBuilder.class)
public class TournamentConfig {

  // The tournament format type (knockout, groups+knockout, qualifying+knockout)
  @Builder.Default
  private TournamentFormat format            = TournamentFormat.KNOCKOUT;
  // Number of pairs in the main draw (used for KNOCKOUT and QUALIF_KO formats)
  @Builder.Default
  private Integer          mainDrawSize      = 0;
  // Number of seeded pairs in the tournament
  @Builder.Default
  private Integer          nbSeeds           = 0;
  // Draw generation mode (seeded vs manual placement)
  @Builder.Default
  private DrawMode         drawMode          = DrawMode.SEEDED;
  // Whether higher seeds enter the tournament in later rounds
  @Builder.Default
  private Boolean          staggeredEntry    = false;
  // Number of pools in group phase (used for GROUPS_KO format)
  @Builder.Default
  private Integer          nbPools           = 0;
  // Number of pairs per pool in group phase (used for GROUPS_KO format)
  @Builder.Default
  private Integer          nbPairsPerPool    = 0;
  // Number of pairs that qualify from each pool to knockout phase
  @Builder.Default
  private Integer          nbQualifiedByPool = 0;
  // Size of the pre-qualifying draw (used for QUALIF_KO format)
  @Builder.Default
  private Integer          preQualDrawSize   = 0;
  // Number of pairs that qualify from pre-qualifying to main draw
  @Builder.Default
  private Integer          nbQualifiers      = 0;
  // Number of seeded pairs in the pre-qualifying draw (used for QUALIF_KO format)
  @Builder.Default
  private Integer          nbSeedsQualify    = 0;

  public int getNbMaxPairs() {
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
  public static class TournamentConfigBuilder {
    // Jackson trouvera automatiquement la méthode build() générée par Lombok
  }

}