package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoundTest {

  @Test
  void constructor_shouldInitializeWithStage() {
    Round round = new Round(Stage.QUARTERS);

    assertEquals(Stage.QUARTERS, round.getStage());
    assertTrue(round.getGames().isEmpty());
    assertTrue(round.getPools().isEmpty());
  }

  @Test
  void addGame_shouldCreateGameWithFormat() {
    Round       round  = new Round(Stage.SEMIS);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);
    round.setMatchFormat(format);

    PlayerPair teamA = new PlayerPair(new Player("A1"), new Player("A2"), 1);
    PlayerPair teamB = new PlayerPair(new Player("B1"), new Player("B2"), 2);

    round.addGame(teamA, teamB);

    assertEquals(1, round.getGames().size());
    Game game = round.getGames().get(0);
    assertEquals(teamA, game.getTeamA());
    assertEquals(teamB, game.getTeamB());
    assertEquals(format, game.getFormat());
  }

  @Test
  void addGame_withGameObject_shouldAddDirectly() {
    Round round = new Round(Stage.FINAL);
    Game  game  = new Game(new MatchFormat());

    round.addGame(game);

    assertEquals(1, round.getGames().size());
    assertEquals(game, round.getGames().get(0));
  }

  @Test
  void addGames_shouldAddMultipleGames() {
    Round round = new Round(Stage.R16);
    Game  game1 = new Game(new MatchFormat());
    Game  game2 = new Game(new MatchFormat());

    round.addGames(List.of(game1, game2));

    assertEquals(2, round.getGames().size());
  }

  @Test
  void replaceGames_shouldClearAndAddNew() {
    Round round   = new Round(Stage.QUARTERS);
    Game  oldGame = new Game(new MatchFormat());
    round.addGame(oldGame);

    Game newGame1 = new Game(new MatchFormat());
    Game newGame2 = new Game(new MatchFormat());
    round.replaceGames(List.of(newGame1, newGame2));

    assertEquals(2, round.getGames().size());
    assertFalse(round.getGames().contains(oldGame));
    assertTrue(round.getGames().contains(newGame1));
    assertTrue(round.getGames().contains(newGame2));
  }

  @Test
  void replaceGames_withNull_shouldClearGames() {
    Round round = new Round(Stage.SEMIS);
    round.addGame(new Game(new MatchFormat()));

    round.replaceGames(null);

    assertTrue(round.getGames().isEmpty());
  }

  @Test
  void replaceGames_withEmptyList_shouldClearGames() {
    Round round = new Round(Stage.SEMIS);
    round.addGame(new Game(new MatchFormat()));

    round.replaceGames(List.of());

    assertTrue(round.getGames().isEmpty());
  }

  @Test
  void addPool_shouldAddToPoolsList() {
    Round round = new Round(Stage.GROUPS);
    Pool  pool  = new Pool("A", List.of());

    round.addPool(pool);

    assertEquals(1, round.getPools().size());
    assertEquals(pool, round.getPools().get(0));
  }

  @Test
  void addPools_shouldAddMultiplePools() {
    Round round = new Round(Stage.GROUPS);
    Pool  pool1 = new Pool("A", List.of());
    Pool  pool2 = new Pool("B", List.of());

    round.addPools(List.of(pool1, pool2));

    assertEquals(2, round.getPools().size());
  }

  @ParameterizedTest
  @CsvSource({
      "true, true, true",
      "false, true, false",
      "true, false, false",
      "false, false, false"
  })
  void isFinished_shouldCheckAllGamesComplete(boolean game1Finished, boolean game2Finished, boolean expected) {
    Round       round  = new Round(Stage.QUARTERS);
    MatchFormat format = new MatchFormat(1L, 2, 6, true, false);

    Game game1 = new Game(format);
    game1.setTeamA(new PlayerPair(new Player("A1"), new Player("A2"), 1));
    game1.setTeamB(new PlayerPair(new Player("B1"), new Player("B2"), 2));
    if (game1Finished) {
      Score score1 = Score.fromString("6-4 6-4");
      game1.setScore(score1);
    }

    Game game2 = new Game(format);
    game2.setTeamA(new PlayerPair(new Player("C1"), new Player("C2"), 3));
    game2.setTeamB(new PlayerPair(new Player("D1"), new Player("D2"), 4));
    if (game2Finished) {
      Score score2 = Score.fromString("6-3 6-3");
      game2.setScore(score2);
    }

    round.addGames(List.of(game1, game2));

    assertEquals(expected, round.isFinished());
  }

  @Test
  void isFinished_withNoGames_shouldReturnFalse() {
    Round round = new Round(Stage.FINAL);

    assertFalse(round.isFinished());
  }

  @Test
  void isPairPresent_shouldReturnTrueWhenPairInGames() {
    Round      round = new Round(Stage.SEMIS);
    PlayerPair pair  = new PlayerPair(new Player("A"), new Player("B"), 1);
    pair.setId(1L);

    Game game = new Game(new MatchFormat());
    game.setTeamA(pair);
    game.setTeamB(new PlayerPair(new Player("C"), new Player("D"), 2));
    round.addGame(game);

    assertTrue(round.isPairPresent(pair));
  }

  @Test
  void isPairPresent_shouldReturnFalseWhenPairNotInGames() {
    Round      round = new Round(Stage.SEMIS);
    PlayerPair pair  = new PlayerPair(new Player("A"), new Player("B"), 1);
    pair.setId(1L);

    Game game = new Game(new MatchFormat());
    game.setTeamA(new PlayerPair(new Player("C"), new Player("D"), 2));
    game.setTeamB(new PlayerPair(new Player("E"), new Player("F"), 3));
    round.addGame(game);

    assertFalse(round.isPairPresent(pair));
  }

  @Test
  void isPairPresent_withNull_shouldReturnFalse() {
    Round round = new Round(Stage.FINAL);

    assertFalse(round.isPairPresent(null));
  }

  @Test
  void equals_shouldCompareByStage() {
    Round round1 = new Round(Stage.QUARTERS);
    Round round2 = new Round(Stage.QUARTERS);
    Round round3 = new Round(Stage.SEMIS);

    assertEquals(round1, round2);
    assertNotEquals(round1, round3);
  }

  @Test
  void hashCode_shouldBeBasedOnStage() {
    Round round1 = new Round(Stage.FINAL);
    Round round2 = new Round(Stage.FINAL);

    assertEquals(round1.hashCode(), round2.hashCode());
  }
}

