package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;
import org.springframework.stereotype.Component;

/**
 * Service responsible for managing game points during a match. Handles point increment, undo operations, and game/set completion logic.
 */
@Component
public class GamePointManager {

  public void incrementGamePoint(Game game, TeamSide teamSide) {
    Score score = initializeScore(game);
    score.saveToHistory();

    ensureActiveSetExists(game, score);
    SetScore activeSet = getActiveSet(score);

    GamePlayMode mode = determineGamePlayMode(game, score, activeSet);
    handleGamePointBasedOnMode(game, score, activeSet, teamSide, mode);
  }

  private Score initializeScore(Game game) {
    Score score = game.getScore();
    if (score == null) {
      score = new Score();
      game.setScore(score);
    }
    if (score.getCurrentGamePointA() == null) {
      score.setCurrentGamePointA(GamePoint.ZERO);
    }
    if (score.getCurrentGamePointB() == null) {
      score.setCurrentGamePointB(GamePoint.ZERO);
    }
    return score;
  }

  private GamePlayMode determineGamePlayMode(Game game, Score score, SetScore activeSet) {
    boolean isSuperTieBreak = isSuperTieBreak(game);
    boolean isTieBreak      = !isSuperTieBreak && isTieBreak(game, activeSet, score);
    boolean withAdvantage   = hasAdvantageRule(game);
    return new GamePlayMode(isSuperTieBreak, isTieBreak, withAdvantage);
  }

  private void handleGamePointBasedOnMode(Game game, Score score, SetScore activeSet, TeamSide teamSide, GamePlayMode mode) {
    if (mode.isSuperTieBreak) {
      handleTieBreak(score, activeSet, teamSide, true, game);
    } else if (mode.isTieBreak) {
      handleTieBreak(score, activeSet, teamSide, false, game);
    } else {
      handleStandardGameMode(game, score, activeSet, teamSide, mode.withAdvantage);
    }
  }

  private void handleStandardGameMode(Game game, Score score, SetScore activeSet, TeamSide teamSide, boolean withAdvantage) {
    if (isSetFinished(game, activeSet)) {
      checkAndAddNewSet(game, score);
      activeSet = getActiveSet(score);
    }
    handleStandardGame(game, score, activeSet, teamSide, withAdvantage);
  }

  private boolean hasAdvantageRule(Game game) {
    return game.getFormat() != null && game.getFormat().isAdvantage();
  }

  private void ensureActiveSetExists(Game game, Score score) {
    if (score.getSets().isEmpty()) {
      score.getSets().add(new SetScore(0, 0));
    } else {
      SetScore lastSet = score.getSets().get(score.getSets().size() - 1);
      if (isSetFinished(game, lastSet)) {
        checkAndAddNewSet(game, score);
      }
    }
  }

  private SetScore getActiveSet(Score score) {
    return score.getSets().get(score.getSets().size() - 1);
  }

  private void checkAndAddNewSet(Game game, Score score) {
    int             setsToWin = getSetsToWin(game);
    SetScoreCounter counter   = countCompletedSets(game, score);

    if (counter.setsWonA < setsToWin && counter.setsWonB < setsToWin) {
      SetScore last = score.getSets().get(score.getSets().size() - 1);
      if (isSetFinished(game, last)) {
        score.getSets().add(new SetScore(0, 0));
      }
    }
  }

  private int getSetsToWin(Game game) {
    MatchFormat format = game.getFormat();
    return (format != null && format.getNumberOfSetsToWin() > 0) ? format.getNumberOfSetsToWin() : 2;
  }

  private SetScoreCounter countCompletedSets(Game game, Score score) {
    SetScoreCounter counter = new SetScoreCounter();
    for (SetScore s : score.getSets()) {
      if (isSetFinished(game, s) && isTeamASetWinner(s)) {
        counter.setsWonA++;
      } else if (isSetFinished(game, s)) {
        counter.setsWonB++;
      }
    }
    return counter;
  }

  private boolean isTeamASetWinner(SetScore set) {
    if (set.getTieBreakTeamA() != null || set.getTieBreakTeamB() != null) {
      int tbA = set.getTieBreakTeamA() != null ? set.getTieBreakTeamA() : 0;
      int tbB = set.getTieBreakTeamB() != null ? set.getTieBreakTeamB() : 0;
      return tbA > tbB;
    }
    return set.getTeamAScore() > set.getTeamBScore();
  }

  private void handleTieBreak(Score score, SetScore set, TeamSide teamSide, boolean isSuperTieBreak, Game game) {
    TieBreakPoints points    = incrementTieBreakPoints(score, set, teamSide);
    int            winPoints = isSuperTieBreak ? 10 : 7;

    if (isTieBreakWon(points.tbA, points.tbB, winPoints)) {
      handleTieBreakVictory(score, set, points, isSuperTieBreak, game);
    }
  }

  private TieBreakPoints incrementTieBreakPoints(Score score, SetScore set, TeamSide teamSide) {
    int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;
    int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;

    if (teamSide == TeamSide.TEAM_A) {
      tbA++;
    } else {
      tbB++;
    }

    score.setTieBreakPointA(tbA);
    score.setTieBreakPointB(tbB);
    set.setTieBreakTeamA(tbA);
    set.setTieBreakTeamB(tbB);

    return new TieBreakPoints(tbA, tbB);
  }

  private boolean isTieBreakWon(int tbA, int tbB, int winPoints) {
    return (tbA >= winPoints || tbB >= winPoints) && Math.abs(tbA - tbB) >= 2;
  }

  private void handleTieBreakVictory(Score score, SetScore set, TieBreakPoints points, boolean isSuperTieBreak, Game game) {
    if (isSuperTieBreak) {
      handleSuperTieBreakVictory(set, points);
    } else {
      handleRegularTieBreakVictory(set, points);
    }
    checkAndAddNewSet(game, score);
    score.setTieBreakPointA(null);
    score.setTieBreakPointB(null);
  }

  private void handleSuperTieBreakVictory(SetScore set, TieBreakPoints points) {
    if (points.tbA > points.tbB) {
      set.setTeamAScore(1);
      set.setTeamBScore(0);
    } else {
      set.setTeamAScore(0);
      set.setTeamBScore(1);
    }
  }

  private void handleRegularTieBreakVictory(SetScore set, TieBreakPoints points) {
    if (points.tbA > points.tbB) {
      set.setTeamAScore(set.getTeamAScore() + 1);
    } else {
      set.setTeamBScore(set.getTeamBScore() + 1);
    }
  }

  private void handleStandardGame(Game game, Score score, SetScore set, TeamSide teamSide, boolean withAdvantage) {
    GamePoint currentA = score.getCurrentGamePointA() == null ? GamePoint.ZERO : score.getCurrentGamePointA();
    GamePoint currentB = score.getCurrentGamePointB() == null ? GamePoint.ZERO : score.getCurrentGamePointB();

    if (teamSide == TeamSide.TEAM_A) {
      if (shouldWinGame(currentA, currentB, withAdvantage)) {
        winStandardGame(game, score, set, TeamSide.TEAM_A);
      } else {
        if (withAdvantage && currentB == GamePoint.AVANTAGE) {
          score.setCurrentGamePointB(GamePoint.QUARANTE);
        } else {
          score.setCurrentGamePointA(nextGamePoint(currentA, currentB, withAdvantage));
        }
      }
    } else {
      if (shouldWinGame(currentB, currentA, withAdvantage)) {
        winStandardGame(game, score, set, TeamSide.TEAM_B);
      } else {
        if (withAdvantage && currentA == GamePoint.AVANTAGE) {
          score.setCurrentGamePointA(GamePoint.QUARANTE);
        } else {
          score.setCurrentGamePointB(nextGamePoint(currentB, currentA, withAdvantage));
        }
      }
    }
  }

  private void winStandardGame(Game game, Score score, SetScore set, TeamSide winner) {
    if (winner == TeamSide.TEAM_A) {
      set.setTeamAScore(set.getTeamAScore() + 1);
    } else {
      set.setTeamBScore(set.getTeamBScore() + 1);
    }
    resetGamePoints(score);

    if (isSetFinished(game, set)) {
      checkAndAddNewSet(game, score);
    }
  }

  private boolean isSetFinished(Game game, SetScore set) {
    if (game.getFormat() != null && game.getFormat().isSuperTieBreakInFinalSet()) {
      if ((set.getTeamAScore() == 1 && set.getTeamBScore() == 0) || (set.getTeamAScore() == 0 && set.getTeamBScore() == 1)) {
        if (isSuperTieBreakSetContext(game, set)) {
          return true;
        }
      }
    }

    return isSetWin(set.getTeamAScore(), set.getTeamBScore()) || isSetWin(set.getTeamBScore(), set.getTeamAScore());
  }

  private boolean isSetWin(int winner, int loser) {
    return (winner == 6 && winner - loser >= 2) || winner == 7;
  }

  private boolean isTieBreak(Game game, SetScore set, Score score) {
    MatchFormat format = game.getFormat();
    if (format == null) {
      return false;
    }
    int     tieBreakAt        = format.getTieBreakAt() > 0 ? format.getTieBreakAt() : 6;
    boolean scoreReached      = set.getTeamAScore() == tieBreakAt && set.getTeamBScore() == tieBreakAt;
    boolean hasTieBreakPoints = score.getTieBreakPointA() != null || score.getTieBreakPointB() != null;
    return scoreReached || hasTieBreakPoints;
  }

  private boolean isSuperTieBreak(Game game) {
    MatchFormat format = game.getFormat();
    if (format == null) {
      return false;
    }
    if (!format.isSuperTieBreakInFinalSet()) {
      return false;
    }
    int setsToWin       = format.getNumberOfSetsToWin() > 0 ? format.getNumberOfSetsToWin() : 2;
    int currentSetCount = game.getScore().getSets().size();
    return currentSetCount == (setsToWin * 2 - 1);
  }

  private boolean isSuperTieBreakSetContext(Game game, SetScore set) {
    MatchFormat format = game.getFormat();
    if (format == null || !format.isSuperTieBreakInFinalSet()) {
      return false;
    }
    int setsToWin = format.getNumberOfSetsToWin() > 0 ? format.getNumberOfSetsToWin() : 2;
    int index     = game.getScore().getSets().indexOf(set);
    return index == (setsToWin * 2 - 2);
  }

  public void undoGamePoint(Game game) {
    Score score = game.getScore();
    if (score != null) {
      score.undo();
    }
  }

  private void resetGamePoints(Score score) {
    score.setCurrentGamePointA(GamePoint.ZERO);
    score.setCurrentGamePointB(GamePoint.ZERO);
  }

  public GamePoint nextGamePoint(GamePoint current, GamePoint opponent, boolean withAdvantage) {
    if (current == null) {
      return GamePoint.QUINZE;
    }
    if (opponent == null) {
      opponent = GamePoint.ZERO;
    }
    if (withAdvantage) {
      return switch (current) {
        case ZERO -> GamePoint.QUINZE;
        case QUINZE -> GamePoint.TRENTE;
        case TRENTE -> GamePoint.QUARANTE;
        case QUARANTE -> (opponent == GamePoint.QUARANTE) ? GamePoint.AVANTAGE : GamePoint.QUARANTE;
        case AVANTAGE -> GamePoint.AVANTAGE;
      };
    } else {
      return switch (current) {
        case ZERO -> GamePoint.QUINZE;
        case QUINZE -> GamePoint.TRENTE;
        case TRENTE -> GamePoint.QUARANTE;
        default -> GamePoint.QUARANTE;
      };
    }
  }

  public boolean shouldWinGame(GamePoint current, GamePoint opponent, boolean withAdvantage) {
    if (current == null) {
      current = GamePoint.ZERO;
    }
    if (opponent == null) {
      opponent = GamePoint.ZERO;
    }
    if (withAdvantage) {
      if (current == GamePoint.AVANTAGE) {
        return true;
      }
      return current == GamePoint.QUARANTE && opponent != GamePoint.QUARANTE && opponent != GamePoint.AVANTAGE;
    } else {
      return current == GamePoint.QUARANTE;
    }
  }

  private record GamePlayMode(boolean isSuperTieBreak, boolean isTieBreak, boolean withAdvantage) {

  }

  private static class SetScoreCounter {

    int setsWonA = 0;
    int setsWonB = 0;
  }

  private record TieBreakPoints(int tbA, int tbB) {

  }
}