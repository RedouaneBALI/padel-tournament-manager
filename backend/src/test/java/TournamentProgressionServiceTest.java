import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.service.TournamentProgressionService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TournamentProgressionServiceTest {

  private final TournamentProgressionService service = new TournamentProgressionService();

  @ParameterizedTest
  @CsvSource({
      // Format : nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expected

      // --- 1 set gagnant, 6 jeux par set ---
      "1, 6, '6-3', false, true",
      "1, 6, '7-5', false, true",
      "1, 6, '7-6', false, true",
      "1, 6, '6-5', false, false",
      "1, 6, '5-7', false, true",
      "1, 6, '6-7', false, true",
      "1, 6, '5-5', false, false",
      // --- 1 set gagnant, 9 jeux par set ---
      "1, 9, '9-7', false, true",
      "1, 9, '8-6', false, false",
      // --- 1 set gagnant, 4 jeux par set (tie-break à 4-4) ---
      "1, 4, '4-2', false, true",
      "1, 4, '5-4', false, true",
      "1, 4, '3-4', false, false",
      "1, 4, '3-3', false, false",
      // --- 2 sets gagnants, 6 jeux par set ---
      "2, 6, '6-0,6-0', false, true",
      "2, 6, '6-4,6-3', false, true",
      "2, 6, '6-3,4-6', false, false",
      "2, 6, '6-4,3-6,6-2', false, true",
      "2, 6, '6-4,3-6,5-5', false, false",
      "2, 6, '6-4,4-6,6-5', false, false",
      "2, 6, '6-4,4-6,7-5', false, true",
      "2, 6, '7-6,6-4', false, true",
      "2, 6, '7-6,6-5', false, false",
      // --- 2 sets gagnants, 4 jeux par set ---
      "2, 4, '4-2,4-2', false, true",
      "2, 4, '4-2,2-4', false, false",
      "2, 4, '4-2,2-4,4-3', false, false",
      "2, 4, '4-2,2-4,3-3', false, false",
      "2, 4, '4-2,2-4,5-4', false, true",
      // --- Super tie-break en 3e set ---
      "2, 6, '6-4,4-6,10-8', true, true",
      "2, 6, '6-4,4-6,10-9', true, false",
      "2, 6, '6-4,4-6,8-10', true, true",
      "2, 6, '6-4,4-6,9-11', true, true",
      "2, 6, '6-4,4-6,11-10', true, false",
      "2, 6, '6-4,4-6,11-13', true, true",
      "2, 6, '6-4,4-6,11-9', true, true",
      "2, 6, '6-4,4-6,8-8', true, false"
  })
  void testIsGameFinished_withFormat(
      int numberOfSetsToWin,
      int pointsPerSet,
      String scoreString,
      boolean isSuperTieBreakInFinalSet,
      boolean expected
  ) {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(numberOfSetsToWin);
    format.setPointsPerSet(pointsPerSet);
    format.setSuperTieBreakInFinalSet(isSuperTieBreakInFinalSet);

    Game game = new Game();
    game.setFormat(format);
    game.setTeamA(new PlayerPair());
    game.setTeamB(new PlayerPair());

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    boolean result = service.isGameFinished(game);
    assertEquals(expected, result);
  }

  // ---------- Test getWinner ----------

  @ParameterizedTest
  @CsvSource({
      // Format : nbSetsToWin, ptsPerSet, scoreString, superTieBreakFinalSet, expectedWinner

      // Matchs terminés et non terminés, divers formats

      // 2 sets gagnants, 6 jeux par set, sans super tie-break
      "2, 6, '6-3,6-1', false, A",
      "2, 6, '4-6,3-6', false, B",
      "2, 6, '6-4,3-6,6-2', false, A",
      "2, 6, '6-3,5-5', false, NONE",

      // 1 set gagnant, 6 jeux par set
      "1, 6, '6-3', false, A",
      "1, 6, '5-7', false, B",

      // 1 set gagnant, 9 jeux par set
      "1, 9, '9-7', false, A",

      // 1 set gagnant, 4 jeux par set
      "1, 4, '3-4', false, NONE",
      "1, 4, '4-2', false, A",

      // 2 sets gagnants, 4 jeux par set
      "2, 4, '4-2,2-4', false, NONE",
      "2, 4, '4-2,2-4,4-3', false, NONE",

      // Super tie-break activé - fin au 3e set, 10 points gagnants
      "2, 6, '6-4,4-6,10-8', true, A",
      "2, 6, '6-4,4-6,10-9', true, NONE",
      "2, 6, '6-4,4-6,8-10', true, B",
      "2, 6, '6-4,4-6,9-11', true, B",
      "2, 6, '6-4,4-6,11-10', true, NONE",
      "2, 6, '6-4,4-6,11-13', true, B",
      "2, 6, '6-4,4-6,11-9', true, A",
      "2, 6, '6-4,4-6,8-8', true, NONE"
  })
  void testGetWinner_withFormat(
      int numberOfSetsToWin,
      int pointsPerSet,
      String scoreString,
      boolean isSuperTieBreakInFinalSet,
      String expectedWinner
  ) {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(numberOfSetsToWin);
    format.setPointsPerSet(pointsPerSet);
    format.setSuperTieBreakInFinalSet(isSuperTieBreakInFinalSet);

    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game();
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setFormat(format);

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    PlayerPair actual = service.getWinner(game);

    switch (expectedWinner) {
      case "A" -> assertEquals(teamA, actual);
      case "B" -> assertEquals(teamB, actual);
      case "NONE" -> assertNull(actual);
      default -> fail("Invalid expectedWinner value: " + expectedWinner);
    }
  }
}