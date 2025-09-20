package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GroupPhaseTests {

  @ParameterizedTest(name = "Groups: {0} teams, {1} pools of {2}, {3} qualified per pool")
  @CsvSource({
      // totalTeams, nbPools, nbPairsPerPool, nbQualifiedByPool
      "16, 4, 4, 1",    // 4 groups of 4, 1 qualified → 4 total qualified
      "16, 4, 4, 2",    // 4 groups of 4, 2 qualified → 8 total qualified
      "12, 4, 3, 1",    // 4 groups of 3, 1 qualified → 4 total qualified
      "12, 4, 3, 2",    // 4 groups of 3, 2 qualified → 8 total qualified
      "8, 2, 4, 2",     // 2 groups of 4, 2 qualified → 4 total qualified
      "8, 2, 4, 1",     // 2 groups of 4, 1 qualified → 2 total qualified
      "10, 2, 5, 2",    // 2 groups of 5, 2 qualified → 4 total qualified
      "10, 2, 5, 4",    // 2 groups of 5, 4 qualified → 8 total qualified
      "24, 8, 3, 1",    // 8 groups of 3, 1 qualified → 8 total qualified
      "32, 8, 4, 1",    // 8 groups of 4, 1 qualified → 8 total qualified
  })
  void testInitializeGroupStructure(int totalTeams, int nbPools, int nbPairsPerPool, int nbQualifiedByPool) {
    // Given
    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .nbQualifiedByPool(nbQualifiedByPool)
                                              .build();

    GroupPhase groupPhase = new GroupPhase(nbPools, nbPairsPerPool, nbQualifiedByPool);

    // When
    List<Round> rounds = groupPhase.initialize(config);

    // Then
    assertEquals(1, rounds.size(), "Should have exactly one round for groups");

    Round groupRound = rounds.get(0);
    assertEquals(Stage.GROUPS, groupRound.getStage(), "Round should be GROUPS stage");

    // Verify pools are created
    assertNotNull(groupRound.getPools(), "Pools should not be null");
    assertEquals(nbPools, groupRound.getPools().size(), "Should have correct number of pools");

    // Verify total qualified count is a power of 2 (for knockout phase)
    int totalQualified = nbPools * nbQualifiedByPool;
    assertTrue(DrawMath.isPowerOfTwo(totalQualified),
               "Total qualified (" + totalQualified + ") should be a power of 2 for knockout phase");
  }

  @ParameterizedTest(name = "Team placement: {0} teams in {1} pools of {2}")
  @CsvSource({
      "12, 4, 3",
      "16, 4, 4",
      "8, 2, 4",
      "10, 2, 5",
      "24, 8, 3",
  })
  void testTeamPlacementInPools(int totalTeams, int nbPools, int nbPairsPerPool) {
    // Given
    GroupPhase groupPhase = new GroupPhase(nbPools, nbPairsPerPool, 1);
    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .build();

    List<Round> rounds     = groupPhase.initialize(config);
    Round       groupRound = rounds.get(0);

    List<PlayerPair> teams = TestFixtures.createPlayerPairs(totalTeams);

    // When
    groupPhase.placeRemainingTeamsRandomly(groupRound, teams);

    // Then
    // Verify all teams are placed
    Set<PlayerPair> allPlacedTeams = groupRound.getPools().stream()
                                               .flatMap(pool -> pool.getPairs().stream())
                                               .collect(Collectors.toSet());

    assertEquals(totalTeams, allPlacedTeams.size(), "All teams should be placed in pools");
    assertEquals(new HashSet<>(teams), allPlacedTeams, "Exactly the same teams should be placed");

    // Verify each pool has the correct number of teams
    for (Pool pool : groupRound.getPools()) {
      assertEquals(nbPairsPerPool, pool.getPairs().size(),
                   "Each pool should have exactly " + nbPairsPerPool + " teams");
    }

    // Verify no team appears in multiple pools
    Set<PlayerPair> uniqueTeams = new HashSet<>();
    for (Pool pool : groupRound.getPools()) {
      for (PlayerPair team : pool.getPairs()) {
        assertTrue(uniqueTeams.add(team), "Team should not appear in multiple pools: " + team);
      }
    }
  }

  @ParameterizedTest(name = "Seeded teams distribution: {0} teams, {1} pools, {2} seeds")
  @CsvSource({
      "12, 4, 4",  // 4 seeds in 4 pools (1 seed per pool)
      "16, 4, 4",  // 4 seeds in 4 pools (1 seed per pool)
      "16, 4, 8",  // 8 seeds in 4 pools (2 seeds per pool)
      "8, 2, 4",   // 4 seeds in 2 pools (2 seeds per pool)
      "24, 8, 8",  // 8 seeds in 8 pools (1 seed per pool)
  })
  void testSeedDistributionInPools(int totalTeams, int nbPools, int nbSeeds) {
    // Given
    GroupPhase groupPhase = new GroupPhase(nbPools, totalTeams / nbPools, 1);
    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(totalTeams / nbPools)
                                              .build();

    List<Round> rounds     = groupPhase.initialize(config);
    Round       groupRound = rounds.get(0);

    List<PlayerPair> teams = TestFixtures.createPlayerPairs(totalTeams);
    // Set seeds for first nbSeeds teams
    for (int i = 0; i < nbSeeds && i < teams.size(); i++) {
      teams.get(i).setSeed(i + 1);
    }

    // When
    groupPhase.placeSeedTeams(groupRound, teams);

    // Only pass non-seeded teams to placeRemainingTeamsRandomly
    List<PlayerPair> nonSeededTeams = teams.stream()
                                           .filter(pair -> pair.getSeed() == 0)
                                           .toList();
    groupPhase.placeRemainingTeamsRandomly(groupRound, nonSeededTeams);

    // Then
    // Verify seeds are distributed evenly across pools
    int expectedSeedsPerPool = nbSeeds / nbPools;
    int poolsWithExtraSeed   = nbSeeds % nbPools;

    int poolsChecked = 0;
    for (Pool pool : groupRound.getPools()) {
      long seedsInPool = pool.getPairs().stream()
                             .filter(pair -> pair.getSeed() > 0 && pair.getSeed() <= nbSeeds)
                             .count();

      int expectedForThisPool = expectedSeedsPerPool;
      if (poolsChecked < poolsWithExtraSeed) {
        expectedForThisPool++;
      }

      assertEquals(expectedForThisPool, seedsInPool,
                   "Pool " + poolsChecked + " should have " + expectedForThisPool + " seeds");
      poolsChecked++;
    }

    // Verify no pool has multiple teams with the same seed number
    for (Pool pool : groupRound.getPools()) {
      Set<Integer> seedsInPool = pool.getPairs().stream()
                                     .map(PlayerPair::getSeed)
                                     .filter(seed -> seed > 0 && seed <= nbSeeds)
                                     .collect(Collectors.toSet());

      long seedsCount = pool.getPairs().stream()
                            .filter(pair -> pair.getSeed() > 0 && pair.getSeed() <= nbSeeds)
                            .count();

      assertEquals(seedsCount, seedsInPool.size(),
                   "Pool should not have duplicate seed numbers");
    }
  }

  @Test
  void testGroupGamesGeneration() {
    // Given
    int        nbPools        = 4;
    int        nbPairsPerPool = 4;
    GroupPhase groupPhase     = new GroupPhase(nbPools, nbPairsPerPool, 2);

    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(nbPools)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .build();

    List<Round> rounds     = groupPhase.initialize(config);
    Round       groupRound = rounds.get(0);

    List<PlayerPair> teams = TestFixtures.createPlayerPairs(16);

    // When
    groupPhase.placeRemainingTeamsRandomly(groupRound, teams);

    // Then
    // Each pool with 4 teams should have 6 games (round-robin: C(4,2) = 6)
    int expectedGamesPerPool = nbPairsPerPool * (nbPairsPerPool - 1) / 2;

    // Since Pool doesn't have getGames(), we'll check the round's games instead
    // In group phase, games should be organized by pools
    assertNotNull(groupRound.getGames(), "Round should have games");

    int totalExpectedGames = nbPools * expectedGamesPerPool;
    assertEquals(totalExpectedGames, groupRound.getGames().size(),
                 "Round should have correct total number of round-robin games");

    // Verify no team plays against itself and all games have valid teams
    for (Game game : groupRound.getGames()) {
      assertNotNull(game.getTeamA(), "Game should have team A");
      assertNotNull(game.getTeamB(), "Game should have team B");
      assertNotEquals(game.getTeamA(), game.getTeamB(), "Team cannot play against itself");
    }
  }

  @Test
  void testValidateGroupConfiguration() {
    // Given
    Tournament tournament = new Tournament();
    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(4)
                                              .nbPairsPerPool(3)
                                              .nbQualifiedByPool(1)
                                              .build();
    tournament.setConfig(config);

    GroupPhase groupPhase = new GroupPhase(4, 3, 1);

    // When
    List<String> errors = groupPhase.validate(tournament);

    // Then
    assertTrue(errors.isEmpty(), "Valid configuration should have no errors");
  }

  @Test
  void testInvalidConfigurations() {
    // Test case 1: More qualified than teams per pool
    Tournament tournament1 = new Tournament();
    TournamentConfig config1 = TournamentConfig.builder()
                                               .nbPools(4)
                                               .nbPairsPerPool(2)
                                               .nbQualifiedByPool(3) // More than pairs per pool
                                               .build();
    tournament1.setConfig(config1);

    GroupPhase   groupPhase1 = new GroupPhase(4, 2, 3);
    List<String> errors1     = groupPhase1.validate(tournament1);

    assertFalse(errors1.isEmpty(), "Should have validation errors for invalid qualification count");

    // Test case 2: Total qualified not power of 2
    Tournament tournament2 = new Tournament();
    TournamentConfig config2 = TournamentConfig.builder()
                                               .nbPools(3)
                                               .nbPairsPerPool(4)
                                               .nbQualifiedByPool(1) // 3 total qualified (not power of 2)
                                               .build();
    tournament2.setConfig(config2);

    GroupPhase   groupPhase2 = new GroupPhase(3, 4, 1);
    List<String> errors2     = groupPhase2.validate(tournament2);

    assertFalse(errors2.isEmpty(), "Should have validation errors for non-power-of-2 qualified count");
  }

  @Test
  void testGetInitialStage() {
    // Given
    GroupPhase groupPhase = new GroupPhase(4, 4, 2);

    // When
    Stage initialStage = groupPhase.getInitialStage();

    // Then
    assertEquals(Stage.GROUPS, initialStage, "Initial stage should be GROUPS");
  }

  @Test
  void testEmptyTeamsList() {
    // Given
    GroupPhase groupPhase = new GroupPhase(4, 3, 1);
    TournamentConfig config = TournamentConfig.builder()
                                              .nbPools(4)
                                              .nbPairsPerPool(3)
                                              .build();

    List<Round> rounds     = groupPhase.initialize(config);
    Round       groupRound = rounds.get(0);

    // When
    groupPhase.placeRemainingTeamsRandomly(groupRound, List.of());

    // Then
    for (Pool pool : groupRound.getPools()) {
      assertTrue(pool.getPairs().isEmpty(), "Pools should be empty when no teams provided");
    }
  }
}
