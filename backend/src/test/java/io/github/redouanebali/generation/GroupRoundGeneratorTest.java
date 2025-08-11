package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
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
    Tournament tournament = new Tournament();
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
    groups.setStage(Stage.GROUPS);

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
    tournament.setNbPools(2);
    tournament.setNbPairsPerPool(3);
    tournament.setNbQualifiedByPool(1);

    Round groups = new Round();
    groups.setStage(Stage.GROUPS);

    // Format: 2 sets gagnants, 6 jeux par set, pas de super tie-break
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setPointsPerSet(6);
    format.setSuperTieBreakInFinalSet(false);

    // --- Paires (noms identiques à la capture) ---
    // Poule A
    PlayerPair a1 = new PlayerPair(-1L,
                                   new Player(1L, "Aymen BENNANI", 1, 0, 1990),
                                   new Player(101L, "Yassine CHRAIBI", 1, 0, 1990), 1);
    PlayerPair a3 = new PlayerPair(-1L,
                                   new Player(3L, "Amine JABRI", 3, 0, 1990),
                                   new Player(103L, "Samy LAKHDAR", 3, 0, 1990), 3);
    PlayerPair a4 = new PlayerPair(-1L,
                                   new Player(4L, "Tarik MOUSSAOUI", 4, 0, 1990),
                                   new Player(104L, "Karim BENALI", 4, 0, 1990), 4);

    Pool poolA = new Pool();
    poolA.setName("A");
    poolA.addPair(a1);
    poolA.addPair(a3);
    poolA.addPair(a4);

    // Poule B
    PlayerPair b2 = new PlayerPair(-1L,
                                   new Player(2L, "Anass EL GHALI", 2, 0, 1990),
                                   new Player(102L, "Hicham ZOUAOUI", 2, 0, 1990), 2);
    PlayerPair b5 = new PlayerPair(-1L,
                                   new Player(5L, "Mehdi BOUKHARI", 5, 0, 1990),
                                   new Player(105L, "Hamza ALAOUI", 5, 0, 1990), 5);
    PlayerPair b6 = new PlayerPair(-1L,
                                   new Player(6L, "Nabil FARES", 6, 0, 1990),
                                   new Player(106L, "Ali JOUDI", 6, 0, 1990), 6);

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
    Score s_a_g1 = new Score();
    s_a_g1.addSetScore(6, 4);
    s_a_g1.addSetScore(6, 2);
    a_g1.setScore(s_a_g1);

    Game a_g2 = new Game(format);
    a_g2.setPool(poolA);
    a_g2.setTeamA(a1);
    a_g2.setTeamB(a4);
    Score s_a_g2 = new Score();
    s_a_g2.addSetScore(6, 0);
    s_a_g2.addSetScore(6, 2);
    a_g2.setScore(s_a_g2);

    Game a_g3 = new Game(format);
    a_g3.setPool(poolA);
    a_g3.setTeamA(a3);
    a_g3.setTeamB(a4);
    Score s_a_g3 = new Score();
    s_a_g3.addSetScore(4, 6);
    s_a_g3.addSetScore(4, 6);
    a_g3.setScore(s_a_g3);

    // Poule B
    Game b_g1 = new Game(format);
    b_g1.setPool(poolB);
    b_g1.setTeamA(b2);
    b_g1.setTeamB(b5);
    Score s_b_g1 = new Score();
    s_b_g1.addSetScore(3, 6);
    s_b_g1.addSetScore(2, 6);
    b_g1.setScore(s_b_g1);

    Game b_g2 = new Game(format);
    b_g2.setPool(poolB);
    b_g2.setTeamA(b2);
    b_g2.setTeamB(b6);
    Score s_b_g2 = new Score();
    s_b_g2.addSetScore(4, 6);
    s_b_g2.addSetScore(5, 7);
    b_g2.setScore(s_b_g2);

    Game b_g3 = new Game(format);
    b_g3.setPool(poolB);
    b_g3.setTeamA(b5);
    b_g3.setTeamB(b6);
    Score s_b_g3 = new Score();
    s_b_g3.addSetScore(5, 7);
    s_b_g3.addSetScore(3, 6);
    b_g3.setScore(s_b_g3);

    groups.addGames(java.util.Arrays.asList(a_g1, a_g2, a_g3, b_g1, b_g2, b_g3));
    tournament.getRounds().add(groups);

    // --- Act: recalc classements & propager en finale ---
    GroupRoundGenerator gen = new GroupRoundGenerator(0, 2, 3);
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
    assertTrue(gameContainsBoth(finalGame, a1, b6), "Expected Final to be A1 vs B1");
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

