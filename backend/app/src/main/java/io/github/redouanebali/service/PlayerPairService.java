package io.github.redouanebali.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import java.util.ArrayList;
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
  public Tournament addPairs(Long tournamentId, List<CreatePlayerPairRequest> requests) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
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
      List<PlayerPair> pairs = reorderPairsWithByesAndQualifiersAtCorrectPositions(tournament);
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
   * Reorders player pairs by inserting BYEs and QUALIFIERS at the correct absolute positions. Uses bye_positions.json to determine where BYEs should
   * be placed.
   */
  private List<PlayerPair> reorderPairsWithByesAndQualifiersAtCorrectPositions(Tournament tournament) {
    List<PlayerPair> allPairs;
    List<PlayerPair> preQualPairs = new ArrayList<>();
    if (tournament.getConfig().getFormat() == TournamentFormat.QUALIF_KO) {
      Integer preQualDrawSize = tournament.getConfig().getPreQualDrawSize();
      if (preQualDrawSize != null && preQualDrawSize < tournament.getPlayerPairs().size()) {
        preQualPairs = new ArrayList<>(tournament.getPlayerPairs().subList(0, preQualDrawSize));
        allPairs     = new ArrayList<>(tournament.getPlayerPairs().subList(preQualDrawSize, tournament.getPlayerPairs().size()));
      } else {
        allPairs = new ArrayList<>(tournament.getPlayerPairs());
      }
    } else {
      allPairs = new ArrayList<>(tournament.getPlayerPairs());
    }
    Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
    Integer nbSeeds      = tournament.getConfig().getNbSeeds();

    if (!isValidDrawConfiguration(mainDrawSize, nbSeeds)) {
      return tournament.getPlayerPairs(); // Return all pairs if config invalid
    }

    List<PlayerPair> realPairs = allPairs.stream().filter(pp -> !pp.isBye()).toList();
    List<PlayerPair> byePairs  = allPairs.stream().filter(PlayerPair::isBye).toList();

    List<PlayerPair> reorderedMainDraw;
    if (byePairs.isEmpty()) {
      reorderedMainDraw = realPairs;
    } else {
      reorderedMainDraw = buildReorderedPairsList(mainDrawSize, nbSeeds, realPairs, byePairs);
    }

    // For QUALIF_KO, prepend preQual pairs
    if (!preQualPairs.isEmpty()) {
      List<PlayerPair> fullList = new ArrayList<>(preQualPairs);
      fullList.addAll(reorderedMainDraw);
      return fullList;
    } else {
      return reorderedMainDraw;
    }
  }

  /**
   * Validates that the draw configuration has valid mainDrawSize and nbSeeds values.
   */
  private boolean isValidDrawConfiguration(Integer mainDrawSize, Integer nbSeeds) {
    return mainDrawSize != null && mainDrawSize > 0 && nbSeeds != null && nbSeeds > 0;
  }

  /**
   * Builds the reordered list of pairs with BYEs at correct positions.
   */
  private List<PlayerPair> buildReorderedPairsList(int mainDrawSize, int nbSeeds, List<PlayerPair> realPairs, List<PlayerPair> byePairs) {
    List<Integer>          byePositions   = loadByePositionsFromJson(mainDrawSize, nbSeeds, byePairs.size());
    java.util.Set<Integer> byePositionSet = new java.util.HashSet<>(byePositions);
    PlayerPair[]           resultArray    = new PlayerPair[mainDrawSize];

    fillResultArrayWithPairs(resultArray, byePositionSet, realPairs, byePairs);

    return convertArrayToList(resultArray);
  }

  /**
   * Fills the result array with BYEs and real pairs at their correct positions.
   */
  private void fillResultArrayWithPairs(PlayerPair[] resultArray, java.util.Set<Integer> byePositionSet,
                                        List<PlayerPair> realPairs, List<PlayerPair> byePairs) {
    int byeIndex  = 0;
    int realIndex = 0;

    for (int position = 0; position < resultArray.length; position++) {
      if (byePositionSet.contains(position)) {
        byeIndex = placePairAtPosition(resultArray, position, byePairs, byeIndex);
      } else {
        realIndex = placePairAtPosition(resultArray, position, realPairs, realIndex);
      }
    }
  }

  /**
   * Places a pair at a specific position in the result array if available. Returns the updated index.
   */
  private int placePairAtPosition(PlayerPair[] resultArray, int position, List<PlayerPair> pairs, int currentIndex) {
    if (currentIndex < pairs.size()) {
      resultArray[position] = pairs.get(currentIndex);
      return currentIndex + 1;
    }
    return currentIndex;
  }

  /**
   * Converts the result array to a list, filtering out null values.
   */
  private List<PlayerPair> convertArrayToList(PlayerPair[] resultArray) {
    List<PlayerPair> result = new ArrayList<>(resultArray.length);
    for (PlayerPair pair : resultArray) {
      if (pair != null) {
        result.add(pair);
      }
    }
    return result;
  }

  /**
   * Loads BYE positions from bye_positions.json file. Similar to SeedPlacementUtil.getSeedsPositions() but for BYEs.
   */
  private List<Integer> loadByePositionsFromJson(int drawSize, int nbSeeds, int nbByes) {
    try {
      ObjectMapper        mapper = new ObjectMapper();
      java.io.InputStream is     = getClass().getClassLoader().getResourceAsStream("bye_positions.json");

      if (is == null) {
        log.warn("bye_positions.json not found, returning empty list");
        return new ArrayList<>();
      }

      JsonNode root = mapper.readTree(is);
      JsonNode byePositionsNode = root.path(String.valueOf(drawSize))
                                      .path(String.valueOf(nbSeeds))
                                      .path(String.valueOf(nbByes));

      if (byePositionsNode.isMissingNode()) {
        log.warn("No BYE positions found for drawSize={}, nbSeeds={}, nbByes={}", drawSize, nbSeeds, nbByes);
        return new ArrayList<>();
      }

      List<Integer> positions = new ArrayList<>();
      for (JsonNode position : byePositionsNode) {
        positions.add(position.asInt());
      }

      return positions;

    } catch (Exception e) {
      log.error("Failed to load BYE positions from JSON", e);
      return new ArrayList<>();
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
    Tournament tournament = tournamentRepository.findById(tournamentId)
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
   * Adds BYE pairs to the tournament if needed to reach the required draw sizes based on the tournament format. For KNOCKOUT: adds BYE to reach
   * mainDrawSize. For QUALIF_KO: adds BYE to reach preQualDrawSize + mainDrawSize. For GROUPS_KO: adds BYE to reach nbPools * nbPairsPerPool.
   *
   * @param tournament the tournament to add BYE pairs to
   */
  private void addByesIfNeeded(Tournament tournament) {
    List<PlayerPair> pairs      = tournament.getPlayerPairs();
    int              byesNeeded = 0;
    switch (tournament.getConfig().getFormat()) {
      case KNOCKOUT -> {
        Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
        if (mainDrawSize != null && pairs.size() < mainDrawSize) {
          byesNeeded = mainDrawSize - pairs.size();
        }
      }
      case QUALIF_KO -> {
        Integer preQualDrawSize = tournament.getConfig().getPreQualDrawSize();
        Integer mainDrawSize    = tournament.getConfig().getMainDrawSize();
        Integer nbQualifiers    = tournament.getConfig().getNbQualifiers();
        int totalRequired = (preQualDrawSize != null ? preQualDrawSize : 0) +
                            (mainDrawSize != null ? mainDrawSize : 0) -
                            (nbQualifiers != null ? nbQualifiers : 0);
        if (pairs.size() < totalRequired) {
          byesNeeded = totalRequired - pairs.size();
        }
      }
      case GROUPS_KO -> {
        Integer nbPools        = tournament.getConfig().getNbPools();
        Integer nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();
        int     totalRequired  = (nbPools != null ? nbPools : 0) * (nbPairsPerPool != null ? nbPairsPerPool : 0);
        if (pairs.size() < totalRequired) {
          byesNeeded = totalRequired - pairs.size();
        }
      }
    }
    if (byesNeeded > 0) {
      log.debug("Adding {} BYE pairs to reach required draw size for format {}", byesNeeded, tournament.getConfig().getFormat());
      for (int i = 0; i < byesNeeded; i++) {
        tournament.getPlayerPairs().add(PlayerPair.bye());
      }
    }
  }

  /**
   * Adds QUALIFIER pairs to the tournament if needed based on the tournament format. For QUALIF_KO: adds QUALIFIER to fill mainDrawSize with
   * qualifiers. For GROUPS_KO: adds QUALIFIER to fill nbPools * currentPoolSizes.
   *
   * @param tournament the tournament to add QUALIFIER pairs to
   */
  private void addQualifiersIfNeeded(Tournament tournament) {
    List<PlayerPair> pairs = tournament.getPlayerPairs();
    switch (tournament.getConfig().getFormat()) {
      case QUALIF_KO -> {
        Integer nbQualifiers = tournament.getConfig().getNbQualifiers();
        if (nbQualifiers != null) {
          log.debug("Adding {} QUALIFIER pairs for main draw in QUALIF_KO", nbQualifiers);
          for (int i = 0; i < nbQualifiers; i++) {
            tournament.getPlayerPairs().add(PlayerPair.qualifier(i + 1));
          }
        }
      }
      case GROUPS_KO -> {
        Integer nbPools        = tournament.getConfig().getNbPools();
        Integer nbPairsPerPool = tournament.getConfig().getNbPairsPerPool();
        if (nbPools != null && nbPairsPerPool != null) {
          int totalPairsNeeded = nbPools * nbPairsPerPool;
          int qualifiersToAdd  = totalPairsNeeded - pairs.size();
          if (qualifiersToAdd > 0) {
            log.debug("Adding {} QUALIFIER pairs to fill group draw sizes", qualifiersToAdd);
            for (int i = 0; i < qualifiersToAdd; i++) {
              tournament.getPlayerPairs().add(PlayerPair.qualifier());
            }
          }
        }
      }
    }
  }
}
