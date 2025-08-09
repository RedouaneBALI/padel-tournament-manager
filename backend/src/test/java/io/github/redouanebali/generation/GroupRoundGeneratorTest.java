package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}