import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.TournamentProgressionService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
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

    Game game = new Game(createSimpleFormat());
    game.setFormat(format);
    game.setTeamA(new PlayerPair());
    game.setTeamB(new PlayerPair());

    Score score = new Score();
    for (String set : scoreString.split(",")) {
      String[] parts = set.trim().split("-");
      score.addSetScore(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    game.setScore(score);

    boolean result = game.isFinished();
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

    Game game = new Game(format);
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

  @Test
  void testPropagateWinners() {
    Round roundCurrent = new Round();
    roundCurrent.setStage(Stage.R32);

    List<Game> gamesCurrent = new ArrayList<>();

    // Joueurs / paires
    PlayerPair byePair = PlayerPair.bye();
    byePair.setId(99L);

    PlayerPair pair1 = createPlayerPair(1L);
    PlayerPair pair2 = createPlayerPair(2L);
    PlayerPair pair3 = createPlayerPair(3L);
    PlayerPair pair4 = createPlayerPair(4L);
    PlayerPair pair5 = createPlayerPair(5L);
    PlayerPair pair6 = createPlayerPair(6L);

    // Création de 4 matchs dans roundCurrent
    // Match 0 : pair1 vs pair2 (terminé, pair1 gagne)
    Game game0 = new Game(createSimpleFormat());
    game0.setTeamA(pair1);
    game0.setTeamB(pair2);
    game0.setScore(createScoreWithWinner(pair1));
    game0.setFormat(createSimpleFormat());

    // Match 1 : pair3 vs BYE (pair3 passe automatiquement)
    Game game1 = new Game(createSimpleFormat());
    game1.setTeamA(pair3);
    game1.setTeamB(byePair);
    game1.setScore(null); // pas joué, mais BYE présent

    // Match 2 : pair4 vs pair5 (pas terminé)
    Game game2 = new Game(createSimpleFormat());
    game2.setTeamA(pair4);
    game2.setTeamB(pair5);
    game2.setScore(null);

    // Match 3 : pair6 vs BYE (pair6 passe automatiquement)
    Game game3 = new Game(createSimpleFormat());
    game3.setTeamA(pair6);
    game3.setTeamB(byePair);
    game3.setScore(null);

    gamesCurrent.add(game0);
    gamesCurrent.add(game1);
    gamesCurrent.add(game2);
    gamesCurrent.add(game3);

    roundCurrent.setGames(gamesCurrent);

    // Round suivant (ex: R16) avec 2 matchs (indices 0 et 1)
    Round roundNext = new Round();
    roundNext.setStage(Stage.R16);
    List<Game> gamesNext = new ArrayList<>();

    Game nextGame0 = new Game(createSimpleFormat()); // Correspond aux matchs 0 et 1 du round actuel
    Game nextGame1 = new Game(createSimpleFormat()); // Correspond aux matchs 2 et 3 du round actuel

    gamesNext.add(nextGame0);
    gamesNext.add(nextGame1);

    roundNext.setGames(gamesNext);

    // Tournoi
    Tournament tournament = new Tournament();
    Set<Round> rounds     = new LinkedHashSet<>();
    rounds.add(roundCurrent);
    rounds.add(roundNext);
    tournament.setRounds(rounds);

    // Action
    service.propagateWinners(tournament);

    // Vérification match nextGame0 (issu des matchs 0 et 1)
    // i=0 -> pair1 (vainqueur de game0) en teamA
    assertEquals(pair1, nextGame0.getTeamA());
    // i=1 -> pair3 (match gagné par BYE) en teamB
    assertEquals(pair3, nextGame0.getTeamB());

    // Vérification match nextGame1 (issu des matchs 2 et 3)
    // i=2 -> game2 non terminé donc teamA = null
    assertNull(nextGame1.getTeamA());
    // i=3 -> pair6 (BYE) en teamB
    assertEquals(pair6, nextGame1.getTeamB());
  }

  // Helpers

  private Score createScoreWithWinner(PlayerPair winner) {
    Score score = new Score();
    // Simple score pour indiquer que le match est terminé et que "winner" a gagné
    // Exemple : 6-0
    SetScore setScore = new SetScore();
    if (winner.getId() % 2 == 0) { // arbitraire juste pour varier
      setScore.setTeamAScore(0);
      setScore.setTeamBScore(6);
    } else {
      setScore.setTeamAScore(6);
      setScore.setTeamBScore(0);
    }
    score.setSets(List.of(setScore));
    return score;
  }

  private MatchFormat createSimpleFormat() {
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(1);
    format.setPointsPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    return format;
  }

  private PlayerPair createPlayerPair(long id) {
    Player player1 = new Player();
    player1.setId(id * 10 + 1);
    player1.setName("Player" + player1.getId() + "A");

    Player player2 = new Player();
    player2.setId(id * 10 + 2);
    player2.setName("Player" + player2.getId() + "B");

    PlayerPair pair = new PlayerPair();
    pair.setId(id);
    pair.setPlayer1(player1);
    pair.setPlayer2(player2);

    return pair;
  }
}