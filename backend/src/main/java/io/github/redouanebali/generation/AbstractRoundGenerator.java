package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public abstract class AbstractRoundGenerator implements RoundGenerator {

  private final int nbSeeds;

  /**
   * Factory: return the appropriate generator for a given tournament.
   */
  public static AbstractRoundGenerator of(final Tournament tournament) {
    if (tournament == null || tournament.getTournamentFormat() == null) {
      throw new IllegalArgumentException("Tournament or format is null");
    }
    return switch (tournament.getTournamentFormat()) {
      case KNOCKOUT -> new KnockoutRoundGenerator(tournament.getNbSeeds());
      case GROUP_STAGE -> new GroupRoundGenerator(
          tournament.getNbSeeds(),
          tournament.getNbPools(),
          tournament.getNbPairsPerPool()
      );
      default -> throw new IllegalArgumentException("Unsupported tournament format: " + tournament.getTournamentFormat());
    };
  }

  public void addMissingByePairsToReachPowerOfTwo(List<PlayerPair> pairs, int originalSize) {
    int powerOfTwo = 1;
    while (powerOfTwo < originalSize) {
      powerOfTwo *= 2;
    }
    int missing = powerOfTwo - originalSize;

    for (int i = 0; i < missing; i++) {
      PlayerPair bye = PlayerPair.bye();
      pairs.add(bye);
    }
  }


  /**
   * Optional propagation hook, overridden by specific generators if needed.
   */
  public abstract void propagateWinners(List<Round> sortedRounds);
}
