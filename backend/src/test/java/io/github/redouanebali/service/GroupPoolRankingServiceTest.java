package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.PoolRanking;
import io.github.redouanebali.model.PoolRankingDetails;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GroupPoolRankingServiceTest {

  private List<PlayerPair> defaultPairs;

  @BeforeEach
  void setUp() {
    defaultPairs = getDefaultPlayerPairs();
  }

  private List<PlayerPair> getDefaultPlayerPairs() {
    return List.of(
        new PlayerPair(null, new Player("A"), new Player("B"), 0),
        new PlayerPair(null, new Player("C"), new Player("D"), 0),
        new PlayerPair(null, new Player("E"), new Player("F"), 0)
    );
  }

  private Game buildGame(String scoreStr, PlayerPair teamA, PlayerPair teamB) {
    Game game = new Game();
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setFormat(new MatchFormat(1L, 1, 6, false, false));
    Score score = Score.fromString(scoreStr);
    game.setScore(score);

    return game;
  }

  @ParameterizedTest
  @CsvSource({
      "'6-0,6-0', '6-0,6-0', '6-3,6-3', 'A:B|C:D|E:F'",
      "'6-2,6-2', '0-0', '6-4,6-1', 'A:B|E:F|C:D'", // A:B 2pts, C:D 0pts (-8), E:F: 0pts (-7)
      "'6-4,6-4', '6-2,6-1', '0-6,0-6', 'C:D|E:F|A:B'", // A:B 1pt (-8), C:D 1pt (+5), E:F: 1pt (+3)
      "'7-5,7-6', '6-0,6-0', '4-6,5-7', 'C:D|A:B|E:F'", // A:B 1pt (-1), C:D 1pt (+9), E:F : 0pt (-9)
      "'6-0,6-0', '6-0,6-0', '6-0,6-0', 'A:B|C:D|E:F'",
      "'6-1,6-1', '2-6,2-6', '6-3,6-3', 'A:B|E:F|C:D'",
      "'6-0,6-0', '0-6,0-6', '6-4,6-4', 'A:B|E:F|C:D'",
      "'0-6,0-6', '3-6,3-6', '6-4,6-4', 'C:D|E:F|A:B'"
  })
  void testComputeRankingParameterized(String score1, String score2, String score3, String expectedRankingStr) {
    Pool pool = new Pool("A", defaultPairs);

    List<Game> games = new ArrayList<>();
    games.add(buildGame(score1, defaultPairs.get(0), defaultPairs.get(1)));
    games.add(buildGame(score2, defaultPairs.get(1), defaultPairs.get(2)));
    games.add(buildGame(score3, defaultPairs.get(0), defaultPairs.get(2)));

    Round round = new Round(Stage.GROUPS);
    games.forEach(round::addGame);
    round.addPool(pool);

    List<PlayerPair> expectedRanking = new ArrayList<>();
    Arrays.stream(expectedRankingStr.split("\\|")).forEach(p -> {
      String[] names = p.split(":");
      expectedRanking.add(new PlayerPair(null, new Player(names[0]), new Player(names[1]), 0));
    });
    List<PoolRankingDetails> ranking = GroupRankingService.computeRanking(pool, round.getGames());
    ranking.forEach(System.out::println);
    System.out.println();
    assertEquals(expectedRanking, ranking.stream().map(PoolRankingDetails::getPlayerPair).collect(Collectors.toList()));

    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setRounds(Set.of(round));
    List<PoolRanking> poolRankings = GroupRankingService.getGroupRankings(tournament);
    assertEquals(1, poolRankings.size());
    assertEquals(defaultPairs.size(), poolRankings.getFirst().getDetails().size());
  }
}
