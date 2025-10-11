package io.github.redouanebali.generation.util.propagation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic strategy: propagates winners in non-classic cases. Used when the bracket structure is not strictly binary, for example for qualification
 * rounds, lucky losers, or any round where the number of matches in the next round does not match half of the current round.
 */
@Slf4j
@RequiredArgsConstructor
public class QualifierSlotPropagationStrategy implements PropagationStrategy {

  private final WinnerPropagationUtil util;

  // Cache to store initial QUALIFIER slots per games list
  private List<QualifierSlot> cachedQualifierSlots = null;
  private List<Game>          cachedNextGames      = null;

  @Override
  public boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner) {
    // CORRECTION: Collect QUALIFIER slots ONLY ONCE at the beginning
    // so that the match index always corresponds to the same slot

    // If it's a new round, recalculate the slots
    if (cachedNextGames != nextGames) {
      cachedNextGames      = nextGames;
      cachedQualifierSlots = collectInitialQualifierSlots(nextGames);

      log.debug("[QualifierSlotPropagationStrategy] Initialized with {} qualifier slots for round",
                cachedQualifierSlots.size());
    }

    // Check that the match index is valid
    if (currentGameIndex < 0 || currentGameIndex >= cachedQualifierSlots.size()) {
      return util.placeWinnerInQualifierOrAvailableSlot(nextGames, winner);
    }

    // Place the winner in the corresponding Nth QUALIFIER slot
    QualifierSlot targetSlot = cachedQualifierSlots.get(currentGameIndex);
    Game          targetGame = nextGames.get(targetSlot.gameIndex);

    // Check that the slot is still a QUALIFIER (in case it was modified)
    PlayerPair currentTeam = targetSlot.isTeamA ? targetGame.getTeamA() : targetGame.getTeamB();
    if (currentTeam == null || currentTeam.getType() != PairType.QUALIFIER) {
      log.debug("[QualifierSlotPropagationStrategy] WARNING: Slot Q{} (Game[{}].{}) is no longer a QUALIFIER, using fallback",
                currentGameIndex + 1, targetSlot.gameIndex, targetSlot.isTeamA ? "TeamA" : "TeamB");
      return util.placeWinnerInQualifierOrAvailableSlot(nextGames, winner);
    }

    log.debug("[QualifierSlotPropagationStrategy] Placing winner (seed {}) in slot Q{} -> Game[{}].{}",
              winner.getSeed(), currentGameIndex + 1, targetSlot.gameIndex, targetSlot.isTeamA ? "TeamA" : "TeamB");

    if (targetSlot.isTeamA) {
      targetGame.setTeamA(winner);
    } else {
      targetGame.setTeamB(winner);
    }

    return true;
  }

  /**
   * Collects all initial QUALIFIER slots in order
   */
  private List<QualifierSlot> collectInitialQualifierSlots(List<Game> nextGames) {
    List<QualifierSlot> qualifierSlots = new ArrayList<>();
    for (int i = 0; i < nextGames.size(); i++) {
      Game game = nextGames.get(i);

      if (game.getTeamA() != null && game.getTeamA().getType() == PairType.QUALIFIER) {
        qualifierSlots.add(new QualifierSlot(i, true)); // gameIndex, isTeamA
      }
      if (game.getTeamB() != null && game.getTeamB().getType() == PairType.QUALIFIER) {
        qualifierSlots.add(new QualifierSlot(i, false)); // gameIndex, isTeamB
      }
    }
    return qualifierSlots;
  }

  /**
   * Represents a QUALIFIER slot in a game of the next round
   */
  private record QualifierSlot(int gameIndex, boolean isTeamA) {

  }
}
