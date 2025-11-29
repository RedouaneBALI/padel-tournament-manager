package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class GamePointManagerTest {

  private GamePointManager gamePointManager;

  @BeforeEach
  void setup() {
    gamePointManager = new GamePointManager();
  }

  private Game createGameWithScoreAndFormat(int gamesA,
                                            int gamesB,
                                            GamePoint pointA,
                                            GamePoint pointB,
                                            String startTieBreakA,
                                            String startTieBreakB) {
    Game        game   = new Game();
    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);
    game.setFormat(format);
    Score score = new Score();
    score.getSets().add(new SetScore(gamesA, gamesB));
    score.setCurrentGamePointA(pointA);
    score.setCurrentGamePointB(pointB);
    if (startTieBreakA != null && !startTieBreakA.isBlank()) {
      score.setTieBreakPointA(Integer.parseInt(startTieBreakA));
    }
    if (startTieBreakB != null && !startTieBreakB.isBlank()) {
      score.setTieBreakPointB(Integer.parseInt(startTieBreakB));
    }
    game.setScore(score);
    return game;
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/game_point_transitions.csv", numLinesToSkip = 1)
  void testUpdateGamePointTransitionsCsv(String currentA,
                                         String currentB,
                                         String teamSide,
                                         boolean increment,
                                         boolean withAdvantage,
                                         boolean withTieBreak,
                                         boolean withSuperTieBreak,
                                         int startGamesA,
                                         int startGamesB,
                                         String startTieBreakA,
                                         String startTieBreakB,
                                         String expectedGamesA,
                                         String expectedGamesB,
                                         String expectedTieBreakA,
                                         String expectedTieBreakB,
                                         String expectedPointA,
                                         String expectedPointB,
                                         String description) {
    GamePoint pointA = (currentA == null || currentA.isBlank()) ? null : GamePoint.valueOf(currentA);
    GamePoint pointB = (currentB == null || currentB.isBlank()) ? null : GamePoint.valueOf(currentB);
    TeamSide  side   = TeamSide.valueOf(teamSide);
    Game      game   = createGameWithScoreAndFormat(startGamesA, startGamesB, pointA, pointB, startTieBreakA, startTieBreakB);
    gamePointManager.updateGamePoint(game, side, increment, withAdvantage);
    int     expGamesA       = (expectedGamesA == null || expectedGamesA.isBlank()) ? startGamesA : Integer.parseInt(expectedGamesA);
    int     expGamesB       = (expectedGamesB == null || expectedGamesB.isBlank()) ? startGamesB : Integer.parseInt(expectedGamesB);
    boolean isSuperTieBreak = withSuperTieBreak;
    int     setIndex;
    if (isSuperTieBreak) {
      setIndex = game.getScore().getSets().size() - 1;
    } else {
      // If a new set was added (0-0), check the previous set
      int lastIdx = game.getScore().getSets().size() - 1;
      if (game.getScore().getSets().get(lastIdx).getTeamAScore() == 0
          && game.getScore().getSets().get(lastIdx).getTeamBScore() == 0
          && game.getScore().getSets().size() > 1) {
        setIndex = lastIdx - 1;
      } else {
        setIndex = lastIdx;
      }
    }
    assertEquals(expGamesA, game.getScore().getSets().get(setIndex).getTeamAScore(), description + " : Game Score A Incorrect");
    assertEquals(expGamesB, game.getScore().getSets().get(setIndex).getTeamBScore(), description + " : Game Score B Incorrect");
    if (expectedPointA != null && !expectedPointA.isEmpty()) {
      String actualA = game.getScore().getCurrentGamePointA() != null ? game.getScore().getCurrentGamePointA().name() : null;
      if (actualA != null && !actualA.equals(expectedPointA)) {
        try {
          var enumA = GamePoint.valueOf(actualA);
          assertEquals(expectedPointA, enumA.getDisplay(), description + " (display value)");
        } catch (Exception e) {
          assertEquals(expectedPointA, actualA, description);
        }
      } else {
        assertEquals(expectedPointA, actualA, description);
      }
    }
    if (expectedPointB != null && !expectedPointB.isEmpty()) {
      String actualB = game.getScore().getCurrentGamePointB() != null ? game.getScore().getCurrentGamePointB().name() : null;
      if (actualB != null && !actualB.equals(expectedPointB)) {
        try {
          var enumB = GamePoint.valueOf(actualB);
          assertEquals(expectedPointB, enumB.getDisplay(), description + " (display value)");
        } catch (Exception e) {
          assertEquals(expectedPointB, actualB, description);
        }
      } else {
        assertEquals(expectedPointB, actualB, description);
      }
    }
  }
}
