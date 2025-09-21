package io.github.redouanebali.generation.draw;

import io.github.redouanebali.generation.util.RandomPlacementUtil;
import io.github.redouanebali.generation.util.TournamentStageUtil;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public class ManualDrawStrategy implements DrawStrategy {

  @Override
  public void placePlayers(Tournament tournament, List<PlayerPair> players) {
    // In manual mode, we simply place players in the provided order
    // in the initial rounds of the tournament
    if (tournament == null || tournament.getRounds() == null) {
      return;
    }
    fillInitialRoundsManual(tournament, players);
  }

  /**
   * Fills initial rounds in manual mode. Logic moved from TournamentBuilder.fillInitialRoundsManual()
   */
  private void fillInitialRoundsManual(Tournament tournament, List<PlayerPair> players) {
    if (players == null || players.isEmpty()) {
      return;
    }

    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    // In manual mode, we place players in order in the first rounds
    // (Q1 and first main draw round)
    int playerIndex = 0;

    for (Round round : rounds) {
      Stage stage = round.getStage();

      // Process only initial rounds where players enter
      if (isInitialRound(stage)) {
        playerIndex = fillRoundWithPlayers(round, players, playerIndex);

        // If all players are placed, stop
        if (playerIndex >= players.size()) {
          break;
        }
      }
    }
  }

  /**
   * Determines if a stage is an initial round where players enter.
   */
  private boolean isInitialRound(Stage stage) {
    return TournamentStageUtil.isInitialRound(stage);
  }

  /**
   * Determines if this is the first main draw round.
   */
  private boolean isFirstMainDrawRound(Stage stage) {
    return TournamentStageUtil.isFirstMainDrawStage(stage);
  }

  /**
   * Fills a round with players in order.
   *
   * @param round The round to fill
   * @param players The complete list of players
   * @param startIndex The starting index in the player list
   * @return The new index after placement
   */
  private int fillRoundWithPlayers(Round round, List<PlayerPair> players, int startIndex) {
    // Extract the sub-list of players to place in this round
    List<PlayerPair> playersForThisRound = players.subList(startIndex, Math.min(players.size(),
                                                                                startIndex + (round.getGames().size() * 2)));

    // Use utility to place in order (without shuffling)
    RandomPlacementUtil.placeTeamsInOrder(round, playersForThisRound);

    return startIndex + playersForThisRound.size();
  }

  /**
   * Alternative to replace existing rounds with predefined rounds (original logic from fillInitialRoundsManual).
   *
   * @param tournament The tournament to modify
   * @param predefinedRounds The predefined rounds by the user
   */
  public void replaceInitialRounds(Tournament tournament, List<Round> predefinedRounds) {
    if (predefinedRounds == null || predefinedRounds.isEmpty()) {
      return;
    }

    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    // Replace initial rounds with those provided by the user
    // Assuming predefinedRounds[0] = Q1, predefinedRounds[1] = first main draw round
    int replaced = 0;
    for (int i = 0; i < rounds.size() && replaced < predefinedRounds.size(); i++) {
      Round currentRound  = rounds.get(i);
      Round providedRound = predefinedRounds.get(replaced);

      // Replace Q1 if stage matches
      if (replaced == 0 && currentRound.getStage().name().equals(providedRound.getStage().name())) {
        rounds.set(i, providedRound);
        replaced++;
      }
      // Replace first main draw round
      else if (replaced == 1 && currentRound.getStage().name().equals(providedRound.getStage().name())) {
        rounds.set(i, providedRound);
        replaced++;
      }
    }
  }
}