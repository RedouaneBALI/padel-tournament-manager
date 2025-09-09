package io.github.redouanebali.generation.strategy;

import io.github.redouanebali.generation.util.ByePlacementUtil;
import io.github.redouanebali.generation.util.RandomPlacementUtil;
import io.github.redouanebali.generation.util.SeedPlacementUtil;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutomaticDrawStrategy implements DrawStrategy {

  @Override
  public void placePlayers(Tournament tournament, List<PlayerPair> players) {
    if (tournament == null || players == null || players.isEmpty()) {
      return;
    }

    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    fillInitialRoundsAutomatic(tournament, players);
  }

  /**
   * Automatically fills the initial rounds of the tournament with logic moved from TournamentBuilder.
   */
  private void fillInitialRoundsAutomatic(Tournament tournament, List<PlayerPair> allPairs) {
    List<Round> rounds = tournament.getRounds();

    // Identify initial rounds (Q1, first main draw round, etc.)
    for (Round round : rounds) {
      Stage stage = round.getStage();

      // Process only initial rounds
      if (isInitialRound(stage)) {
        processInitialRound(tournament, round, allPairs);
      }
    }
  }

  /**
   * Determines if a stage represents an initial round where players enter.
   */
  private boolean isInitialRound(Stage stage) {
    return stage == Stage.Q1 ||
           stage == Stage.GROUPS ||
           stage == Stage.R64 ||
           stage == Stage.R32 ||
           stage == Stage.R16 ||
           stage == Stage.QUARTERS ||
           stage == Stage.SEMIS ||
           stage == Stage.FINAL;
  }

  /**
   * Processes an initial round with automatic logic.
   */
  private void processInitialRound(Tournament tournament, Round initialRound, List<PlayerPair> allPairs) {
    int   roundDrawSize   = initialRound.getGames().size() * 2;
    int   configuredSeeds = tournament.getConfig().getNbSeeds();
    Stage stage           = initialRound.getStage();

    if (stage.isQualification()) {
      processQualificationRound(tournament, initialRound, allPairs, roundDrawSize);
    } else {
      processMainDrawRound(tournament, initialRound, allPairs, roundDrawSize, configuredSeeds);
    }
  }

  /**
   * Processes a qualification round with automatic logic.
   */
  private void processQualificationRound(Tournament tournament, Round initialRound, List<PlayerPair> allPairs, int qualifDrawSize) {
    int nbSeedsQualif = tournament.getConfig().getNbSeedsQualify();

    // Calculate how many teams participate in qualifs vs direct entry to main draw
    int totalSlotsInMainDraw    = tournament.getConfig().getMainDrawSize();
    int nbQualifiers            = tournament.getConfig().getNbQualifiers();
    int directEntriesToMainDraw = totalSlotsInMainDraw - nbQualifiers;

    // Teams that go directly to main draw (best ranked)
    int teamsNotInQualifs = Math.min(directEntriesToMainDraw, allPairs.size());

    // Remaining teams that compete in qualifs
    int teamsAvailableForQualifs = Math.max(0, allPairs.size() - teamsNotInQualifs);
    int byesNeededInQualifs      = Math.max(0, qualifDrawSize - teamsAvailableForQualifs);

    // Determine teams to protect with BYEs in qualifs
    int teamsToProtectInQualifs = Math.max(nbSeedsQualif, byesNeededInQualifs);

    // Get teams that participate in qualifs
    List<PlayerPair> qualifParticipants = new ArrayList<>();
    int              startIndex         = Math.max(0, allPairs.size() - teamsAvailableForQualifs);
    for (int i = startIndex; i < allPairs.size(); i++) {
      qualifParticipants.add(allPairs.get(i));
    }

    // Place seeds in qualifs (among qualif participants only)
    if (nbSeedsQualif > 0 && !qualifParticipants.isEmpty()) {
      SeedPlacementUtil.placeSeedTeams(initialRound, qualifParticipants, nbSeedsQualif, qualifDrawSize);
    }

    // Place BYEs in qualifs if necessary
    if (byesNeededInQualifs > 0) {
      ByePlacementUtil.placeByeTeams(initialRound, teamsAvailableForQualifs, nbSeedsQualif, qualifDrawSize);
    }

    // Place remaining qualif participants randomly
    List<PlayerPair> remainingQualifTeams = new ArrayList<>();
    for (int i = teamsToProtectInQualifs; i < qualifParticipants.size(); i++) {
      remainingQualifTeams.add(qualifParticipants.get(i));
    }
    RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingQualifTeams);
  }

  /**
   * Processes a main draw round with automatic logic.
   */
  private void processMainDrawRound(Tournament tournament, Round initialRound, List<PlayerPair> allPairs, int roundDrawSize, int configuredSeeds) {
    // Place configured seeds in their appropriate positions
    SeedPlacementUtil.placeSeedTeams(initialRound, allPairs, configuredSeeds, roundDrawSize);

    // Place BYEs opposite protected teams
    ByePlacementUtil.placeByeTeams(initialRound, allPairs.size(), configuredSeeds, roundDrawSize);

    // Filter already placed teams (seeds and those protected with BYEs)
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game g : initialRound.getGames()) {
      if (g.getTeamA() != null && !g.getTeamA().isBye()) {
        alreadyPlaced.add(g.getTeamA());
      }
      if (g.getTeamB() != null && !g.getTeamB().isBye()) {
        alreadyPlaced.add(g.getTeamB());
      }
    }

    List<PlayerPair> remainingTeams = allPairs.stream()
                                              .filter(p -> !alreadyPlaced.contains(p))
                                              .toList();

    // Place remaining teams in available slots
    RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingTeams);
  }
}
