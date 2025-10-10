package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "bali.redouane@gmail.com")
public class PoolTest {

  private List<PlayerPair> defaultPairs;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
    defaultPairs = TestFixtures.createPlayerPairs(3); // three pairs: [0],[1],[2]
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
    games.add(buildGame(score1, defaultPairs.getFirst(), defaultPairs.get(1)));
    games.add(buildGame(score2, defaultPairs.get(1), defaultPairs.get(2)));
    games.add(buildGame(score3, defaultPairs.getFirst(), defaultPairs.get(2)));

    Round round = new Round(Stage.GROUPS);
    games.forEach(round::addGame);
    round.addPool(pool);

    List<PlayerPair> expectedRanking = new ArrayList<>();
    for (String token : expectedRankingStr.split("\\|")) {
      switch (token) {
        case "A:B" -> expectedRanking.add(defaultPairs.getFirst());
        case "C:D" -> expectedRanking.add(defaultPairs.get(1));
        case "E:F" -> expectedRanking.add(defaultPairs.get(2));
        default -> throw new IllegalArgumentException("Unexpected token in expectedRankingStr: " + token);
      }
    }
    List<PoolRankingDetails> ranking = Pool.computeRanking(pool, round.getGames());
    assertEquals(expectedRanking, ranking.stream().map(PoolRankingDetails::getPlayerPair).collect(Collectors.toList()));

    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.getRounds().clear();
    tournament.getRounds().add(round);
    List<PoolRanking> poolRankings = Pool.getGroupRankings(tournament);
    assertEquals(1, poolRankings.size());
    assertEquals(defaultPairs.size(), poolRankings.getFirst().getDetails().size());
  }
}
