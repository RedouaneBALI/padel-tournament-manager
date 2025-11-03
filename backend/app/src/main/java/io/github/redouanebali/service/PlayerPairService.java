package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerPairService {

  private static final String               TOURNAMENT_NOT_FOUND = "Tournament not found";
  private final        TournamentRepository tournamentRepository;
  private final        SecurityProps        securityProps;
  private final        TournamentMapper     tournamentMapper;

  /**
   * Adds player pairs to a tournament and clears existing game assignments. Automatically adds BYE pairs if needed to reach the main draw size. Only
   * the owner or super admins can add pairs.
   *
   * @param tournamentId the tournament ID
   * @param requests list of player pair requests to add
   * @return the updated tournament with new pairs
   * @throws IllegalArgumentException if tournament is not found
   * @throws AccessDeniedException if user lacks modification rights
   */
  @Transactional
  public Tournament addPairs(Long tournamentId, List<CreatePlayerPairRequest> requests) {
    Tournament tournament = tournamentRepository.findByIdWithLock(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException(TOURNAMENT_NOT_FOUND));
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }

    log.info("Adding {} player pairs to tournament {} by user {}", requests.size(), tournamentId, me);

    tournament.getRounds().forEach(round ->
                                       round.getGames().forEach(game -> {
                                         game.setTeamA(null);
                                         game.setTeamB(null);
                                       })
    );
    List<PlayerPair> pairs = tournamentMapper.toPlayerPairList(requests);
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(pairs);
    addByesIfNeeded(tournament);
    addQualifiersIfNeeded(tournament);
    return tournamentRepository.save(tournament);
  }

  /**
   * Retrieves all player pairs for a tournament. Can optionally exclude BYE pairs and QUALIFIER placeholders from the result. When including BYEs,
   * they are reorganized to be placed opposite seeds for better UX in manual mode.
   *
   * @param tournamentId the tournament ID
   * @param includeByes whether to include BYE pairs in the result
   * @param includeQualified whether to include QUALIFIER placeholders in the result
   * @return list of player pairs, with BYEs reorganized opposite seeds if includeByes is true
   * @throws IllegalArgumentException if tournament is not found
   */
  public List<PlayerPair> getPairsByTournamentId(Long tournamentId, boolean includeByes, boolean includeQualified) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException(TOURNAMENT_NOT_FOUND));
    if (includeByes) {
      List<PlayerPair> pairs = tournament.getPlayerPairs();
      if (!includeQualified) {
        return pairs.stream().filter(pp -> !pp.isQualifier()).toList();
      }
      return pairs;
    } else {
      return tournament.getPlayerPairs().stream()
                       .filter(pp -> !pp.isBye() && (includeQualified || !pp.isQualifier()))
                       .toList();
    }
  }

  /**
   * Updates an existing player pair's information (names and/or seed). BYE pairs cannot be modified. Only the owner or super admins can update
   * pairs.
   *
   * @param tournamentId the tournament ID
   * @param pairId the player pair ID to update
   * @param player1Name new name for player 1 (optional)
   * @param player2Name new name for player 2 (optional)
   * @param seed new seed value (optional)
   * @throws IllegalArgumentException if tournament or pair is not found, or names are blank
   * @throws IllegalStateException if trying to modify a BYE pair
   * @throws AccessDeniedException if user lacks modification rights
   */
  @Transactional
  public void updatePlayerPair(Long tournamentId, Long pairId, String player1Name, String player2Name, Integer seed) {
    Tournament tournament = tournamentRepository.findByIdWithLock(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException(TOURNAMENT_NOT_FOUND));

    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }

    PlayerPair pair = tournament.getPlayerPairs().stream()
                                .filter(pp -> pp.getId() != null && pp.getId().equals(pairId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("PlayerPair not found in this tournament"));

    if (pair.isBye()) {
      throw new IllegalStateException("Cannot modify a BYE pair");
    }

    if (player1Name != null) {
      String p1 = player1Name.trim();
      if (p1.isEmpty()) {
        throw new IllegalArgumentException("player1Name must not be blank");
      }
      pair.getPlayer1().setName(p1);
    }
    if (player2Name != null) {
      String p2 = player2Name.trim();
      if (p2.isEmpty()) {
        throw new IllegalArgumentException("player2Name must not be blank");
      }
      pair.getPlayer2().setName(p2);
    }
    if (seed != null) {
      pair.setSeed(seed);
    }

    tournamentRepository.save(tournament);
  }

  /**
   * Reorders the complete list of player pairs for a tournament. Preserves the exact order sent from the frontend, including BYE and QUALIFIER
   * positions. Example: [pair1, pair2, BYE, pair3, QUALIFIER, pair4] stays in this exact order. Only the owner or super admins can reorder pairs.
   *
   * @param tournamentId the tournament ID
   * @param orderedPairIds list of ALL pair IDs in the desired order (including BYE and QUALIFIER)
   * @throws IllegalArgumentException if tournament not found, or if orderedPairIds doesn't match existing pairs
   * @throws AccessDeniedException if user lacks modification rights
   */
  @Transactional
  public void reorderPlayerPairs(Long tournamentId, List<Long> orderedPairIds) {
    Tournament tournament = tournamentRepository.findByIdWithLock(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException(TOURNAMENT_NOT_FOUND));

    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }

    List<PlayerPair> currentPairs = tournament.getPlayerPairs();

    // Validate that orderedPairIds contains exactly the same IDs as currentPairs
    Set<Long> currentIds = currentPairs.stream()
                                       .map(PlayerPair::getId)
                                       .filter(java.util.Objects::nonNull)
                                       .collect(java.util.stream.Collectors.toSet());

    Set<Long> providedIds = new java.util.HashSet<>(orderedPairIds);

    if (!currentIds.equals(providedIds)) {
      throw new IllegalArgumentException(
          String.format("Provided pair IDs don't match existing pairs. Expected %d IDs, got %d. " +
                        "All pairs (including BYE and QUALIFIER) must be included.",
                        currentIds.size(), providedIds.size()));
    }

    // Create a map for quick lookup of ALL pairs (normal, BYE, and QUALIFIER)
    java.util.Map<Long, PlayerPair> pairMap = currentPairs.stream()
                                                          .filter(p -> p.getId() != null)
                                                          .collect(java.util.stream.Collectors.toMap(
                                                              PlayerPair::getId,
                                                              java.util.function.Function.identity()
                                                          ));

    // Build the new ordered list preserving the exact order from frontend
    List<PlayerPair> reorderedPairs = new java.util.ArrayList<>();
    for (Long pairId : orderedPairIds) {
      PlayerPair pair = pairMap.get(pairId);
      if (pair != null) {
        reorderedPairs.add(pair);
      }
    }

    // Replace the tournament's pair list with the reordered one
    currentPairs.clear();
    currentPairs.addAll(reorderedPairs);

    tournamentRepository.save(tournament);

    log.info("Reordered {} player pairs (including BYE and QUALIFIER) for tournament {} by user {}",
             orderedPairIds.size(), tournamentId, me);
  }

  /**
   * Adds BYE pairs to the tournament if needed to reach the required draw sizes based on the tournament format. For KNOCKOUT: adds BYE to reach
   * mainDrawSize. For QUALIF_KO: adds BYE to reach preQualDrawSize + mainDrawSize. For GROUPS_KO: adds BYE to reach nbPools * nbPairsPerPool.
   *
   * @param tournament the tournament to add BYE pairs to
   */
  private void addByesIfNeeded(Tournament tournament) {
    List<PlayerPair> pairs      = tournament.getPlayerPairs();
    int              byesNeeded = calculateByesNeeded(tournament, pairs.size());
    if (byesNeeded > 0) {
      log.debug("Adding {} BYE pairs to reach required draw size for format {}", byesNeeded, tournament.getConfig().getFormat());
      for (int i = 0; i < byesNeeded; i++) {
        tournament.getPlayerPairs().add(PlayerPair.bye());
      }
    }
  }

  /**
   * Calculates the number of BYE pairs needed based on the tournament format.
   */
  private int calculateByesNeeded(Tournament tournament, int currentPairs) {
    switch (tournament.getConfig().getFormat()) {
      case KNOCKOUT:
        return calculateByesForKnockout(tournament, currentPairs);
      case QUALIF_KO:
        return calculateByesForQualifKo(tournament, currentPairs);
      case GROUPS_KO:
        return calculateByesForGroupsKo(tournament, currentPairs);
      default:
        throw new IllegalArgumentException("Unsupported tournament format: " + tournament.getConfig().getFormat());
    }
  }

  /**
   * Calculates BYEs for KNOCKOUT format.
   */
  private int calculateByesForKnockout(Tournament tournament, int currentPairs) {
    Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
    return mainDrawSize != null && currentPairs < mainDrawSize ? mainDrawSize - currentPairs : 0;
  }

  /**
   * Calculates BYEs for QUALIF_KO format.
   */
  private int calculateByesForQualifKo(Tournament tournament, int currentPairs) {
    Integer preQualDrawSize = tournament.getConfig().getPreQualDrawSize();
    Integer mainDrawSize    = tournament.getConfig().getMainDrawSize();
    Integer nbQualifiers    = tournament.getConfig().getNbQualifiers();
    int totalRequired = (preQualDrawSize != null ? preQualDrawSize : 0) +
                        (mainDrawSize != null ? mainDrawSize : 0) -
                        (nbQualifiers != null ? nbQualifiers : 0);
    return currentPairs < totalRequired ? totalRequired - currentPairs : 0;
  }

  /**
   * Calculates BYEs for GROUPS_KO format.
   */
  private int calculateByesForGroupsKo(Tournament tournament, int currentPairs) {
    Integer nbPools        = tournament.getConfig().getNbPools();
    Integer nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();
    int     totalRequired  = (nbPools != null ? nbPools : 0) * (nbPairsPerPool != null ? nbPairsPerPool : 0);
    return currentPairs < totalRequired ? totalRequired - currentPairs : 0;
  }

  /**
   * Adds QUALIFIER pairs to the tournament if needed based on the tournament format. For QUALIF_KO: adds QUALIFIER to fill mainDrawSize with
   * qualifiers. For GROUPS_KO: adds QUALIFIER to fill nbPools * currentPoolSizes.
   *
   * @param tournament the tournament to add QUALIFIER pairs to
   */
  private void addQualifiersIfNeeded(Tournament tournament) {
    int qualifiersToAdd = calculateQualifiersToAdd(tournament);
    if (qualifiersToAdd > 0) {
      log.debug("Adding {} QUALIFIER pairs for format {}", qualifiersToAdd, tournament.getConfig().getFormat());
      for (int i = 0; i < qualifiersToAdd; i++) {
        tournament.getPlayerPairs().add(PlayerPair.qualifier(i + 1));
      }
    }
  }

  /**
   * Calculates the number of QUALIFIER pairs to add based on the tournament format.
   */
  private int calculateQualifiersToAdd(Tournament tournament) {
    switch (tournament.getConfig().getFormat()) {
      case QUALIF_KO: {
        Integer nbQualifiers = tournament.getConfig().getNbQualifiers();
        return nbQualifiers != null ? nbQualifiers : 0;
      }
      case GROUPS_KO: {
        Integer nbPools        = tournament.getConfig().getNbPools();
        Integer nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();
        if (nbPools != null && nbPairsPerPool != null) {
          int totalPairsNeeded = nbPools * nbPairsPerPool;
          int currentPairs     = tournament.getPlayerPairs().size();
          return Math.max(0, totalPairsNeeded - currentPairs);
        }
        return 0;
      }
      case KNOCKOUT:
        return 0;
      default:
        throw new IllegalArgumentException("Unsupported tournament format: " + tournament.getConfig().getFormat());
    }
  }
}
