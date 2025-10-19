package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

/**
 * Utility for propagating winners between tournament rounds.
 */
public class WinnerPropagationUtil {

  /**
   * Propagate winners from all rounds to their next rounds
   */
  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }
    final List<Round> rounds = tournament.getRounds();
    // Process every boundary left-to-right so earlier rounds feed later ones in the same call
    for (int i = 0; i < rounds.size() - 1; i++) {
      propagateFromRoundToNext(rounds.get(i), rounds.get(i + 1));
    }
  }

  /**
   * Propagate winners from one round to the next Réinitialise d'abord les slots dépendants, puis propage les vainqueurs.
   */
  private void propagateFromRoundToNext(Round currentRound, Round nextRound) {
    if (currentRound == null || nextRound == null) {
      return;
    }

    final List<Game> curGames  = currentRound.getGames();
    final List<Game> nextGames = nextRound.getGames();
    if (curGames == null || nextGames == null || nextGames.isEmpty()) {
      return;
    }

    final boolean currentEmpty = curGames.stream().allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
    if (currentEmpty) {
      return;
    }

    PropagationStrategy strategy = determinePropagationStrategy(curGames.size(), nextGames.size());

    // --- Knockout : réinitialiser tous les slots du tour suivant ---
    if (strategy instanceof KnockoutPropagationStrategy) {
      for (int i = 0; i < curGames.size(); i++) {
        int idx = i / 2;
        if (idx < nextGames.size()) {
          Game ng = nextGames.get(idx);
          if (i % 2 == 0) {
            ng.setTeamA(null);
          } else {
            ng.setTeamB(null);
          }
        }
      }
    }
    // --- Qualifier : réinitialiser tous les slots QUALIFIER ---
    if (strategy instanceof QualifierSlotPropagationStrategy) {
      for (Game ng : nextGames) {
        if (ng.getTeamA() != null && ng.getTeamA().getType() == PairType.QUALIFIER) {
          ng.setTeamA(null);
        }
        if (ng.getTeamB() != null && ng.getTeamB().getType() == PairType.QUALIFIER) {
          ng.setTeamB(null);
        }
      }
    }

    for (int i = 0; i < curGames.size(); i++) {
      final Game currentGame = curGames.get(i);
      PlayerPair winner      = currentGame.getWinner();
      if (winner == null) {
        boolean teamABye = currentGame.getTeamA() != null && currentGame.getTeamA().isBye();
        boolean teamBBye = currentGame.getTeamB() != null && currentGame.getTeamB().isBye();
        if (teamABye && teamBBye) {
          winner = PlayerPair.bye();
        }
      }
      strategy.placeWinner(nextGames, i, winner);
    }
  }

  /**
   * Vérifie si une équipe du tour suivant dépend d'un match du tour courant (par référence).
   */
  private boolean isDependentOnCurrentRound(PlayerPair team, List<Game> curGames) {
    if (team == null) {
      return false;
    }
    for (Game g : curGames) {
      if (team == g.getWinner()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine the propagation strategy based on round sizes
   */
  private PropagationStrategy determinePropagationStrategy(int currentSize, int nextSize) {
    if (nextSize == currentSize / 2) {
      return new KnockoutPropagationStrategy(this);
    } else {
      return new QualifierSlotPropagationStrategy(this);
    }
  }

  /**
   * Check if a winner should be propagated
   */
  private boolean shouldPropagateWinner(PlayerPair winner, Game currentGame, List<Game> nextGames) {
    // Skip if no winner determined
    if (winner == null) {
      return false;
    }

    // Special case: BYE vs BYE should propagate a BYE
    boolean teamABye   = currentGame.getTeamA() != null && currentGame.getTeamA().isBye();
    boolean teamBBye   = currentGame.getTeamB() != null && currentGame.getTeamB().isBye();
    boolean isByeVsBye = teamABye && teamBBye;

    // Skip BYE winners unless it's from a BYE vs BYE match
    if (winner.isBye() && !isByeVsBye) {
      return false;
    }

    // Avoid duplicate placement
    return !isAlreadyAssignedInNextByReference(nextGames, winner);
  }

  /**
   * Try to place winner into a given slot. Package-private for strategy classes.
   */
  boolean assignWinnerToSlot(Game game, boolean sideA, PlayerPair winner) {
    PlayerPair current = sideA ? game.getTeamA() : game.getTeamB();
    if (current == null || (current.getType() == PairType.QUALIFIER)) {
      if (sideA) {
        game.setTeamA(winner);
      } else {
        game.setTeamB(winner);
      }
      return true;
    }
    return false;
  }

  /**
   * Single-pass fallback: scan nextGames once and place into the first QUALIFIER placeholder; if none, the first null. Package-private for strategy
   * classes.
   */
  boolean placeWinnerInQualifierOrAvailableSlot(List<Game> nextGames, PlayerPair winner) {
    // First pass: QUALIFIER placeholders
    for (Game nextGame : nextGames) {
      if (nextGame.getTeamA() != null && nextGame.getTeamA().getType() == PairType.QUALIFIER) {
        nextGame.setTeamA(winner);
        return true;
      }
      if (nextGame.getTeamB() != null && nextGame.getTeamB().getType() == PairType.QUALIFIER) {
        nextGame.setTeamB(winner);
        return true;
      }
    }
    // Second pass: first null slot
    for (Game ng : nextGames) {
      if (ng.getTeamA() == null) {
        ng.setTeamA(winner);
        return true;
      }
      if (ng.getTeamB() == null) {
        ng.setTeamB(winner);
        return true;
      }
    }
    return false;
  }

  /**
   * Checks by reference (==) if the winner is already assigned anywhere in nextGames.
   */
  private boolean isAlreadyAssignedInNextByReference(List<Game> nextGames, PlayerPair winner) {
    for (Game g : nextGames) {
      if (g.getTeamA() == winner || g.getTeamB() == winner) {
        return true;
      }
    }
    return false;
  }
}
