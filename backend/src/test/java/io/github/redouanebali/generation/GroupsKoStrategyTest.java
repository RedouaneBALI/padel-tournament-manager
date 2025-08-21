package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
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

  @ParameterizedTest(name = "groups={0}, pairsPerGroup={1}, qualify={2} → mainDraw={3}")
  @CsvSource({
      // 2 poules de 4 avec le premier qualifié → 2 qualifiés → MD=2
      "2,4,1,2",
      // 2 poules de 5 avec les deux premiers qualifiés → 4 qualifiés → MD=4
      "2,5,2,4",
      // 4 poules de 4 avec le premier qualifié → 4 qualifiés → MD=4
      "4,4,1,4",
      // 4 poules de 4 avec les deux premiers qualifiés → 8 qualifiés → MD=8
      "4,4,2,8"
  })
  void buildInitialRounds_createsGroupsAndKnockoutStructure(int nbPools, int nbPairsPerPool, int nbQualifiedByPool, int mainDrawSize) {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder().nbPools(nbPools).nbPairsPerPool(nbPairsPerPool)
                                                       .nbQualifiedByPool(nbQualifiedByPool).mainDrawSize(mainDrawSize).nbSeeds(0).build();

    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.GROUPS_KO);
    t.setConfig(cfg);

    // When
    t.getRounds().clear();
    List<String> errors = new java.util.ArrayList<>();
    strategy.validate(cfg, errors);
    assertTrue(errors.isEmpty());

    strategy.buildInitialRounds(t, cfg);

    // Then: GROUPS round exists and KO structure matches mainDrawSize
    assertFalse(t.getRounds().isEmpty());

    Round groups = t.getRounds().stream().filter(r -> r.getStage() == Stage.GROUPS).findFirst().orElse(null);
    assertNotNull(groups, "GROUPS round should exist");

    int  expectedKoRounds = (int) (Math.log(mainDrawSize) / Math.log(2));
    long koRounds         = t.getRounds().stream().filter(r -> r.getStage() != Stage.GROUPS).count();
    assertEquals(expectedKoRounds, koRounds, "Unexpected number of KO rounds");

    Round finalRound = t.getRounds().get(t.getRounds().size() - 1);
    assertEquals(Stage.FINAL, finalRound.getStage());
    assertEquals(1, finalRound.getGames().size());

    // Verify stages ordering and game counts for each KO round
    List<Round> ko = t.getRounds().stream()
                      .filter(r -> r.getStage() != Stage.GROUPS)
                      .toList();

    Stage expected   = Stage.fromNbTeams(mainDrawSize);
    int   totalGames = 0;
    for (Round r : ko) {
      assertEquals(expected, r.getStage(), "Unexpected stage order");
      int expectedGames = expected.getNbTeams() / 2;
      assertEquals(expectedGames, r.getGames().size(), "Unexpected games count for stage " + expected);
      totalGames += r.getGames().size();
      expected = expected.next();
    }
    // In a single-elimination bracket, total number of games equals mainDrawSize - 1
    assertEquals(mainDrawSize - 1, totalGames, "Total KO games should be mainDrawSize - 1");
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

    strategy.buildInitialRounds(t, cfg);

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
    assertTrue(errors.stream().anyMatch(e -> e.contains("mainDrawSize must be a power of two.")));
  }

}
