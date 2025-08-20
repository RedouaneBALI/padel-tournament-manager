package io.github.redouanebali.model.format;

import io.github.redouanebali.generation.GroupRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public class GroupsKoStrategy implements FormatStrategy<GroupsKoConfig> {

  @Override
  public void validate(GroupsKoConfig cfg, List<String> errors) {
    if (cfg.getNbPools() <= 0) {
      errors.add("nbPools must be > 0.");
    }
    if (cfg.getNbPairsPerPool() < 2) {
      errors.add("nbPairsPerPool must be >= 2.");
    }
    if (cfg.getNbQualifiedByPool() < 1) {
      errors.add("nbQualifiedByPool must be >= 1.");
    }
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
    int qualifiers = cfg.getNbPools() * cfg.getNbQualifiedByPool();
    if (qualifiers != cfg.getMainDrawSize()) {
      errors.add("nbPools * nbQualifiedByPool must equal mainDrawSize (got " + qualifiers + " vs " + cfg.getMainDrawSize() + ").");
    }
  }

  @Override
  public List<StageKey> stages(GroupsKoConfig cfg) {
    return List.of(StageKey.GROUPS, StageKey.MAIN_DRAW);
  }

  @Override
  public void buildInitialRounds(Tournament t, GroupsKoConfig cfg) {
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds(),
        cfg.getNbPools(),
        cfg.getNbPairsPerPool(),
        cfg.getNbQualifiedByPool()
    );
    List<Round> rounds = generator.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(rounds);
  }

  @Override
  public Round generateRound(Tournament t, List<PlayerPair> pairs, boolean manual) {
    if (!(t.getFormatConfig() instanceof GroupsKoConfig cfg)) {
      throw new IllegalArgumentException("Tournament.formatConfig must be a GroupsKoConfig for GROUPS_KO format");
    }
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds(),
        cfg.getNbPools(),
        cfg.getNbPairsPerPool(),
        cfg.getNbQualifiedByPool()
    );
    return manual ? generator.generateManualRound(pairs)
                  : generator.generateAlgorithmicRound(pairs);
  }

  @Override
  public void propagateWinners(Tournament t) {
    if (!(t.getFormatConfig() instanceof GroupsKoConfig cfg)) {
      return;
    }
    GroupRoundGenerator generator = new GroupRoundGenerator(
        cfg.getNbSeeds(),
        cfg.getNbPools(),
        cfg.getNbPairsPerPool(),
        cfg.getNbQualifiedByPool()
    );
    generator.propagateWinners(t);
  }
}