package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;

public class GamePointManager {

  public void updateGamePoint(Game game, TeamSide teamSide) {
    Score score = game.getScore();
    if (score == null) {
      score = new Score();
      game.setScore(score);
    }
    if (score.getSets().isEmpty()) {
      score.getSets().add(new SetScore(0, 0));
    }

    // Always work on the last set
    SetScore set = score.getSets().get(score.getSets().size() - 1);

    // Detect game mode
    boolean isSuperTieBreak = isSuperTieBreak(game);
    // Only check for standard TieBreak if it's NOT a Super TieBreak
    boolean isTieBreak    = !isSuperTieBreak && isTieBreak(game, set, score);
    boolean withAdvantage = game.getFormat() != null && game.getFormat().isAdvantage();

    if (isSuperTieBreak) {
      handleTieBreak(score, set, teamSide, true);
    } else if (isTieBreak) {
      handleTieBreak(score, set, teamSide, false);
    } else {
      handleStandardGame(score, set, teamSide, withAdvantage);
    }
  }

  private void handleTieBreak(Score score, SetScore set, TeamSide teamSide, boolean isSuperTieBreak) {
    int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;
    int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;

    if (teamSide == TeamSide.TEAM_A) {
      tbA++;
      score.setTieBreakPointA(tbA);
    } else {
      tbB++;
      score.setTieBreakPointB(tbB);
    }

    int winPoints = isSuperTieBreak ? 10 : 7;

    // Check for victory (Target points reached + 2 points difference)
    if ((tbA >= winPoints || tbB >= winPoints) && Math.abs(tbA - tbB) >= 2) {
      set.setTieBreakTeamA(tbA);
      set.setTieBreakTeamB(tbB);

      if (isSuperTieBreak) {
        // Super Tie-Break: Winner takes the set (1-0)
        if (tbA > tbB) {
          set.setTeamAScore(1);
          set.setTeamBScore(0);
        } else {
          set.setTeamAScore(0);
          set.setTeamBScore(1);
        }
      } else {
        // Standard Tie-Break: Winner takes the set (7-6)
        if (tbA > tbB) {
          set.setTeamAScore(set.getTeamAScore() + 1);
        } else {
          set.setTeamBScore(set.getTeamBScore() + 1);
        }
        // Prepare next set
        score.getSets().add(new SetScore(0, 0));
      }

      // Reset tie-break points
      score.setTieBreakPointA(null);
      score.setTieBreakPointB(null);
    }
  }

  private void handleStandardGame(Score score, SetScore set, TeamSide teamSide, boolean withAdvantage) {
    GamePoint pA = score.getCurrentGamePointA();
    GamePoint pB = score.getCurrentGamePointB();

    // Use ZERO for null to simplify logic, but store null in DB
    GamePoint currentA = pA == null ? GamePoint.ZERO : pA;
    GamePoint currentB = pB == null ? GamePoint.ZERO : pB;

    if (teamSide == TeamSide.TEAM_A) {
      if (shouldWinGame(currentA, currentB, withAdvantage)) {
        winStandardGame(score, set, TeamSide.TEAM_A);
      } else {
        // Handle return to Deuce
        if (withAdvantage && currentB == GamePoint.AVANTAGE) {
          score.setCurrentGamePointB(GamePoint.QUARANTE);
          // A stays at 40
        } else {
          score.setCurrentGamePointA(nextGamePoint(currentA, currentB, withAdvantage));
        }
      }
    } else { // TEAM_B
      if (shouldWinGame(currentB, currentA, withAdvantage)) {
        winStandardGame(score, set, TeamSide.TEAM_B);
      } else {
        // Handle return to Deuce
        if (withAdvantage && currentA == GamePoint.AVANTAGE) {
          score.setCurrentGamePointA(GamePoint.QUARANTE);
          // B stays at 40
        } else {
          score.setCurrentGamePointB(nextGamePoint(currentB, currentA, withAdvantage));
        }
      }
    }
  }

  private void winStandardGame(Score score, SetScore set, TeamSide winner) {
    if (winner == TeamSide.TEAM_A) {
      set.setTeamAScore(set.getTeamAScore() + 1);
    } else {
      set.setTeamBScore(set.getTeamBScore() + 1);
    }

    resetGamePoints(score);

    // Check if set is won
    if (isSetWin(set.getTeamAScore(), set.getTeamBScore())) {
      score.getSets().add(new SetScore(0, 0));
    }
  }

  private void resetGamePoints(Score score) {
    // IMPORTANT: Set to null to match test expectations
    score.setCurrentGamePointA(null);
    score.setCurrentGamePointB(null);
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

    // Case 1: Real match context (e.g., 3rd set in a best of 3)
    boolean isDecidingSet = currentSetCount == (setsToWin * 2 - 1);

    // Correction : ne pas activer le super tie-break en test si le flag n'est pas activé
    // On retire la condition isTestScenario qui rendait isSuperTieBreak toujours vrai en test
    // Ancien code :
    // boolean isTestScenario = currentSetCount == 1 && format.isSuperTieBreakInFinalSet();
    // return isDecidingSet || isTestScenario;
    return isDecidingSet;
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

  public boolean shouldWinGame(GamePoint currentPoint, GamePoint opponentPoint, boolean withAdvantage) {
    if (currentPoint == null) {
      currentPoint = GamePoint.ZERO;
    }
    if (opponentPoint == null) {
      opponentPoint = GamePoint.ZERO;
    }

    if (withAdvantage) {
      if (currentPoint == GamePoint.AVANTAGE) {
        return true;
      }
      return currentPoint == GamePoint.QUARANTE && opponentPoint != GamePoint.QUARANTE && opponentPoint != GamePoint.AVANTAGE;
    } else {
      return currentPoint == GamePoint.QUARANTE;
    }
  }

  private boolean isSetWin(int gamesWinner, int gamesLoser) {
    return (gamesWinner == 6 && gamesWinner - gamesLoser >= 2) || gamesWinner == 7;
  }

  /**
   * Undo the last game point for the given team side. This method is naive: it only decrements the last point (standard/tiebreak), does not handle
   * full history. For robust undo, a stack of actions is needed.
   */
  public void undoGamePoint(Game game, TeamSide teamSide) {
    Score score = game.getScore();
    if (score == null || score.getSets().isEmpty()) {
      return;
    }
    SetScore set             = score.getSets().get(score.getSets().size() - 1);
    boolean  isSuperTieBreak = isSuperTieBreak(game);
    boolean  isTieBreak      = !isSuperTieBreak && isTieBreak(game, set, score);
    boolean  withAdvantage   = game.getFormat() != null && game.getFormat().isAdvantage();

    if (isSuperTieBreak || isTieBreak) {
      // Undo tiebreak point
      if (teamSide == TeamSide.TEAM_A) {
        Integer tbA = score.getTieBreakPointA();
        if (tbA != null && tbA > 0) {
          tbA = tbA - 1;
          score.setTieBreakPointA(tbA == 0 ? null : tbA);
        } else if (tbA != null && tbA == 0) {
          score.setTieBreakPointA(null);
        }
      } else {
        Integer tbB = score.getTieBreakPointB();
        if (tbB != null && tbB > 0) {
          tbB = tbB - 1;
          score.setTieBreakPointB(tbB == 0 ? null : tbB);
        } else if (tbB != null && tbB == 0) {
          score.setTieBreakPointB(null);
        }
      }
    } else {
      // Undo standard game point
      if (teamSide == TeamSide.TEAM_A) {
        GamePoint pA = score.getCurrentGamePointA();
        score.setCurrentGamePointA(prevGamePoint(pA, withAdvantage));
      } else {
        GamePoint pB = score.getCurrentGamePointB();
        score.setCurrentGamePointB(prevGamePoint(pB, withAdvantage));
      }
    }
  }

  private GamePoint prevGamePoint(GamePoint current, boolean withAdvantage) {
    if (current == null) {
      return null;
    }
    if (withAdvantage) {
      return switch (current) {
        case AVANTAGE -> GamePoint.QUARANTE;
        case QUARANTE -> GamePoint.TRENTE;
        case TRENTE -> GamePoint.QUINZE;
        case QUINZE -> GamePoint.ZERO;
        case ZERO -> GamePoint.ZERO;
      };
    } else {
      return switch (current) {
        case QUARANTE -> GamePoint.TRENTE;
        case TRENTE -> GamePoint.QUINZE;
        case QUINZE -> GamePoint.ZERO;
        case ZERO -> GamePoint.ZERO;
        default -> null;
      };
    }
  }
}
