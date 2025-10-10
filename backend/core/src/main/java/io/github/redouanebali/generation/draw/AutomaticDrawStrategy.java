package io.github.redouanebali.generation.draw;

import io.github.redouanebali.generation.util.ByePlacementUtil;
import io.github.redouanebali.generation.util.GameSlotUtil;
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
                                                  .toList();

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

    // If we have more teams than qualification slots, the best teams go directly to main draw
    int teamsGoingDirectToMainDraw = Math.max(0, allPairs.size() - qualifDrawSize);

    // Teams participating in qualifs are the LAST ones in the list (lower-ranked)
    List<PlayerPair> qualifParticipants = new ArrayList<>();
    for (int i = teamsGoingDirectToMainDraw; i < allPairs.size(); i++) {
      qualifParticipants.add(allPairs.get(i));
    }

    // Calculate necessary BYEs in qualif
    int byesNeededInQualifs = Math.max(0, qualifDrawSize - qualifParticipants.size());

    // Place seeds in qualifs (among qualif participants only)
    if (nbSeedsQualif > 0 && !qualifParticipants.isEmpty()) {
      SeedPlacementUtil.placeSeedTeams(initialRound, qualifParticipants, nbSeedsQualif, qualifDrawSize);
    }

    // Place BYEs in qualifs if necessary
    if (byesNeededInQualifs > 0) {
      ByePlacementUtil.placeByeTeams(initialRound, qualifParticipants.size(), nbSeedsQualif, qualifDrawSize);
    }

    // Place remaining qualif participants randomly
    List<PlayerPair> remainingQualifTeams = new ArrayList<>();

    // Find which teams are already placed in the round
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game game : initialRound.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        alreadyPlaced.add(game.getTeamA());
      }
      if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        alreadyPlaced.add(game.getTeamB());
      }
    }

    // Add all teams that are NOT yet placed
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
    for (List<PlayerPair> pool : pools) {
      while (pool.size() < nbPairsPerPool && idx < others.size()) {
        pool.add(others.get(idx));
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

    switch (format) {
      case KNOCKOUT ->
        // Pure KNOCKOUT mode - use standard tennis/padel logic
          processKnockoutStandard(initialRound, allPairs, roundDrawSize, configuredSeeds);
      case QUALIF_KO ->
        // QUALIF_KO mode - different logic with qualifiers
          processKnockoutWithQualifiers(tournament, initialRound, allPairs, roundDrawSize, configuredSeeds);
      default ->
        // Fallback for other formats
          processKnockoutStandard(initialRound, allPairs, roundDrawSize, configuredSeeds);
    }
  }

  /**
   * Processes KNOCKOUT format using standard tennis/padel logic: 1. The TOP N teams (where N = nbByes) receive BYEs at standard positions 2. The
   * remaining teams play against each other in first round
   *
   * Standard tennis/padel rule: If you have X teams in a Y-slot draw, the best (Y-X) teams get BYES, regardless of how many are officially "seeded"
   */
  private void processKnockoutStandard(Round initialRound, List<PlayerPair> allPairs, int roundDrawSize, int configuredSeeds) {
    int nbTeams = allPairs.size();
    int nbByes  = roundDrawSize - nbTeams;

    if (nbByes == 0) {
      // No BYES needed - draw is full
      // Use SeedPlacementUtil for proper seed placement
      if (configuredSeeds > 0) {
        SeedPlacementUtil.placeSeedTeams(initialRound, allPairs, configuredSeeds, roundDrawSize);
      }
      // Place remaining non-seeded teams RANDOMLY (not sequentially!)
      placeRemainingNonSeededTeamsRandomly(initialRound, allPairs);
      return;
    }

    // Standard tennis/padel logic: The TOP N teams (where N = nbByes) receive BYES
    int nbTeamsReceivingByes = Math.min(nbByes, nbTeams);

    // Step 1: Place the top N teams at standard seed positions (they will receive BYES)
    List<PlayerPair> teamsWithByes = allPairs.subList(0, nbTeamsReceivingByes);
    if (nbTeamsReceivingByes > 0) {
      SeedPlacementUtil.placeSeedTeams(initialRound, teamsWithByes, nbTeamsReceivingByes, roundDrawSize);
    }

    // Step 2: Place BYEs opposite ALL teams that have been placed (not just configured seeds)
    // This is the key fix: we place BYEs opposite the teams that were actually placed,
    // not based on theoretical seed positions
    placeByesOppositePlacedTeams(initialRound, nbByes);

    // Step 3: Place remaining teams that will play in first round
    // CRITICAL FIX: Use RANDOM placement instead of sequential!
    int nbTeamsPlayingFirstRound = nbTeams - nbTeamsReceivingByes;
    if (nbTeamsPlayingFirstRound > 0) {
      List<PlayerPair> teamsPlayingFirstRound = allPairs.subList(nbTeamsReceivingByes, nbTeams);
      // Use RandomPlacementUtil instead of placeTeamsInEmptySlots to ensure random draw
      RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, teamsPlayingFirstRound);
    }
  }

  /**
   * Places BYEs opposite all teams that have already been placed in the round. This ensures every placed team gets a BYE opponent.
   */
  private void placeByesOppositePlacedTeams(Round initialRound, int maxByes) {
    List<Game> games      = initialRound.getGames();
    int        byesPlaced = 0;

    // Pass 1: Place BYes opposite teams that are already placed
    byesPlaced = placeByesOppositeExistingTeams(games, byesPlaced, maxByes);

    // Pass 2: Place BYEs in empty slots (avoiding BYE vs BYE)
    if (byesPlaced < maxByes) {
      byesPlaced = placeByesInEmptySlotsAvoidingByeVsBye(games, byesPlaced, maxByes);
    }

    // Pass 3: Last resort - place BYEs even if it creates BYE vs BYE
    if (byesPlaced < maxByes) {
      placeRemainingByesInAnySlot(games, byesPlaced, maxByes);
    }
  }

  private int placeByesOppositeExistingTeams(List<Game> games, int byesPlaced, int maxByes) {
    int placed = byesPlaced;
    for (Game game : games) {
      if (placed >= maxByes) {
        break;
      }
      placed = tryPlaceByeOppositeTeam(game, placed);
    }
    return placed;
  }

  private int tryPlaceByeOppositeTeam(Game game, int byesPlaced) {
    PlayerPair teamA = game.getTeamA();
    PlayerPair teamB = game.getTeamB();

    if (isRealTeam(teamA) && teamB == null) {
      game.setTeamB(PlayerPair.bye());
      return byesPlaced + 1;
    }
    if (isRealTeam(teamB) && teamA == null) {
      game.setTeamA(PlayerPair.bye());
      return byesPlaced + 1;
    }
    return byesPlaced;
  }

  private boolean isRealTeam(PlayerPair team) {
    return team != null && !team.isBye() && !team.isQualifier();
  }

  private int placeByesInEmptySlotsAvoidingByeVsBye(List<Game> games, int byesPlaced, int maxByes) {
    int placed = byesPlaced;
    for (Game game : games) {
      if (placed >= maxByes) {
        break;
      }
      placed = tryPlaceByeInEmptySlot(game, placed, maxByes);
    }
    return placed;
  }

  private int tryPlaceByeInEmptySlot(Game game, int byesPlaced, int maxByes) {
    int        placed = byesPlaced;
    PlayerPair teamA  = game.getTeamA();
    PlayerPair teamB  = game.getTeamB();

    if (teamA == null && teamB != null && !teamB.isBye()) {
      game.setTeamA(PlayerPair.bye());
      placed++;
    }

    if (placed < maxByes && teamB == null && teamA != null && !teamA.isBye()) {
      game.setTeamB(PlayerPair.bye());
      placed++;
    }

    return placed;
  }

  private void placeRemainingByesInAnySlot(List<Game> games, int byesPlaced, int maxByes) {
    int placed = byesPlaced;
    for (Game game : games) {
      if (placed >= maxByes) {
        break;
      }

      if (game.getTeamA() == null) {
        game.setTeamA(PlayerPair.bye());
        placed++;
      }

      if (placed < maxByes && game.getTeamB() == null) {
        game.setTeamB(PlayerPair.bye());
        placed++;
      }
    }
  }


  /**
   * Places non-seeded teams RANDOMLY in empty slots after seeds have been placed.
   */
  private void placeRemainingNonSeededTeamsRandomly(Round initialRound, List<PlayerPair> allPairs) {
    // Find already placed teams
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game game : initialRound.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye()) {
        alreadyPlaced.add(game.getTeamA());
      }
      if (game.getTeamB() != null && !game.getTeamB().isBye()) {
        alreadyPlaced.add(game.getTeamB());
      }
    }

    // Get remaining teams
    List<PlayerPair> remainingTeams = allPairs.stream()
                                              .filter(p -> !alreadyPlaced.contains(p))
                                              .toList();

    // Place them RANDOMLY in empty slots (not sequentially!)
    if (!remainingTeams.isEmpty()) {
      RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingTeams);
    }
  }

  /**
   * Places teams sequentially in remaining empty slots. This ensures teams are paired correctly for matches.
   */
  private void placeTeamsInEmptySlots(Round initialRound, List<PlayerPair> teams) {
    if (teams.isEmpty()) {
      return;
    }

    List<Game> games     = initialRound.getGames();
    int        teamIndex = 0;

    for (Game game : games) {
      if (teamIndex >= teams.size()) {
        break;
      }

      // Fill teamA if empty
      if (game.getTeamA() == null) {
        game.setTeamA(teams.get(teamIndex++));
      }

      if (teamIndex >= teams.size()) {
        break;
      }

      // Fill teamB if empty
      if (game.getTeamB() == null) {
        game.setTeamB(teams.get(teamIndex++));
      }
    }
  }

  /**
   * Places seeds at their standard positions (1, 2, 3-4, 5-8, etc.)
   */
  private void placeSeedsAtStandardPositions(Round initialRound, List<PlayerPair> seeds, int nbSeeds, int drawSize) {
    List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    List<Game>    games         = initialRound.getGames();

    for (int i = 0; i < Math.min(seeds.size(), seedPositions.size()); i++) {
      int     slot      = seedPositions.get(i);
      int     gameIndex = slot / 2;
      boolean isTeamA   = (slot % 2 == 0);

      Game game = games.get(gameIndex);
      if (isTeamA) {
        game.setTeamA(seeds.get(i));
      } else {
        game.setTeamB(seeds.get(i));
      }
    }
  }

  /**
   * Places BYEs opposite seeds following standard tennis/padel logic. Seed 1 plays BYE, Seed 2 plays BYE, etc. until all BYEs are placed.
   */
  private void placeByesOppositeSeedsStandard(Round initialRound, int nbByes, int nbSeeds, int drawSize) {
    List<Integer> seedPositions = SeedPlacementUtil.getSeedsPositions(drawSize, nbSeeds);
    List<Game>    games         = initialRound.getGames();

    int byesPlaced = 0;

    // Step 1: Place BYEs opposite seeds (starting with seed 1)
    byesPlaced = placeByesOppositeSeedsPhase1(games, seedPositions, byesPlaced, nbByes, drawSize);

    // Step 2: If we still have BYEs to place, place them in empty slots but AVOID creating BYE vs BYE
    if (byesPlaced < nbByes) {
      byesPlaced = placeByesAvoidingByeVsByePhase2(games, byesPlaced, nbByes);
    }

    // Step 3: Final pass - place remaining BYEs only in completely empty games (both slots null)
    if (byesPlaced < nbByes) {
      placeRemainingByesInEmptyGamesPhase3(games, byesPlaced, nbByes);
    }
  }

  /**
   * Phase 1: Place BYEs opposite seeds at their standard positions.
   */
  private int placeByesOppositeSeedsPhase1(List<Game> games, List<Integer> seedPositions, int byesPlaced, int nbByes, int drawSize) {
    int placed = byesPlaced;

    for (int i = 0; i < seedPositions.size() && placed < nbByes; i++) {
      int seedSlot     = seedPositions.get(i);
      int oppositeSlot = GameSlotUtil.getOppositeSlot(seedSlot);

      int     gameIndex = oppositeSlot / 2;
      boolean isTeamA   = (oppositeSlot % 2 == 0);

      Game       game        = games.get(gameIndex);
      PlayerPair currentTeam = isTeamA ? game.getTeamA() : game.getTeamB();

      // Only place BYE if slot is empty
      if (currentTeam == null) {
        if (isTeamA) {
          game.setTeamA(PlayerPair.bye());
        } else {
          game.setTeamB(PlayerPair.bye());
        }
        placed++;
      }
    }

    return placed;
  }

  /**
   * Phase 2: Place BYEs in empty slots while avoiding BYE vs BYE matches.
   */
  private int placeByesAvoidingByeVsByePhase2(List<Game> games, int byesPlaced, int nbByes) {
    int placed = byesPlaced;

    for (Game game : games) {
      if (placed >= nbByes) {
        break;
      }

      placed = tryPlaceByeInTeamASlotAvoidingByeVsBye(game, placed, nbByes);
      placed = tryPlaceByeInTeamBSlotAvoidingByeVsBye(game, placed, nbByes);
    }

    return placed;
  }

  /**
   * Tries to place a BYE in teamA slot if opponent is not a BYE.
   */
  private int tryPlaceByeInTeamASlotAvoidingByeVsBye(Game game, int byesPlaced, int nbByes) {
    if (byesPlaced >= nbByes) {
      return byesPlaced;
    }

    if (game.getTeamA() == null) {
      PlayerPair opponent = game.getTeamB();
      // Only place BYE if opponent is NOT a BYE (to avoid BYE vs BYE)
      if (opponent != null && !opponent.isBye()) {
        game.setTeamA(PlayerPair.bye());
        return byesPlaced + 1;
      }
    }

    return byesPlaced;
  }

  /**
   * Tries to place a BYE in teamB slot if opponent is not a BYE.
   */
  private int tryPlaceByeInTeamBSlotAvoidingByeVsBye(Game game, int byesPlaced, int nbByes) {
    if (byesPlaced >= nbByes) {
      return byesPlaced;
    }

    if (game.getTeamB() == null) {
      PlayerPair opponent = game.getTeamA();
      // Only place BYE if opponent is NOT a BYE (to avoid BYE vs BYE)
      if (opponent != null && !opponent.isBye()) {
        game.setTeamB(PlayerPair.bye());
        return byesPlaced + 1;
      }
    }

    return byesPlaced;
  }

  /**
   * Phase 3: Place remaining BYEs only in completely empty games (both slots null). This creates BYE vs BYE matches only as a last resort.
   */
  private void placeRemainingByesInEmptyGamesPhase3(List<Game> games, int byesPlaced, int nbByes) {
    int placed = byesPlaced;

    for (Game game : games) {
      if (placed >= nbByes) {
        break;
      }

      // Only place BYEs in completely empty games to create BYE vs BYE matches
      if (game.getTeamA() == null && game.getTeamB() == null) {
        game.setTeamA(PlayerPair.bye());
        placed++;

        if (placed >= nbByes) {
          break;
        }

        game.setTeamB(PlayerPair.bye());
        placed++;
      }
    }
  }

  /**
   * Processes KNOCKOUT with qualifiers (QUALIF_KO format)
   */
  private void processKnockoutWithQualifiers(Tournament tournament,
                                             Round initialRound,
                                             List<PlayerPair> allPairs,
                                             int roundDrawSize,
                                             int configuredSeeds) {
    int nbQualifiers  = tournament.getConfig().getNbQualifiers();
    int totalSlots    = tournament.getConfig().getMainDrawSize();
    int directEntries = Math.min(totalSlots - nbQualifiers, allPairs.size());

    // Step 1: Place only the actual seeds
    int actualSeeds = Math.min(configuredSeeds, directEntries);
    if (actualSeeds > 0) {
      List<PlayerPair> seeds = allPairs.subList(0, actualSeeds);
      placeSeedsAtStandardPositions(initialRound, seeds, actualSeeds, roundDrawSize);
    }

    // Step 2: Place qualifiers
    RandomPlacementUtil.placeQualifiers(initialRound, nbQualifiers);

    // Step 3: Place BYEs (accounting for direct entries and qualifiers)
    int nbByes = roundDrawSize - directEntries - nbQualifiers;
    if (nbByes > 0) {
      placeByesOppositeSeedsStandard(initialRound, nbByes, actualSeeds, roundDrawSize);
    }

    // Step 4: Place remaining teams
    Set<PlayerPair> alreadyPlaced = new HashSet<>();
    for (Game game : initialRound.getGames()) {
      if (game.getTeamA() != null && !game.getTeamA().isBye() && !game.getTeamA().isQualifier()) {
        alreadyPlaced.add(game.getTeamA());
      }
      if (game.getTeamB() != null && !game.getTeamB().isBye() && !game.getTeamB().isQualifier()) {
        alreadyPlaced.add(game.getTeamB());
      }
    }

    List<PlayerPair> remainingTeams = allPairs.stream()
                                              .filter(p -> !alreadyPlaced.contains(p))
                                              .limit(directEntries)
                                              .toList();

    if (!remainingTeams.isEmpty()) {
      RandomPlacementUtil.placeRemainingTeamsRandomly(initialRound, remainingTeams);
    }
  }


  /**
   * Collects already placed teams including all types (for KNOCKOUT format).
   */
  private Set<PlayerPair> collectAlreadyPlacedTeamsIncludingAll(Round initialRound) {
    Set<PlayerPair> alreadyPlaced = new HashSet<>();

    for (Game g : initialRound.getGames()) {
      addTeamIfNotBye(g.getTeamA(), alreadyPlaced);
      addTeamIfNotBye(g.getTeamB(), alreadyPlaced);
    }

    return alreadyPlaced;
  }

  /**
   * Adds team if it's not null, not BYE and not QUALIFIER.
   */
  private void addTeamIfNotByeAndNotQualifier(PlayerPair team, Set<PlayerPair> collection) {
    if (team != null && !team.isBye() && !team.isQualifier()) {
      collection.add(team);
    }
  }

  /**
   * Adds team if it's not null and not BYE.
   */
  private void addTeamIfNotBye(PlayerPair team, Set<PlayerPair> collection) {
    if (team != null && !team.isBye()) {
      collection.add(team);
    }
  }

  /**
   * Collects all teams that have been placed in a round (from games and pools).
   */
  private Set<PlayerPair> collectTeamsPlacedInRound(Round round) {
    Set<PlayerPair> teamsPlaced = new HashSet<>();
    collectTeamsFromGames(round, teamsPlaced);
    collectTeamsFromPools(round, teamsPlaced);
    return teamsPlaced;
  }

  private void collectTeamsFromGames(Round round, Set<PlayerPair> teamsPlaced) {
    if (round.getGames() == null) {
      return;
    }
    for (Game game : round.getGames()) {
      addTeamIfNotBye(game.getTeamA(), teamsPlaced);
      addTeamIfNotBye(game.getTeamB(), teamsPlaced);
    }
  }

  private void collectTeamsFromPools(Round round, Set<PlayerPair> teamsPlaced) {
    if (round.getPools() == null) {
      return;
    }
    for (Pool pool : round.getPools()) {
      if (pool.getPairs() == null) {
        continue;
      }
      for (PlayerPair pair : pool.getPairs()) {
        if (pair != null && !pair.isBye()) {
          teamsPlaced.add(pair);
        }
      }
    }
  }
}
