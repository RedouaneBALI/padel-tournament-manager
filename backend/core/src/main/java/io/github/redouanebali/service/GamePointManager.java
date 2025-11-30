package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GamePointManager {

  private static final Logger log = LoggerFactory.getLogger(GamePointManager.class);

  public void updateGamePoint(Game game, TeamSide teamSide) {
    Score score = game.getScore();
    if (score == null) {
      score = new Score();
      game.setScore(score);
    }
    score.saveToHistory();

    // 1. On s'assure d'avoir un set actif valide (création du 3e set si besoin)
    ensureActiveSetExists(game, score);

    // 2. On récupère le set en cours
    SetScore activeSet = getActiveSet(score);

    // 3. Détection du mode de jeu
    boolean isSuperTieBreak = isSuperTieBreak(game);
    boolean isTieBreak      = !isSuperTieBreak && isTieBreak(game, activeSet, score);
    boolean withAdvantage   = game.getFormat() != null && game.getFormat().isAdvantage();

    if (isSuperTieBreak) {
      handleTieBreak(score, activeSet, teamSide, true, game);
    } else if (isTieBreak) {
      handleTieBreak(score, activeSet, teamSide, false, game);
    } else {
      // Sécurité : si le set récupéré est déjà fini (ex: on continue de cliquer), on passe au suivant
      if (isSetFinished(game, activeSet)) {
        checkAndAddNewSet(game, score);
        activeSet = getActiveSet(score);
      }
      handleStandardGame(game, score, activeSet, teamSide, withAdvantage);
    }
  }

  // --- Gestion Sets ---

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
    MatchFormat format    = game.getFormat();
    int         setsToWin = (format != null && format.getNumberOfSetsToWin() > 0) ? format.getNumberOfSetsToWin() : 2;

    int setsWonA = 0;
    int setsWonB = 0;

    for (SetScore s : score.getSets()) {
      if (isSetFinished(game, s)) {
        // Déterminer le vainqueur du set
        boolean teamAWon;
        if (s.getTieBreakTeamA() != null || s.getTieBreakTeamB() != null) {
          // Victoire aux points (Tie-Break)
          int tbA = s.getTieBreakTeamA() != null ? s.getTieBreakTeamA() : 0;
          int tbB = s.getTieBreakTeamB() != null ? s.getTieBreakTeamB() : 0;
          teamAWon = tbA > tbB;
        } else {
          // Victoire aux jeux (Standard ou STB 1-0)
          teamAWon = s.getTeamAScore() > s.getTeamBScore();
        }

        if (teamAWon) {
          setsWonA++;
        } else {
          setsWonB++;
        }
      }
    }

    // On ajoute un set seulement si le match n'est PAS fini
    if (setsWonA < setsToWin && setsWonB < setsToWin) {
      SetScore last = score.getSets().get(score.getSets().size() - 1);
      // Et seulement si le dernier set est bien terminé pour éviter les doublons
      if (isSetFinished(game, last)) {
        score.getSets().add(new SetScore(0, 0));
      }
    }
  }

  // --- Tie-Break & Super Tie-Break ---

  private void handleTieBreak(Score score, SetScore set, TeamSide teamSide, boolean isSuperTieBreak, Game game) {
    int tbA = score.getTieBreakPointA() != null ? score.getTieBreakPointA() : 0;
    int tbB = score.getTieBreakPointB() != null ? score.getTieBreakPointB() : 0;

    // Increment
    if (teamSide == TeamSide.TEAM_A) {
      tbA++;
    } else {
      tbB++;
    }
    // Synchronize both Score and SetScore
    score.setTieBreakPointA(tbA);
    score.setTieBreakPointB(tbB);
    set.setTieBreakTeamA(tbA);
    set.setTieBreakTeamB(tbB);

    int winPoints = isSuperTieBreak ? 10 : 7;

    // Victory
    if ((tbA >= winPoints || tbB >= winPoints) && Math.abs(tbA - tbB) >= 2) {
      if (isSuperTieBreak) {
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
      }
      checkAndAddNewSet(game, score);
      score.setTieBreakPointA(null);
      score.setTieBreakPointB(null);
    }
  }

  // --- Jeu Standard ---

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

  // --- Helpers ---

  private boolean isSetFinished(Game game, SetScore set) {
    // 1. Victoire aux points (Tie-Break fini)
    // Note: Comme on update setTieBreakTeamA en live maintenant, on doit vérifier si le set est GAGNÉ
    // et pas juste s'il a des points.
    // MAIS pour la compatibilité avec l'ancien code, on suppose que si set.setTeamAScore a changé (7-6 ou 1-0), c'est fini.
    // Ou on utilise la logique stricte :

    // Super TB
    if (game.getFormat() != null && game.getFormat().isSuperTieBreakInFinalSet()) {
      if ((set.getTeamAScore() == 1 && set.getTeamBScore() == 0) || (set.getTeamAScore() == 0 && set.getTeamBScore() == 1)) {
        if (isSuperTieBreakSetContext(game, set)) {
          return true;
        }
      }
    }

    // Standard Set
    return isSetWin(set.getTeamAScore(), set.getTeamBScore()) || isSetWin(set.getTeamBScore(), set.getTeamAScore());
  }

  private boolean isSetWin(int winner, int loser) {
    // 6-4, 6-0, 7-5, 7-6
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
    score.setCurrentGamePointA(null);
    score.setCurrentGamePointB(null);
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
}