package io.github.redouanebali.generation.draw;

import io.github.redouanebali.generation.util.ByePlacementUtil;
import io.github.redouanebali.generation.util.RandomPlacementUtil;
import io.github.redouanebali.generation.util.SeedPlacementUtil;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Find the first qualification round (Q1) and first main draw round
    Round firstQualifRound   = null;
    Round firstMainDrawRound = null;

    for (Round round : rounds) {
      Stage stage = round.getStage();

      // Find Q1 (first qualification round)
      if ((stage == Stage.Q1 || stage == Stage.GROUPS) && firstQualifRound == null) {
        firstQualifRound = round;
      }

      // Find first main draw round (largest stage present)
      if (!stage.isQualification() && stage != Stage.GROUPS && firstMainDrawRound == null) {
        firstMainDrawRound = round;
      }
    }

    // Process Q1 or Groups if present
    if (firstQualifRound != null) {
      processInitialRound(tournament, firstQualifRound, allPairs);
    }

    // Process first main draw round if present
    if (firstMainDrawRound != null) {
      processInitialRound(tournament, firstMainDrawRound, allPairs);
    }
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
    } else if (stage == Stage.GROUPS) {
      processGroupsRound(tournament, initialRound, allPairs);
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

    // Determine teams to protect with BYES in qualifs
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
   * Répartit les joueurs dans nbPools groupes et génère les matchs de poule (round robin) dans le round GROUPS.
   */
  private void processGroupsRound(Tournament tournament, Round round, List<PlayerPair> allPairs) {
    int nbPools           = tournament.getConfig().getNbPools();
    int nbPairsPerPool    = tournament.getConfig().getNbPairsPerPool();
    int nbQualifiedByPool = tournament.getConfig().getNbQualifiedByPool();
    if (nbPools <= 0 || nbPairsPerPool <= 0 || allPairs == null || allPairs.isEmpty()) {
      return;
    }
    // Sépare les seeds des autres paires
    List<PlayerPair> seeds  = new ArrayList<>();
    List<PlayerPair> others = new ArrayList<>();
    for (PlayerPair pair : allPairs) {
      if (pair.getSeed() > 0) {
        seeds.add(pair);
      } else {
        others.add(pair);
      }
    }
    // Placement des seeds dans les pools selon le snake officiel
    List<List<PlayerPair>> pools = SeedPlacementUtil.placeSeedsInPoolsSnake(seeds, nbPools);
    // Complète les pools avec les autres paires
    int idx = 0;
    for (int p = 0; p < nbPools; p++) {
      while (pools.get(p).size() < nbPairsPerPool && idx < others.size()) {
        pools.get(p).add(others.get(idx));
        idx++;
      }
    }
    // Affectation des paires aux pools du round
    assignPairsToPools(round, pools);
    // Génération des matchs round robin pour chaque poule
    List<Game> games = new ArrayList<>();
    for (List<PlayerPair> pool : pools) {
      int n = pool.size();
      for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
          Game g = new Game();
          g.setTeamA(pool.get(i));
          g.setTeamB(pool.get(j));
          g.setFormat(round.getMatchFormat());
          games.add(g);
        }
      }
    }
    round.getGames().clear();
    round.getGames().addAll(games);
  }

  /**
   * Remplit les pools du round avec les paires affectées.
   */
  private void assignPairsToPools(Round round, List<List<PlayerPair>> poolsPairs) {
    round.getPools().clear();
    char poolName = 'A';
    for (List<PlayerPair> pairs : poolsPairs) {
      Pool pool = new Pool("Pool " + poolName, pairs);
      round.getPools().add(pool);
      poolName++;
    }
  }

  /**
   * Processes a main draw round with automatic logic.
   */
  private void processMainDrawRound(Tournament tournament, Round initialRound, List<PlayerPair> allPairs, int roundDrawSize, int configuredSeeds) {
    // Step 1: Place seeds
    SeedPlacementUtil.placeSeedTeams(initialRound, allPairs, configuredSeeds, roundDrawSize);

    // Step 2: Place BYEs for direct entries only, skipping qualifier slots
    int nbQualifiers  = tournament.getConfig().getNbQualifiers();
    int totalSlots    = tournament.getConfig().getMainDrawSize();
    int directEntries = Math.min(totalSlots - nbQualifiers, allPairs.size());
    ByePlacementUtil.placeByeTeams(initialRound, directEntries, configuredSeeds, roundDrawSize, nbQualifiers);

    // Step 3: Place qualifiers randomly in available slots
    RandomPlacementUtil.placeQualifiers(initialRound, nbQualifiers);

    // Step 4: Place remaining teams randomly in available slots
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game g : initialRound.getGames()) {
      if (g.getTeamA() != null && !g.getTeamA().isBye() && !g.getTeamA().isQualifier()) {
        alreadyPlaced.add(g.getTeamA());
      }
      if (g.getTeamB() != null && !g.getTeamB().isBye() && !g.getTeamB().isQualifier()) {
        alreadyPlaced.add(g.getTeamB());
      }
    }
    List<PlayerPair> remainingTeams = allPairs.stream()
                                              .filter(p -> !alreadyPlaced.contains(p))
                                              .limit(directEntries)
                                              .collect(Collectors.toList());
    RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingTeams);
  }

  /**
   * Calculates how many teams actually participate in the main draw. This accounts for teams that go to qualifications vs direct entry.
   */
  private int calculateMainDrawParticipants(Tournament tournament, List<PlayerPair> allPairs) {
    if (tournament.getConfig().getFormat() == TournamentFormat.KNOCKOUT) {
      return tournament.getConfig().getMainDrawSize();
    }
    int totalSlots   = tournament.getConfig().getMainDrawSize();
    int nbQualifiers = tournament.getConfig().getNbQualifiers();

    if (nbQualifiers > 0) {
      // Tournament has qualifications
      // Direct entries = total slots - qualifier spots
      int directEntries = totalSlots - nbQualifiers;
      return Math.min(directEntries, allPairs.size());
    } else {
      // No qualifications, all teams go to main draw
      return Math.min(allPairs.size(), totalSlots);
    }
  }
}
