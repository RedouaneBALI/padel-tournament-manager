package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
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
    // Add BYE pairs if needed to reach main draw size
    Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
    if (mainDrawSize != null && pairs.size() < mainDrawSize) {
      int byesNeeded = mainDrawSize - pairs.size();
      log.debug("Adding {} BYE pairs to reach main draw size of {}", byesNeeded, mainDrawSize);
      for (int i = 0; i < byesNeeded; i++) {
        tournament.getPlayerPairs().add(PlayerPair.bye());
      }
    }
    return tournamentRepository.save(tournament);
  }

  /**
   * Retrieves all player pairs for a tournament. Can optionally exclude BYE pairs from the result.
   *
   * @param tournamentId the tournament ID
   * @param includeByes whether to include BYE pairs in the result
   * @return list of player pairs, optionally excluding BYEs
   * @throws IllegalArgumentException if tournament is not found
   */
  public List<PlayerPair> getPairsByTournamentId(Long tournamentId, boolean includeByes) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException(TOURNAMENT_NOT_FOUND));
    if (includeByes) {
      return new ArrayList<>(tournament.getPlayerPairs());
    } else {
      return tournament.getPlayerPairs().stream().filter(pp -> !pp.isBye()).toList();
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

}
