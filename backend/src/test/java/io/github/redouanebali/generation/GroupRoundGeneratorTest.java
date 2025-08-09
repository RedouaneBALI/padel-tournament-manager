package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GroupRoundGeneratorTest {

  private GroupRoundGenerator generator;

  @ParameterizedTest
  @CsvSource({
      "6,2,3,6",   // 2 groups of 3 => 3 games per group = 3*2/2 = 3 games per group, total = 6
      "12,3,4,18", // 3 groups of 4 => 6 games per group = 4*3/2 = 6, total = 18
      "16,4,4,24", // 4 groups of 4 => 6 games per group, total = 24
      "20,5,4,30", // 5 groups of 4 => 6 games per group, total = 30
      "18,6,3,18"  // 6 groups of 3 => 3 games per group, total = 18
  })
  public void checkManualPoolGeneration(int nbPairs, int expectedGroups, int expectedPairsPerGroup, int expectedNbGames) {
    generator = new GroupRoundGenerator(0, expectedGroups, expectedPairsPerGroup);
    List<PlayerPair> pairs = createPairs(nbPairs);
    Round            round = generator.generateManualRound(pairs);

    assertEquals(expectedGroups, round.getPools().size());
    int index = 0;
    for (Pool pool : round.getPools()) {
      assertEquals(expectedPairsPerGroup, pool.getPairs().size());
      for (PlayerPair pair : pool.getPairs()) {
        assertEquals(pairs.get(index++), pair, "Player pair order mismatch at index " + (index - 1));
      }
    }

    assertEquals(expectedNbGames, round.getGames().size());
  }

  @ParameterizedTest
  @CsvSource({
      "3,3,3,'1|2|3'",
      "6,3,3,'1-6|2-5|3-4'",
      "4,4,3,'1|2|3|4'",
      "4,4,4,'1|2|3|4'",
      "8,4,3,'1-8|2-7|3-6|4-5'",
      "8,4,4,'1-8|2-7|3-6|4-5'",
      "2,2,5,'1|2|'",
      "4,2,5,'1-4|2-3|'",
  })
  public void checkAlgorithmicPoolGeneration(int nbSeeds, int nbPools, int nbTeamPerPool, String expectedSeedsStr) {
    int totalPairs = nbPools * nbTeamPerPool;
    generator = new GroupRoundGenerator(nbSeeds, nbPools, nbTeamPerPool);
    List<PlayerPair> pairs = createPairs(totalPairs);
    Round            round = generator.generateAlgorithmicRound(pairs);

    assertEquals(nbPools, round.getPools().size());

    List<Pool> pools         = new ArrayList<>(round.getPools());
    String[]   expectedPools = expectedSeedsStr.split("\\|");
    for (int i = 0; i < expectedPools.length; i++) {
      String[]      seedStrings      = expectedPools[i].split("-");
      List<Integer> expectedSeedList = new ArrayList<>();
      for (String s : seedStrings) {
        expectedSeedList.add(Integer.parseInt(s));
      }
      List<PlayerPair> poolPairs   = new ArrayList<>(pools.get(i).getPairs());
      List<Integer>    actualSeeds = poolPairs.stream().map(PlayerPair::getSeed).toList();
      for (int expectedSeed : expectedSeedList) {
        assert actualSeeds.contains(expectedSeed) : "Expected seed " + expectedSeed + " in pool " + i + ", but found seeds " + actualSeeds;
      }
      assertEquals(nbTeamPerPool, poolPairs.size(), "Expected " + nbTeamPerPool + " pairs in pool " + i);
    }
  }


  private List<PlayerPair> createPairs(int count) {
    List<PlayerPair> pairs = new ArrayList<>();
    IntStream.rangeClosed(1, count).forEach(seed -> {
      Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
      Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
      pairs.add(new PlayerPair(-1L, player1, player2, seed));
    });
    return pairs;
  }

  @ParameterizedTest
  @CsvSource({
      "2,3,6",   // 2 pools of 3 => 3 games per pool = 3*2/2 = 3 games per pool, total = 6
      "3,4,18",  // 3 pools of 4 => 6 games per pool = 4*3/2 = 6, total = 18
      "4,4,24",  // 4 pools of 4 => 6 games per pool = 4*3/2 = 6, total = 24
      "5,4,30",  // 5 pools of 4 => 6 games per pool = 4*3/2 = 6, total = 30
      "6,3,18"   // 6 pools of 3 => 3 games per pool = 3*2/2 = 3, total = 18
  })
  public void testInitRoundsAndGames(int nbPools, int nbPairsPerPool, int expectedNbGames) {
    io.github.redouanebali.model.Tournament tournament = new io.github.redouanebali.model.Tournament();
    tournament.setNbPools(nbPools);
    tournament.setNbPairsPerPool(nbPairsPerPool);

    GroupRoundGenerator generator = new GroupRoundGenerator(0, nbPools, nbPairsPerPool);
    List<Round>         rounds    = generator.initRoundsAndGames(tournament);
    Round               round     = rounds.iterator().next();

    assertEquals(expectedNbGames, round.getGames().size());
  }

  @ParameterizedTest
  @CsvSource({
      // nbPools, nbPairsPerPool, nbQualifiedByPool, expectedFinalRoundsCount, expectedFirstFinalRoundMatches
      "4,4,1,2,2", // 4 pools of 4, 1 qualified -> 4 teams => Semi (2) + Final (1)
      "4,4,2,3,4", // 4 pools of 4, 2 qualified -> 8 teams => Quarter (4) + Semi (2) + Final (1)
      "4,4,4,4,8", // 4 pools of 4, 4 qualified -> 16 teams => R16 (8) + QF (4) + SF (2) + F (1)
      "4,3,1,2,2", // 4 pools of 3, 1 qualified -> 4 teams => Semi (2) + Final (1)
      "4,3,2,3,4",  // 4 pools of 3, 2 qualified -> 8 teams => Quarter (4) + Semi (2) + Final (1)
      "2,4,1,1,1"  // 2 pools of 4, 1 qualified -> 2 teams => Final (1)
  })
  public void testFinalBracketCreation(int nbPools, int nbPairsPerPool, int nbQualifiedByPool,
                                       int expectedFinalRoundsCount, int expectedFirstFinalRoundMatches) {
    Tournament tournament = new Tournament();
    tournament.setNbPools(nbPools);
    tournament.setNbPairsPerPool(nbPairsPerPool);
    tournament.setNbQualifiedByPool(nbQualifiedByPool);

    GroupRoundGenerator generator = new GroupRoundGenerator(0, nbPools, nbPairsPerPool);
    List<Round>         rounds    = generator.initRoundsAndGames(tournament);

    // There must always be 1 group phase round first
    assertEquals(1 + expectedFinalRoundsCount, rounds.size(),
                 "Unexpected total number of rounds (group + finals)");

    Round groupRound = rounds.get(0);
    assertEquals(nbPools * (nbPairsPerPool * (nbPairsPerPool - 1) / 2), groupRound.getGames().size(),
                 "Incorrect number of group-phase games");

    // Check first finals round exists and has the expected number of matches
    Round firstFinalsRound = rounds.get(1);
    assertEquals(expectedFirstFinalRoundMatches, firstFinalsRound.getGames().size(),
                 "Incorrect number of matches in the first finals round");

    // The last finals round must always be the Final with exactly 1 match
    Round lastRound = rounds.get(rounds.size() - 1);
    assertEquals(1, lastRound.getGames().size(), "The last round should be the Final with exactly 1 match");
  }

  @ParameterizedTest
  @CsvSource({
      // nbPools, nbPairsPerPool, nbQualifiedByPool, expectedTeamsInNextRound, expectedMatchesInNextRound
      "4,4,1,4,2",  // 4 poules de 4 avec 1 qualifié -> 4 teams -> 2 matches (demi-finales)
      "4,4,2,8,4",  // 4 poules de 4 avec 2 qualifiés -> 8 teams -> 4 matches (quarts)
      "2,4,1,2,1",  // 2 poules de 4 avec 1 qualifié -> 2 teams -> 1 match (finale)
      "2,4,2,4,2",  // 2 poules de 4 avec 2 qualifiés -> 4 teams -> 2 matches (demi-finales)
      "2,3,1,2,1",  // 2 poules de 3 avec 1 qualifié -> 2 teams -> 1 match (finale)
      "8,3,1,8,4"   // 8 poules de 3 avec 1 qualifié -> 8 teams -> 4 matches (huitièmes)
  })
  void testPropagateWinners_param(int nbPools, int nbPairsPerPool, int nbQualifiedByPool,
                                  int expectedTeamsInNextRound, int expectedMatchesInNextRound) {
    // Arrange
    Tournament tournament = new Tournament();
    tournament.setNbPools(nbPools);
    tournament.setNbPairsPerPool(nbPairsPerPool);
    tournament.setNbQualifiedByPool(nbQualifiedByPool);

    Round groups = new Round();
    groups.setStage(io.github.redouanebali.model.Stage.GROUPS);

    // Pools A,B,C... ; insertion dans l'ordre = classement (1er, 2e, ...)
    for (int p = 0; p < nbPools; p++) {
      Pool pool = new Pool();
      pool.setName(String.valueOf((char) ('A' + p)));
      for (int r = 1; r <= nbPairsPerPool; r++) {
        pool.addPair(makePair(p, r));
      }
      groups.getPools().add(pool);
    }
    tournament.getRounds().add(groups);

    GroupRoundGenerator gen = new GroupRoundGenerator(0, nbPools, nbPairsPerPool);

    // Act
    gen.propagateWinners(tournament);

    // Assert: stage & count
    io.github.redouanebali.model.Stage nextStage =
        io.github.redouanebali.model.Stage.fromNbTeams(expectedTeamsInNextRound);

    Round nextRound = tournament.getRounds().stream()
                                .filter(r -> r.getStage() == nextStage)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Next finals round not found for stage: " + nextStage));

    assertEquals(expectedMatchesInNextRound, nextRound.getGames().size(),
                 "Unexpected number of matches in the next finals round");

    // Assert: affiches exactes (ordre des fixtures) ; TeamA/TeamB peut être inversé
    List<List<PlayerPair>> expected = expectedFixtures(groups, nbQualifiedByPool);
    for (int i = 0; i < expected.size(); i++) {
      List<PlayerPair> fx = expected.get(i);
      Game             g  = nextRound.getGames().get(i);
      assertTrue(gameContainsBoth(g, fx.get(0), fx.get(1)),
                 "Mismatch at game index " + i + ": expected (" + fx.get(0) + ") vs (" + fx.get(1) + ") but got ("
                 + g.getTeamA() + ", " + g.getTeamB() + ")");
    }
  }

  // Expected fixtures in order: (A,B), (C,D), ...
// If 2 qualified: A1-B2, B1-A2, C1-D2, D1-C2, ...
// If 1 qualified: A1-B1, C1-D1, ...
  private List<List<PlayerPair>> expectedFixtures(Round groups, int nbQualifiedByPool) {
    List<Pool> pools = new ArrayList<>(groups.getPools());
    pools.sort(java.util.Comparator.comparing(Pool::getName));
    List<List<PlayerPair>> out = new ArrayList<>();
    for (int i = 0; i + 1 < pools.size(); i += 2) {
      List<PlayerPair> x = new ArrayList<>(pools.get(i).getPairs());
      List<PlayerPair> y = new ArrayList<>(pools.get(i + 1).getPairs());
      if (nbQualifiedByPool == 1) {
        out.add(List.of(x.get(0), y.get(0)));
      } else { // == 2
        out.add(List.of(x.get(0), y.get(1))); // A1 vs B2
        out.add(List.of(y.get(0), x.get(1))); // B1 vs A2
      }
    }
    return out;
  }

  private boolean gameContainsBoth(Game g, PlayerPair p, PlayerPair q) {
    return (g.getTeamA() == p && g.getTeamB() == q) || (g.getTeamA() == q && g.getTeamB() == p);
  }

  // --- helpers ---
  private PlayerPair makePair(int poolIndex, int rankInPool) {
    // id/seed déterministes pour debug facile
    int    seed = (poolIndex + 1) * 100 + rankInPool;
    Player p1   = new Player((long) seed, "P" + seed + "A", seed, 0, 1990);
    Player p2   = new Player((long) seed + 10000, "P" + seed + "B", seed, 0, 1990);
    return new PlayerPair(-1L, p1, p2, seed);
  }

}