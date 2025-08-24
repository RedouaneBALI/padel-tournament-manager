package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.util.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

// edit this file please
public class GroupRoundGeneratorTest {

  private GroupRoundGenerator generator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
  }

  @ParameterizedTest
  @CsvSource({
      "6,2,3,6",   // 2 groups of 3 => 3 games per group = 3*2/2 = 3 games per group, total = 6
      "12,3,4,18", // 3 groups of 4 => 6 games per group = 4*3/2 = 6, total = 18
      "16,4,4,24", // 4 groups of 4 => 6 games per group, total = 24
      "20,5,4,30", // 5 groups of 4 => 6 games per group, total = 30
      "18,6,3,18"  // 6 groups of 3 => 3 games per group, total = 18
  })
  public void checkManualPoolGeneration(int nbPairs, int expectedGroups, int expectedPairsPerGroup, int expectedNbGames) {
    generator = new GroupRoundGenerator(0, expectedGroups, expectedPairsPerGroup, 1); // @todo to remove
    List<PlayerPair> pairs  = TestFixtures.createPairs(nbPairs);
    List<Round>      rounds = generator.generateManualRounds(pairs);
    Round            round  = rounds.get(0);

    assertEquals(expectedGroups, round.getPools().size());
    int index = 0;
    for (Pool pool : round.getPools()) {
      assertEquals(expectedPairsPerGroup, pool.getPairs().size());
      for (PlayerPair pair : pool.getPairs()) {
        assertEquals(pairs.get(index++), pair, "Player pair order mismatch at index " + (index - 1));
      }
    }

    assertEquals(TestFixtures.totalGroupGames(expectedGroups, expectedPairsPerGroup), round.getGames().size());
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
    generator = new GroupRoundGenerator(nbSeeds, nbPools, nbTeamPerPool, 1);
    List<PlayerPair> pairs  = TestFixtures.createPairs(totalPairs);
    List<Round>      rounds = generator.generateAlgorithmicRounds(pairs);
    Round            round  = rounds.get(0);

    assertEquals(nbPools, round.getPools().size());

    List<Pool> pools         = TestFixtures.sortedPoolsByName(round.getPools());
    String[]   expectedPools = expectedSeedsStr.split("\\|");
    for (int i = 0; i < expectedPools.length; i++) {
      String[]      seedStrings      = expectedPools[i].split("-");
      List<Integer> expectedSeedList = new java.util.ArrayList<>();
      for (String s : seedStrings) {
        expectedSeedList.add(Integer.parseInt(s));
      }
      List<PlayerPair> poolPairs   = TestFixtures.sortedPairsBySeed(pools.get(i).getPairs());
      List<Integer>    actualSeeds = poolPairs.stream().map(PlayerPair::getSeed).toList();
      for (int expectedSeed : expectedSeedList) {
        assert actualSeeds.contains(expectedSeed) : "Expected seed " + expectedSeed + " in pool " + i + ", but found seeds " + actualSeeds;
      }
      assertEquals(nbTeamPerPool, poolPairs.size(), "Expected " + nbTeamPerPool + " pairs in pool " + i);
    }
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

    Round groups = new Round();
    groups.setStage(Stage.GROUPS);

    // Pools A,B,C... ; insertion dans l'ordre = classement (1er, 2e, ...)
    for (int p = 0; p < nbPools; p++) {
      Pool pool = new Pool();
      pool.setName(String.valueOf((char) ('A' + p)));
      for (int r = 1; r <= nbPairsPerPool; r++) {
        pool.addPair(TestFixtures.makePairFromPoolRank(p, r));
      }
      groups.getPools().add(pool);
    }
    tournament.getRounds().add(groups);

    GroupRoundGenerator gen = new GroupRoundGenerator(0, nbPools, nbPairsPerPool, nbQualifiedByPool);

    // Act
    gen.propagateWinners(tournament);

    // Assert: stage & count
    Stage nextStage = Stage.fromNbTeams(expectedTeamsInNextRound);

    Round nextRound = tournament.getRounds().stream()
                                .filter(r -> r.getStage() == nextStage)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Next finals round not found for stage: " + nextStage));

    assertEquals(expectedMatchesInNextRound, nextRound.getGames().size(),
                 "Unexpected number of matches in the next finals round");
  }

  @Test
  public void testGroupRankingAndFinal_MatchesScreenshot() {
    // --- Arrange: tournoi & format ---
    Tournament tournament = new Tournament();

    Round groups = new Round();
    groups.setStage(Stage.GROUPS);

    // Format: 2 sets gagnants, 6 jeux par set, pas de super tie-break
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);
    format.setSuperTieBreakInFinalSet(false);

    // --- Paires (fixtures) ---
    List<PlayerPair> pairs = TestFixtures.createPairs(6);
    PlayerPair       a1    = pairs.get(0); // seed 1
    PlayerPair       a3    = pairs.get(2); // seed 3
    PlayerPair       a4    = pairs.get(3); // seed 4

    Pool poolA = new Pool();
    poolA.setName("A");
    poolA.addPair(a1);
    poolA.addPair(a3);
    poolA.addPair(a4);

    PlayerPair b2 = pairs.get(1); // seed 2
    PlayerPair b5 = pairs.get(4); // seed 5
    PlayerPair b6 = pairs.get(5); // seed 6

    Pool poolB = new Pool();
    poolB.setName("B");
    poolB.addPair(b2);
    poolB.addPair(b5);
    poolB.addPair(b6);

    groups.getPools().add(poolA);
    groups.getPools().add(poolB);

    // --- Matchs & scores (comme sur la capture) ---
    // Poule A
    Game a_g1 = new Game(format);
    a_g1.setPool(poolA);
    a_g1.setTeamA(a1);
    a_g1.setTeamB(a3);
    a_g1.setScore(TestFixtures.createScoreWithWinner(a_g1, a1));

    Game a_g2 = new Game(format);
    a_g2.setPool(poolA);
    a_g2.setTeamA(a1);
    a_g2.setTeamB(a4);
    a_g2.setScore(TestFixtures.createScoreWithWinner(a_g2, a1));

    Game a_g3 = new Game(format);
    a_g3.setPool(poolA);
    a_g3.setTeamA(a3);
    a_g3.setTeamB(a4);
    a_g3.setScore(TestFixtures.createScoreWithWinner(a_g3, a4));

    // Poule B
    Game b_g1 = new Game(format);
    b_g1.setPool(poolB);
    b_g1.setTeamA(b2);
    b_g1.setTeamB(b5);
    b_g1.setScore(TestFixtures.createScoreWithWinner(b_g1, b5));

    Game b_g2 = new Game(format);
    b_g2.setPool(poolB);
    b_g2.setTeamA(b2);
    b_g2.setTeamB(b6);
    b_g2.setScore(TestFixtures.createScoreWithWinner(b_g2, b6));

    Game b_g3 = new Game(format);
    b_g3.setPool(poolB);
    b_g3.setTeamA(b5);
    b_g3.setTeamB(b6);
    b_g3.setScore(TestFixtures.createScoreWithWinner(b_g3, b6));

    groups.addGames(java.util.Arrays.asList(a_g1, a_g2, a_g3, b_g1, b_g2, b_g3));
    tournament.getRounds().add(groups);

    // --- Act: recalc classements & propager en finale ---
    GroupRoundGenerator gen = new GroupRoundGenerator(0, 2, 3, 1);
    gen.propagateWinners(tournament);

    // --- Assert: classements attendus ---
    var aDetails = poolA.getPoolRanking().getDetails();
    assertEquals(3, aDetails.size(), "Pool A ranking size");
    assertEquals(a1, aDetails.get(0).getPlayerPair(), "Pool A: A1 = BENNANI/CHRAIBI");
    assertEquals(a4, aDetails.get(1).getPlayerPair(), "Pool A: A2 = MOUSSAOUI/BENALI");
    assertEquals(a3, aDetails.get(2).getPlayerPair(), "Pool A: A3 = JABRI/LAKHDAR");

    var bDetails = poolB.getPoolRanking().getDetails();
    assertEquals(3, bDetails.size(), "Pool B ranking size");
    assertEquals(b6, bDetails.get(0).getPlayerPair(), "Pool B: B1 = FARES/JOUDI");
    assertEquals(b5, bDetails.get(1).getPlayerPair(), "Pool B: B2 = BOUKHARI/ALAOUI");
    assertEquals(b2, bDetails.get(2).getPlayerPair(), "Pool B: B3 = EL GHALI/ZOUAOUI");

    // --- Assert: Finale = A1 vs B1 ---
    Stage finalStage = Stage.fromNbTeams(2);
    Round finalRound = tournament.getRounds().stream()
                                 .filter(r -> r.getStage() == finalStage)
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Final round not found"));

    assertEquals(1, finalRound.getGames().size(), "Final round must have 1 match");
    Game finalGame = finalRound.getGames().get(0);
    assertTrue(TestFixtures.gameContainsBoth(finalGame, a1, b6), "Expected Final to be A1 vs B1");
  }


  @Test
  public void testAlgorithmicSeeding_distributesTopSeedsAcrossPools() {
    int nbPools      = 4;
    int teamsPerPool = 3;
    int nbSeeds      = nbPools;

    GroupRoundGenerator generator = new GroupRoundGenerator(nbSeeds, nbPools, teamsPerPool, 1);
    List<PlayerPair>    pairs     = TestFixtures.createPairs(nbPools * teamsPerPool);
    List<Round>         rounds    = generator.generateAlgorithmicRounds(pairs);
    Round               round     = rounds.get(0);

    List<Pool> pools = TestFixtures.sortedPoolsByName(round.getPools());

    assertEquals(nbPools, pools.size(), "Unexpected number of pools");

    assertAll("Each pool should contain exactly one top seed",
              java.util.stream.IntStream.range(0, nbPools)
                                        .mapToObj(i -> () -> {
                                          Pool pool = pools.get(i);
                                          long topSeedsInPool = pool.getPairs().stream()
                                                                    .filter(pp -> pp.getSeed() >= 1 && pp.getSeed() <= nbPools)
                                                                    .count();
                                          assertEquals(1, topSeedsInPool, "Pool " + pool.getName() + " should contain exactly one top seed");
                                        })
    );
  }

  private void finishAllGroupGamesDeterministically(Round groupsRound) {
    for (Game g : groupsRound.getGames()) {
      PlayerPair a = g.getTeamA();
      PlayerPair b = g.getTeamB();
      if (a == null || b == null) {
        continue;
      }
      PlayerPair winner = (a.getSeed() <= b.getSeed()) ? a : b;
      g.setScore(TestFixtures.createScoreWithWinner(g, winner));
    }
  }

}
