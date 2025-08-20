package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.GroupsKoConfig;
import io.github.redouanebali.model.format.GroupsKoStrategy;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.util.TestFixtures;
import java.util.List;
import java.util.Set;
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
  void validate_variousConfigs(int npPools, int nbPairsPerPool, int nbQualifiedByPool, int mainDrawSize, int nbSeeds, boolean expectedValid) {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    GroupsKoConfig   cfg      = new GroupsKoConfig(npPools, nbPairsPerPool, nbQualifiedByPool, mainDrawSize, nbSeeds);

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
    GroupsKoConfig   cfg      = new GroupsKoConfig(nbPools, nbPairsPerPool, nbQualifiedByPool, mainDrawSize, 0);

    Tournament t = new Tournament();
    t.setTournamentFormat(TournamentFormat.GROUPS_KO);
    t.setNbMaxPairs(nbPools * nbPairsPerPool);

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
    // 3 pools * 1 qualified = 3 != 8
    GroupsKoConfig cfg = new GroupsKoConfig(3, 2, 1, 8, 2);

    List<String> errors = new java.util.ArrayList<>();
    strategy.validate(cfg, errors);
    assertFalse(errors.isEmpty(), "Validation must fail when nbPools * nbQualifiedByPool != mainDrawSize");
  }

  @Test
  void propagateWinners_noScores_doesNotThrow() {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    GroupsKoConfig   cfg      = new GroupsKoConfig(2, 3, 2, 4, 0);

    Tournament t = new Tournament();
    t.setTournamentFormat(TournamentFormat.GROUPS_KO);

    // init structure
    t.getRounds().clear();
    strategy.buildInitialRounds(t, cfg);

    // no scores at group stage → calling propagate should not throw (strategy may no-op)
    strategy.propagateWinners(t);

    // still has final round present
    Round finalRound = t.getRounds().get(t.getRounds().size() - 1);
    assertEquals(Stage.FINAL, finalRound.getStage());
  }

  @Test
  void groupsRanking_drivesMainDrawQualifiers() {
    // Build structure
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    GroupsKoConfig   cfg      = new GroupsKoConfig(2, 4, 2, 4, 0); // 2 pools, top-2 qualify → 4-team main draw

    Tournament t = new Tournament();
    t.setTournamentFormat(TournamentFormat.GROUPS_KO);
    t.setFormatConfig(cfg); // ensure propagateWinners reads the typed config

    // Build structure
    strategy.buildInitialRounds(t, cfg);

    // Create 8 pairs and let the strategy create pools and games
    List<PlayerPair> pairs = new java.util.ArrayList<>(TestFixtures.createPairs(8));

    // Let the strategy create pools and schedule group games properly (with pool linkage)
    Round generatedGroups = strategy.generateRound(t, pairs, true);

    // Apply generated pools and games to the stored GROUPS round
    TestFixtures.applyGeneratedGroups(t, generatedGroups);
    Round groups = TestFixtures.findRound(t, Stage.GROUPS);

    // Now read the pools actually attached to the tournament's GROUPS round
    List<Pool> pools = groups.getPools();
    assertEquals(2, pools.size(), "Expected 2 pools created by strategy");

    Pool poolA = pools.get(0);
    Pool poolB = pools.get(1);

    List<PlayerPair> aPairs = TestFixtures.sortedPairsBySeed(poolA.getPairs());
    List<PlayerPair> bPairs = TestFixtures.sortedPairsBySeed(poolB.getPairs());

    // Choose top-2 candidates per pool (deterministic: first two in each list)
    PlayerPair a1 = aPairs.get(0);
    PlayerPair a2 = aPairs.get(1);
    PlayerPair b1 = bPairs.get(0);
    PlayerPair b2 = bPairs.get(1);

    // For each pool, simulate a round-robin: make a1 and a2 win their matches vs others,
    // and in their head-to-head, let a1 win. Do the same for pool B.
    TestFixtures.simulatePoolWinners(groups, poolA, a1, a2);
    TestFixtures.simulatePoolWinners(groups, poolB, b1, b2);

    // Propagate from groups to KO based on simulated results
    strategy.propagateWinners(t);

    // Expect the 4 qualifiers in SEMIS: top-2 from each pool
    Round           semis = t.getRounds().stream().filter(r -> r.getStage() == Stage.SEMIS).findFirst().orElseThrow();
    Set<PlayerPair> teams = TestFixtures.teamsInRound(semis);

    Set<PlayerPair> expected = Set.of(a1, a2, b1, b2);
    assertEquals(expected, teams);
  }

  @ParameterizedTest(name = "generate groups: pools={0}, perPool={1}")
  @CsvSource({
      "2,4",
      "3,3"
  })
  void generateRound_createsPoolsAndGroupGames(int nbPools, int nbPairsPerPool) {
    GroupsKoStrategy strategy = new GroupsKoStrategy();
    GroupsKoConfig   cfg      = new GroupsKoConfig(nbPools, nbPairsPerPool, 1, nbPools, 0);

    Tournament t = new Tournament();
    t.setTournamentFormat(TournamentFormat.GROUPS_KO);
    t.setFormatConfig(cfg);

    strategy.buildInitialRounds(t, cfg);

    // Create all pairs and ask the strategy to generate groups
    List<PlayerPair> pairs           = new java.util.ArrayList<>(TestFixtures.createPairs(nbPools * nbPairsPerPool));
    Round            generatedGroups = strategy.generateRound(t, pairs, true);

    // Merge into stored groups round (like the service would do)
    TestFixtures.applyGeneratedGroups(t, generatedGroups);
    Round groups = TestFixtures.findRound(t, Stage.GROUPS);

    // Check pools count and total group games
    assertEquals(nbPools, groups.getPools().size());
    int expectedGames = TestFixtures.totalGroupGames(nbPools, nbPairsPerPool);
    assertEquals(expectedGames, groups.getGames().size());
  }

}

