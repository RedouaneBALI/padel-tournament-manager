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

    Round firstQualifRound   = findFirstQualificationRound(rounds);
    Round firstMainDrawRound = findFirstMainDrawRound(rounds);

    // Process Q1 or Groups if present and collect teams placed in qualification
    Set<PlayerPair> teamsPlacedInQualif = new HashSet<>();
    if (firstQualifRound != null) {
      processInitialRound(tournament, firstQualifRound, allPairs);
      teamsPlacedInQualif = collectTeamsPlacedInRound(firstQualifRound);
    }

    // Process first main draw round if present
    if (firstMainDrawRound != null) {
      final Set<PlayerPair> excludedTeams = teamsPlacedInQualif; // Make it effectively final
      List<PlayerPair> pairsForMainDraw = allPairs.stream()
                                                  .filter(pair -> !excludedTeams.contains(pair))
                                                  .collect(Collectors.toList());

      processInitialRound(tournament, firstMainDrawRound, pairsForMainDraw);
    }
  }

  /**
   * Finds the first qualification round (Q1 or GROUPS).
   */
  private Round findFirstQualificationRound(List<Round> rounds) {
    for (Round round : rounds) {
      Stage stage = round.getStage();
      if (stage == Stage.Q1 || stage == Stage.GROUPS) {
        return round;
      }
    }
    return null;
  }

  /**
   * Finds the first main draw round.
   */
  private Round findFirstMainDrawRound(List<Round> rounds) {
    for (Round round : rounds) {
      Stage stage = round.getStage();
      if (!stage.isQualification() && stage != Stage.GROUPS) {
        return round;
      }
    }
    return null;
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

    // CORRECTION: Lower-ranked teams must go through qualifications
    // Calculate how many teams MUST go to qualifs (fill the qualification draw to maximum)
    int teamsForQualifs = Math.min(qualifDrawSize, allPairs.size());

    // If we have more teams than qualification slots, the best teams go directly to main draw
    int teamsGoingDirectToMainDraw = Math.max(0, allPairs.size() - qualifDrawSize);

    // Teams participating in qualifs are the LAST ones in the list (lower-ranked)
    List<PlayerPair> qualifParticipants = new ArrayList<>();
    int              startIndex         = teamsGoingDirectToMainDraw;
    for (int i = startIndex; i < allPairs.size(); i++) {
      qualifParticipants.add(allPairs.get(i));
    }

    // Calculate necessary BYEs in qualif
    int byesNeededInQualifs = Math.max(0, qualifDrawSize - qualifParticipants.size());

    // Place seeds in qualifs (among qualif participants only)
    // Note: nbSeedsQualif représente le nombre de têtes de série PARMI les participants aux qualifs
    if (nbSeedsQualif > 0 && !qualifParticipants.isEmpty()) {
      SeedPlacementUtil.placeSeedTeams(initialRound, qualifParticipants, nbSeedsQualif, qualifDrawSize);
    }

    // Place BYEs in qualifs if necessary
    if (byesNeededInQualifs > 0) {
      ByePlacementUtil.placeByeTeams(initialRound, qualifParticipants.size(), nbSeedsQualif, qualifDrawSize);
    }

    // Place remaining qualif participants randomly
    // On doit placer TOUTES les équipes qui ne sont pas déjà placées (seeds + BYEs)
    List<PlayerPair> remainingQualifTeams = new ArrayList<>();

    // Trouver quelles équipes sont déjà placées dans le round
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game game : initialRound.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        alreadyPlaced.add(game.getTeamA());
      }
      if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        alreadyPlaced.add(game.getTeamB());
      }
    }

    // Ajouter toutes les équipes qui ne sont PAS encore placées
    for (PlayerPair pair : qualifParticipants) {
      if (!alreadyPlaced.contains(pair)) {
        remainingQualifTeams.add(pair);
      }
    }

    RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingQualifTeams);
  }

  /**
   * Répartit les joueurs dans nbPools groupes et génère les matchs de poule (round robin) dans le round GROUPS.
   */
  private void processGroupsRound(Tournament tournament, Round round, List<PlayerPair> allPairs) {
    if (!isValidGroupsConfiguration(tournament, allPairs)) {
      return;
    }

    int nbPools        = tournament.getConfig().getNbPools();
    int nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();

    // Sépare les seeds des autres paires
    List<PlayerPair> seeds  = new ArrayList<>();
    List<PlayerPair> others = new ArrayList<>();
    separateSeedsFromOthers(allPairs, seeds, others);

    // Placement des seeds dans les pools selon le snake officiel
    List<List<PlayerPair>> pools = SeedPlacementUtil.placeSeedsInPoolsSnake(seeds, nbPools);

    // Complète les pools avec les autres paires
    fillPoolsWithRemainingTeams(pools, others, nbPairsPerPool);

    // Affectation des paires aux pools du round
    assignPairsToPools(round, pools);

    // Génération des matchs round robin pour chaque poule
    generateRoundRobinMatches(round, pools);
  }

  /**
   * Validates the configuration for groups processing.
   */
  private boolean isValidGroupsConfiguration(Tournament tournament, List<PlayerPair> allPairs) {
    int nbPools        = tournament.getConfig().getNbPools();
    int nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();

    return nbPools > 0 && nbPairsPerPool > 0 && allPairs != null && !allPairs.isEmpty();
  }

  /**
   * Separates player pairs into seeds and others.
   */
  private void separateSeedsFromOthers(List<PlayerPair> allPairs, List<PlayerPair> seeds, List<PlayerPair> others) {
    for (PlayerPair pair : allPairs) {
      if (pair.getSeed() > 0) {
        seeds.add(pair);
      } else {
        others.add(pair);
      }
    }
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
   * Fills pools with remaining non-seeded teams.
   */
  private void fillPoolsWithRemainingTeams(List<List<PlayerPair>> pools, List<PlayerPair> others, int nbPairsPerPool) {
    int idx = 0;
    for (int p = 0; p < pools.size(); p++) {
      while (pools.get(p).size() < nbPairsPerPool && idx < others.size()) {
        pools.get(p).add(others.get(idx));
        idx++;
      }
    }
  }

  /**
   * Generates round robin matches for all pools.
   */
  private void generateRoundRobinMatches(Round round, List<List<PlayerPair>> pools) {
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
   * Processes a main draw round with automatic logic.
   */
  private void processMainDrawRound(Tournament tournament, Round initialRound, List<PlayerPair> allPairs, int roundDrawSize, int configuredSeeds) {
    TournamentFormat format = tournament.getConfig().getFormat();

    // Step 1: Place seeds
    SeedPlacementUtil.placeSeedTeams(initialRound, allPairs, configuredSeeds, roundDrawSize);

    // Step 2: Place BYEs and qualifiers based on tournament format
    if (format == TournamentFormat.QUALIF_KO) {
      // QUALIFS_KO mode: Place BYEs for direct entries only, skipping qualifier slots
      int nbQualifiers  = tournament.getConfig().getNbQualifiers();
      int totalSlots    = tournament.getConfig().getMainDrawSize();
      int directEntries = Math.min(totalSlots - nbQualifiers, allPairs.size());
      ByePlacementUtil.placeByeTeams(initialRound, directEntries, configuredSeeds, roundDrawSize, nbQualifiers);

      // Step 3: Place qualifiers randomly in available slots (only in QUALIFS_KO mode)
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
    } else {
      // KNOCKOUT mode: Place BYEs normally for all teams (no qualifiers)
      int nbTeams = allPairs.size();
      ByePlacementUtil.placeByeTeams(initialRound, nbTeams, configuredSeeds, roundDrawSize);

      // Place remaining teams randomly in available slots
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
                                                .collect(Collectors.toList());
      RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingTeams);
    }
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

  /**
   * Collects all real teams (non-BYE, non-QUALIFIER) placed in a round.
   */
  private Set<PlayerPair> collectTeamsPlacedInRound(Round round) {
    Set<PlayerPair> teamsPlaced = new HashSet<>();

    for (Game game : round.getGames()) {
      addRealTeamIfPresent(game.getTeamA(), teamsPlaced);
      addRealTeamIfPresent(game.getTeamB(), teamsPlaced);
    }

    return teamsPlaced;
  }

  /**
   * Adds a team to the collection if it's a real team (not BYE, not QUALIFIER).
   */
  private void addRealTeamIfPresent(PlayerPair team, Set<PlayerPair> teamsCollection) {
    if (team != null && !team.isBye() && !team.isQualifier()) {
      teamsCollection.add(team);
    }
  }
}

