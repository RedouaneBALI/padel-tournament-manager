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

  // Reusable strategy instances to preserve state (cache) across round boundaries
  private final KnockoutPropagationStrategy      knockoutStrategy;
  private final QualifierSlotPropagationStrategy qualifierStrategy;

  public WinnerPropagationUtil() {
    this.knockoutStrategy  = new KnockoutPropagationStrategy(this);
    this.qualifierStrategy = new QualifierSlotPropagationStrategy(this);
  }

  /**
   * Propagate winners from all rounds to their next rounds
   */
  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    // Reset qualifier strategy cache for new propagation pass
    qualifierStrategy.resetCache();

    final List<Round> rounds = tournament.getRounds();
    // Process every boundary left-to-right so earlier rounds feed later ones in the same call
    for (int i = 0; i < rounds.size() - 1; i++) {
      propagateFromRoundToNext(rounds.get(i), rounds.get(i + 1));
    }
  }

  /**
   * Optimized propagation: only propagate from the round containing the given game onwards. Finds the round index of the game, then propagates from
   * that round to the end.
   */
  public void propagateWinnersFromGame(Tournament tournament, Game game) {
    if (tournament == null || game == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    qualifierStrategy.resetCache();

    final List<Round> rounds     = tournament.getRounds();
    int               startIndex = findRoundIndexContainingGame(rounds, game);
    if (startIndex == -1) {
      return; // Game not found, nothing to propagate
    }

    // Propagate from the found round onwards
    for (int i = startIndex; i < rounds.size() - 1; i++) {
      propagateFromRoundToNext(rounds.get(i), rounds.get(i + 1));
    }
  }

  /**
   * Find the index of the round containing the given game
   */
  private int findRoundIndexContainingGame(List<Round> rounds, Game game) {
    for (int i = 0; i < rounds.size(); i++) {
      Round round = rounds.get(i);
      if (round.getGames() != null && round.getGames().contains(game)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Propagate winners from one round to the next Réinitialise d'abord les slots dépendants, puis propage les vainqueurs.
   */
  private void propagateFromRoundToNext(Round currentRound, Round nextRound) {
    if (!canPropagateRounds(currentRound, nextRound)) {
      return;
    }

    final List<Game> curGames  = currentRound.getGames();
    final List<Game> nextGames = nextRound.getGames();

    if (isCurrentRoundEmpty(curGames)) {
      return;
    }

    PropagationStrategy strategy = determinePropagationStrategy(curGames.size(), nextGames.size());

    resetNextRoundSlots(strategy, curGames, nextGames);
    propagateWinnersToNextRound(strategy, curGames, nextGames);
  }

  /**
   * Check if rounds can be propagated
   */
  private boolean canPropagateRounds(Round currentRound, Round nextRound) {
    if (currentRound == null || nextRound == null) {
      return false;
    }

    final List<Game> curGames  = currentRound.getGames();
    final List<Game> nextGames = nextRound.getGames();

    return curGames != null && nextGames != null && !nextGames.isEmpty();
  }

  /**
   * Check if current round is empty
   */
  private boolean isCurrentRoundEmpty(List<Game> curGames) {
    return curGames.stream().allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
  }

  /**
   * Reset slots in next round based on strategy
   */
  private void resetNextRoundSlots(PropagationStrategy strategy, List<Game> curGames, List<Game> nextGames) {
    if (strategy instanceof KnockoutPropagationStrategy) {
      resetKnockoutSlots(curGames, nextGames);
    }
  }

  /**
   * Reset knockout slots
   */
  private void resetKnockoutSlots(List<Game> curGames, List<Game> nextGames) {
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

  /**
   * Propagate all winners to next round
   */
  private void propagateWinnersToNextRound(PropagationStrategy strategy, List<Game> curGames, List<Game> nextGames) {
    for (int i = 0; i < curGames.size(); i++) {
      PlayerPair winner = determineWinner(curGames.get(i));
      strategy.placeWinner(nextGames, i, winner);
    }
  }

  /**
   * Determine winner of a game (handles BYE vs BYE case)
   */
  private PlayerPair determineWinner(Game currentGame) {
    PlayerPair winner = currentGame.getWinner();
    if (winner == null && isByeVsBye(currentGame)) {
      winner = PlayerPair.bye();
    }
    return winner;
  }

  /**
   * Check if game is BYE vs BYE
   */
  private boolean isByeVsBye(Game game) {
    boolean teamABye = game.getTeamA() != null && game.getTeamA().isBye();
    boolean teamBBye = game.getTeamB() != null && game.getTeamB().isBye();
    return teamABye && teamBBye;
  }

  /**
   * Determine the propagation strategy based on round sizes
   */
  private PropagationStrategy determinePropagationStrategy(int currentSize, int nextSize) {
    if (nextSize == currentSize / 2) {
      return knockoutStrategy;
    } else {
      return qualifierStrategy;
    }
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

}
