package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.model.format.groupsko.GroupsKoStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


public class GroupsKoStrategyTest {

  @ParameterizedTest
  @CsvSource({
      "4,4,2,8,4,true",
      "2,4,1,2,0,true",
      "0,1,0,12,32,false",
      "3,2,1,8,4,false"
  })
  void validate_variousConfigs(int nbPools, int nbPairsPerPool, int nbQualifiedByPool, int mainDrawSize, int nbSeeds, boolean expectedValid) {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder().nbPools(nbPools).nbPairsPerPool(nbPairsPerPool)
                                                       .nbQualifiedByPool(nbQualifiedByPool).mainDrawSize(mainDrawSize).nbSeeds(nbSeeds).build();

    List<String> errors = new java.util.ArrayList<>();
    strategy.validate(cfg, errors);
    if (expectedValid) {
      assertTrue(errors.isEmpty());
    } else {
      assertFalse(errors.isEmpty());
    }
  }

  @Test
  void validate_fails_whenQualifiersMismatchMainDrawSize() {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder().nbPools(3).nbPairsPerPool(1)
                                                       .nbQualifiedByPool(1).mainDrawSize(8).nbSeeds(0).build();

    List<String> errors = new java.util.ArrayList<>();
    strategy.validate(cfg, errors);
    assertFalse(errors.isEmpty(), "Validation must fail when nbPools * nbQualifiedByPool != mainDrawSize");
  }

  @Test
  void propagateWinners_executesWithoutError() {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .nbPools(2).nbPairsPerPool(4).nbQualifiedByPool(2).mainDrawSize(4).nbSeeds(0)
                                                       .build();

    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.GROUPS_KO);
    t.setConfig(cfg);

    // Assume some winners are filled in manually here (skipped for brevity)

    // Just check that propagation doesn't throw and preserves round structure
    int roundCountBefore = t.getRounds().size();
    strategy.propagateWinners(t);
    assertEquals(roundCountBefore, t.getRounds().size(), "Round count should remain unchanged after propagation");
  }

  @Test
  void validate_fails_whenValuesAreNull() {
    GroupsKoStrategy       strategy = new GroupsKoStrategy();
    TournamentFormatConfig cfg      = TournamentFormatConfig.builder().build();

    List<String> errors = new java.util.ArrayList<>();
    strategy.validate(cfg, errors);

    assertFalse(errors.isEmpty(), "Validation must fail when config fields are missing");
    assertTrue(errors.stream().anyMatch(e -> e.contains("nbPools must be > 0.")));
    assertTrue(errors.stream().anyMatch(e -> e.contains("nbPairsPerPool must be >= 2.")));
    assertTrue(errors.stream().anyMatch(e -> e.contains("nbQualifiedByPool must be >= 1.")));
  }

}
