package io.github.redouanebali.model.format;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typed configuration for a tournament with Group Stage followed by Knockout.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupsKoConfig implements TournamentConfig {

  /**
   * Number of groups (pools). Example: 4.
   */
  private int nbPools = 4;

  /**
   * Number of pairs per group. Example: 4.
   */
  private int nbPairsPerPool = 4;

  /**
   * Number of qualified pairs per group advancing to KO. Example: 2.
   */
  private int nbQualifiedByPool = 2;

  /**
   * Size of the main draw (power of two) that follows the groups. Example: 16.
   */
  private int mainDrawSize = 16;

  /**
   * Number of seeds applied in the main draw.
   */
  private int nbSeeds = 0;
}
