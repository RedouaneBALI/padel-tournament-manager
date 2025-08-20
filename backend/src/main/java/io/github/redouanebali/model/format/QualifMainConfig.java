package io.github.redouanebali.model.format;

import lombok.Data;

/**
 * Typed configuration for a tournament with Pre-Qualifying (Qualifs) feeding a Main Draw.
 */
@Data
public class QualifMainConfig implements TournamentConfig {

  /**
   * Size of the pre-qualifying draw (power of two). Example: 16.
   */
  private int preQualDrawSize = 16;

  /**
   * Number of teams that qualify from pre-qualifying into the main draw. Example: 4.
   */
  private int numQualifiers = 4;

  /**
   * Size of the main draw (power of two). Example: 16 or 32.
   */
  private int mainDrawSize = 16;

  /**
   * Number of seeds in the main draw. Example: 8.
   */
  private int nbSeeds = 0;

  /**
   * Direct acceptances into main draw (ranking-based entries).
   */
  private int directAcceptances = 8;

  /**
   * Wildcards directly placed into main draw.
   */
  private int wildcards = 0;

  /**
   * Policy for placing qualifiers into the main draw. Allowed: "randomize", "ordered".
   */
  private String qualifierPlacementPolicy = "randomize";

  /**
   * Policy for distributing BYEs (e.g., "top-seeds").
   */
  private String byeDistributionPolicy = "top-seeds";
}
