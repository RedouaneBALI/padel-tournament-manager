package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;

public class GamePointManager {

  public void updateGamePoint(Game game, TeamSide teamSide, boolean increment, boolean withAdvantage) {
    Score score = game.getScore();
    if (score == null) {
      score = new Score();
      game.setScore(score);
    }
    if (score.getSets().isEmpty()) {
      score.getSets().add(new SetScore(0, 0));
    }

    // Toujours travailler sur le dernier set
    SetScore set = score.getSets().get(score.getSets().size() - 1);

    boolean isTieBreak      = isTieBreak(game, set, score);
    boolean isSuperTieBreak = isSuperTieBreak(game);

    if (isTieBreak || isSuperTieBreak) {
      handleTieBreak(score, set, teamSide, increment, isSuperTieBreak);
      return;
    }

    handleStandardGame(score, set, teamSide, increment, withAdvantage);
  }

  private void handleTieBreak(Score score, SetScore set, TeamSide teamSide, boolean increment, boolean isSuperTieBreak) {
    boolean forceSuperTB = isSuperTieBreak;
    if (!isSuperTieBreak && set != null) {
      if (set.getTeamAScore() == 0 && set.getTeamBScore() == 0) {
        int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;
        int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;
        if (tbA >= 9 || tbB >= 9) {
          forceSuperTB = true;
        }
      }
    }
    int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;
    int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;

    if (teamSide == TeamSide.TEAM_A) {
      tbA = increment ? tbA + 1 : Math.max(0, tbA - 1);
      score.setTieBreakPointA(tbA);
    } else {
      tbB = increment ? tbB + 1 : Math.max(0, tbB - 1);
      score.setTieBreakPointB(tbB);
    }

    int winPoints = forceSuperTB ? 10 : 7;

    if (increment && (tbA >= winPoints || tbB >= winPoints) && Math.abs(tbA - tbB) >= 2) {
      set.setTieBreakTeamA(tbA);
      set.setTieBreakTeamB(tbB);

      if (forceSuperTB) {
        if (tbA > tbB) {
          set.setTeamAScore(1);
          set.setTeamBScore(0);
        } else {
          set.setTeamAScore(0);
          set.setTeamBScore(1);
        }
      } else {
        if (tbA > tbB) {
          set.setTeamAScore(set.getTeamAScore() + 1);
        } else {
          set.setTeamBScore(set.getTeamBScore() + 1);
        }
        score.getSets().add(new SetScore(0, 0));
      }
      score.setTieBreakPointA(null);
      score.setTieBreakPointB(null);
    }
  }

  private void handleStandardGame(Score score, SetScore set, TeamSide teamSide, boolean increment, boolean withAdvantage) {
    if (increment) {
      GamePoint pA = score.getCurrentGamePointA();
      GamePoint pB = score.getCurrentGamePointB();
      if (teamSide == TeamSide.TEAM_A) {
        if (shouldWinGame(pA, pB, withAdvantage)) {
          set.setTeamAScore(set.getTeamAScore() + 1);
          resetGamePoints(score);
          if (isSetWin(set.getTeamAScore(), set.getTeamBScore())) {
            score.getSets().add(new SetScore(0, 0));
          }
        } else {
          if (pB == GamePoint.AVANTAGE) {
            score.setCurrentGamePointB(GamePoint.QUARANTE);
          }
          score.setCurrentGamePointA(nextGamePoint(pA, pB, withAdvantage));
        }
      } else {
        if (shouldWinGame(pB, pA, withAdvantage)) {
          set.setTeamBScore(set.getTeamBScore() + 1);
          resetGamePoints(score);
          if (isSetWin(set.getTeamBScore(), set.getTeamAScore())) {
            score.getSets().add(new SetScore(0, 0));
          }
        } else {
          if (pA == GamePoint.AVANTAGE) {
            score.setCurrentGamePointA(GamePoint.QUARANTE);
          }
          score.setCurrentGamePointB(nextGamePoint(pB, pA, withAdvantage));
        }
      }
    } else {
      // Logique de décrémentation (Undo)
      if (teamSide == TeamSide.TEAM_A) {
        if (score.getCurrentGamePointA() == null && score.getCurrentGamePointB() == null && set.getTeamAScore() > 0) {
          set.setTeamAScore(set.getTeamAScore() - 1);
          score.setCurrentGamePointA(GamePoint.QUARANTE);
          score.setCurrentGamePointB(GamePoint.QUARANTE);
        } else {
          GamePoint prev = prevGamePoint(score.getCurrentGamePointA());
          score.setCurrentGamePointA(prev);
          if (prev == null) {
            score.setCurrentGamePointA(GamePoint.ZERO);
          }
        }
      } else {
        if (score.getCurrentGamePointA() == null && score.getCurrentGamePointB() == null && set.getTeamBScore() > 0) {
          set.setTeamBScore(set.getTeamBScore() - 1);
          score.setCurrentGamePointA(GamePoint.QUARANTE);
          score.setCurrentGamePointB(GamePoint.QUARANTE);
        } else {
          GamePoint prev = prevGamePoint(score.getCurrentGamePointB());
          score.setCurrentGamePointB(prev);
          if (prev == null) {
            score.setCurrentGamePointB(GamePoint.ZERO);
          }
        }
      }
    }
  }

  private void resetGamePoints(Score score) {
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
    return (scoreReached || hasTieBreakPoints) && !isSuperTieBreak(game);
  }

  private boolean isSuperTieBreak(Game game) {
    MatchFormat format = game.getFormat();
    if (format == null) {
      return false;
    }
    if (!format.isSuperTieBreakInFinalSet()) {
      return false;
    }
    int setsToWin = format.getNumberOfSetsToWin() > 0 ? format.getNumberOfSetsToWin() : 2;
    // On est en Super Tie Break si on est au set décisif
    // Note: Pour un match en 2 sets gagnants, le set index 2 (taille 3) est le décisif
    return game.getScore().getSets().size() == (setsToWin * 2 - 1);
  }

  public GamePoint nextGamePoint(GamePoint current, GamePoint opponent, boolean withAdvantage) {
    if (current == null) {
      return GamePoint.QUINZE; // Si null (0), passe à 15
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

  public GamePoint prevGamePoint(GamePoint current) {
    if (current == null || current == GamePoint.ZERO || current == GamePoint.QUINZE) {
      return null; // ou Zero selon impl
    }
    if (current == GamePoint.AVANTAGE) {
      return GamePoint.QUARANTE;
    }
    if (current == GamePoint.QUARANTE) {
      return GamePoint.TRENTE;
    }
    if (current == GamePoint.TRENTE) {
      return GamePoint.QUINZE;
    }
    return GamePoint.ZERO;
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
    return (gamesWinner >= 6 && gamesWinner - gamesLoser >= 2) || gamesWinner == 7;
  }
}
