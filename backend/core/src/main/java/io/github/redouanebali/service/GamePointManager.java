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

// Always work on the last set

    SetScore set = score.getSets().get(score.getSets().size() - 1);

    boolean isTieBreak = isTieBreak(game, set, score);

    boolean isSuperTieBreak = isSuperTieBreak(game);

    if (isTieBreak || isSuperTieBreak) {

      handleTieBreak(score, set, teamSide, increment, isSuperTieBreak);

      return;

    }

    handleStandardGame(score, set, teamSide, increment, withAdvantage);

  }


  private void handleTieBreak(Score score, SetScore set, TeamSide teamSide, boolean increment, boolean isSuperTieBreak) {

// 1. Update points

    int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;

    int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;

    if (teamSide == TeamSide.TEAM_A) {

      tbA = increment ? tbA + 1 : Math.max(0, tbA - 1);

      score.setTieBreakPointA(tbA);

    } else {

      tbB = increment ? tbB + 1 : Math.max(0, tbB - 1);

      score.setTieBreakPointB(tbB);

    }

// 2. Check for victory

    int winPoints = isSuperTieBreak ? 10 : 7;

    if (increment && (tbA >= winPoints || tbB >= winPoints) && Math.abs(tbA - tbB) >= 2) {

// Save the precise tie-break score in the set (ex: 7-5)

      set.setTieBreakTeamA(tbA);

      set.setTieBreakTeamB(tbB);

      if (isSuperTieBreak) {

// Super Tie-Break: Winner takes the set (often noted 1-0)

// Modify the current set (which is usually at 0-0)

        if (tbA > tbB) {

          set.setTeamAScore(1);

          set.setTeamBScore(0);

        } else {

          set.setTeamAScore(0);

          set.setTeamBScore(1);

        }

      } else {

// Standard Tie-Break: Winner takes the set (6-6 -> 7-6)

        if (tbA > tbB) {

          set.setTeamAScore(set.getTeamAScore() + 1);

        } else {

          set.setTeamBScore(set.getTeamBScore() + 1);

        }

      }

// Reset temporary points

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

// Only add a new set if the set is not already finished

            if (set.getTeamAScore() == 7 || (set.getTeamAScore() >= 6 && set.getTeamAScore() - set.getTeamBScore() >= 2)) {

              score.getSets().add(new SetScore(0, 0));

            }

          }

        } else {

// If opponent has advantage, revert to deuce

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

// Only add a new set if the set is not already finished

            if (set.getTeamBScore() == 7 || (set.getTeamBScore() >= 6 && set.getTeamBScore() - set.getTeamAScore() >= 2)) {

              score.getSets().add(new SetScore(0, 0));

            }

          }

        } else {

// If opponent has advantage, revert to deuce

          if (pA == GamePoint.AVANTAGE) {

            score.setCurrentGamePointA(GamePoint.QUARANTE);

          }

          score.setCurrentGamePointB(nextGamePoint(pB, pA, withAdvantage));

        }

      }

    } else {

      if (teamSide == TeamSide.TEAM_A) {

        score.setCurrentGamePointA(prevGamePoint(score.getCurrentGamePointA()));

      } else {

        score.setCurrentGamePointB(prevGamePoint(score.getCurrentGamePointB()));

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

// Fallback to 6 if tieBreakAt is 0 (frequent config error)

    int tieBreakAt = format.getTieBreakAt() > 0 ? format.getTieBreakAt() : 6;

    boolean scoreReached = set.getTeamAScore() == tieBreakAt && set.getTeamBScore() == tieBreakAt;

    boolean hasTieBreakPoints = score.getTieBreakPointA() != null || score.getTieBreakPointB() != null;

// We are in a tie-break if the score is reached OR if we already have tie-break points (for continuity)

// AND it's not a super tie-break

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

// The deciding set is the one that allows reaching the total max number of sets (ex: 3rd set for a 2 sets won match)

    int

        currentSetIndex =

        game.getScore()

            .getSets()

            .size(); // If size=3, we play the 3rd set (theoretical index 2, but size gives 3 existing sets including the last in progress)

// Calculation: if setsToWin=2, max 3 sets are played. If size=3, it's the deciding set.

    return game.getScore().getSets().size() == (setsToWin * 2 - 1);

  }


  public GamePoint nextGamePoint(GamePoint current, GamePoint opponent, boolean withAdvantage) {

    if (current == null) {

      current = GamePoint.ZERO;

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

        case QUARANTE, AVANTAGE -> GamePoint.QUARANTE;

      };

    }

  }


  public GamePoint prevGamePoint(GamePoint current) {

    if (current == null) {

      return GamePoint.ZERO;

    }

    return switch (current) {

      case AVANTAGE -> GamePoint.QUARANTE;

      case QUARANTE -> GamePoint.TRENTE;

      case TRENTE -> GamePoint.QUINZE;

      case QUINZE -> GamePoint.ZERO;

      case ZERO -> GamePoint.ZERO;

    };

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

// Win a set if 6 games with 2 difference or 7 games

    return (gamesWinner >= 6 && gamesWinner - gamesLoser >= 2) || gamesWinner == 7;

  }

}