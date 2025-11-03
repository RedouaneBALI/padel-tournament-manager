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


  @Override
  public boolean placeWinner(List<Game> nextGames, int currentGameIndex, PlayerPair winner) {
    // Find the SPECIFIC qualifier with number = currentGameIndex + 1
    // For example: match 0 -> find Q1, match 1 -> find Q2, etc.
    // This ensures each match always targets its corresponding qualifier number

    int           targetQualifierNumber = currentGameIndex + 1;
    QualifierSlot targetSlot            = findQualifierByNumber(nextGames, targetQualifierNumber);

    if (targetSlot == null) {
      // Qualifier not found (already replaced or doesn't exist)
      if (winner != null) {
        log.debug("[QualifierSlotPropagationStrategy] Q{} not found (already replaced), skipping", targetQualifierNumber);
      }
      return true;
    }

    Game targetGame = nextGames.get(targetSlot.gameIndex);

    // Si winner est null, garder le QUALIFIER tel quel (ne rien faire)
    if (winner == null) {
      log.debug("[QualifierSlotPropagationStrategy] No winner for match {}, keeping Q{} as is",
                currentGameIndex, targetQualifierNumber);
      return true;
    }

    // Place the winner
    log.debug("[QualifierSlotPropagationStrategy] Placing winner (seed {}) in Q{} -> Game[{}].{}",
              winner.getSeed(), targetQualifierNumber, targetSlot.gameIndex, targetSlot.isTeamA ? "TeamA" : "TeamB");
    if (targetSlot.isTeamA) {
      targetGame.setTeamA(winner);
    } else {
      targetGame.setTeamB(winner);
    }
    return true;
  }

  /**
   * Find a qualifier by its specific number (e.g., Q1, Q2, Q3) Returns null if the qualifier is not found (already replaced)
   */
  private QualifierSlot findQualifierByNumber(List<Game> nextGames, int qualifierNumber) {
    String targetName = "Q" + qualifierNumber;

    for (int i = 0; i < nextGames.size(); i++) {
      Game game = nextGames.get(i);

      // Check teamA
      if (game.getTeamA() != null && game.getTeamA().isQualifier()) {
        String qualifierName = game.getTeamA().getPlayer1() != null
                               ? game.getTeamA().getPlayer1().getName()
                               : "Q";
        if (targetName.equals(qualifierName)) {
          return new QualifierSlot(i, true);
        }
      }

      // Check teamB
      if (game.getTeamB() != null && game.getTeamB().isQualifier()) {
        String qualifierName = game.getTeamB().getPlayer1() != null
                               ? game.getTeamB().getPlayer1().getName()
                               : "Q";
        if (targetName.equals(qualifierName)) {
          return new QualifierSlot(i, false);
        }
      }
    }

    return null; // Qualifier not found
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
