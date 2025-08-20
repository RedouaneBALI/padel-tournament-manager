package io.github.redouanebali.model.format;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal, typed configuration for a pure knockout tournament. Keeps things simple: just the draw size and number of seeds.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnockoutConfig implements TournamentConfig {

  /**
   * Size of the main draw (power of two). Example: 8, 16, 32.
   */
  private int mainDrawSize = 16;

  /**
   * Number of seeds in the main draw. Example: 0, 4, 8.
   */
  private int nbSeeds = 0;
}