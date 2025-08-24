package io.github.redouanebali.model.format.groupsko;

import io.github.redouanebali.generation.GroupRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.StageKey;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.List;

public class GroupsKoStrategy implements FormatStrategy {

  @Override
  public void validate(TournamentFormatConfig cfg, List<String> errors) {
    if (cfg.getNbPools() == null || cfg.getNbPools() <= 0) {
      errors.add("nbPools must be > 0.");
    }
    if (cfg.getNbPairsPerPool() == null || cfg.getNbPairsPerPool() < 2) {
      errors.add("nbPairsPerPool must be >= 2.");
    }
    if (cfg.getNbQualifiedByPool() == null || cfg.getNbQualifiedByPool() < 1) {
      errors.add("nbQualifiedByPool must be >= 1.");
    }
    if (cfg.getNbPools() != null && cfg.getNbPairsPerPool() != null) {
      int totalPlayers = cfg.getNbPools() * cfg.getNbPairsPerPool();
      if (cfg.getNbSeeds() != null && cfg.getNbSeeds() > totalPlayers) {
        errors.add("nbSeeds > total players");
      }
      if (cfg.getNbPools() != null && cfg.getNbQualifiedByPool() != null) {
        int qualifiers = cfg.getNbPools() * cfg.getNbQualifiedByPool();
        if (!DrawMath.isPowerOfTwo(qualifiers)) {
          errors.add("qualifiers should be a power of two");
        }
      }
    }
  }

  @Override
  public List<StageKey> stages(TournamentFormatConfig cfg) {
    return List.of(StageKey.GROUPS, StageKey.MAIN_DRAW);
  }

  /*@Override @todo to remove
  public void buildEmptyRounds(Tournament t, TournamentFormatConfig cfg) {
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds() != null ? cfg.getNbSeeds() : 0,
        cfg.getNbPools() != null ? cfg.getNbPools() : 0,
        cfg.getNbPairsPerPool() != null ? cfg.getNbPairsPerPool() : 0,
        cfg.getNbQualifiedByPool() != null ? cfg.getNbQualifiedByPool() : 0
    );
    List<Round> rounds = generator.createRoundsStructure(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
  }*/


  @Override
  public List<Round> initializeRounds(Tournament t, List<PlayerPair> pairs, boolean manual) {
    TournamentFormatConfig cfg = t.getConfig();
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds() != null ? cfg.getNbSeeds() : 0,
        cfg.getNbPools() != null ? cfg.getNbPools() : 0,
        cfg.getNbPairsPerPool() != null ? cfg.getNbPairsPerPool() : 0,
        cfg.getNbQualifiedByPool() != null ? cfg.getNbQualifiedByPool() : 0
    );
    return manual ? generator.generateManualRounds(pairs)
                  : generator.generateAlgorithmicRounds(pairs);
  }

  @Override
  public void propagateWinners(Tournament t) {
    TournamentFormatConfig cfg = t.getConfig();
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds() != null ? cfg.getNbSeeds() : 0,
        cfg.getNbPools() != null ? cfg.getNbPools() : 0,
        cfg.getNbPairsPerPool() != null ? cfg.getNbPairsPerPool() : 0,
        cfg.getNbQualifiedByPool() != null ? cfg.getNbQualifiedByPool() : 0
    );
    generator.propagateWinners(t);
  }
}