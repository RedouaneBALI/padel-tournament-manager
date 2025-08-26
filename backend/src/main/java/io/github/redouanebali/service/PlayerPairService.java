package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.generation.AbstractRoundGenerator;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerPairService {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;
  private final TournamentMapper     tournamentMapper;

  public Tournament addPairs(Long tournamentId, List<CreatePlayerPairRequest> requests) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }
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
      for (int i = 0; i < byesNeeded; i++) {
        tournament.getPlayerPairs().add(PlayerPair.bye());
      }
    }
    return tournamentRepository.save(tournament);
  }

  public List<PlayerPair> getPairsByTournamentId(Long tournamentId, boolean includeByes) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    if (includeByes) {
      return new ArrayList<>(tournament.getPlayerPairs());
    } else {
      return tournament.getPlayerPairs().stream().filter(pp -> !pp.isBye()).toList();
    }
  }

  @Transactional
  public void updatePlayerPair(Long tournamentId, Long pairId, String player1Name, String player2Name, Integer seed) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

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

  public Tournament sortManualPairs(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }

    Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
    if (mainDrawSize == null) {
      throw new IllegalStateException("Main draw size is not set");
    }

    List<PlayerPair> manualPairs = tournament.getPlayerPairs().stream()
                                             .filter(pp -> !pp.isBye())
                                             .toList();
    int byesNeeded = mainDrawSize - manualPairs.size();
    if (byesNeeded < 0) {
      throw new IllegalStateException("More pairs than main draw size");
    }

    List<PlayerPair> newPairs = new ArrayList<>(mainDrawSize);
    // Initialize with nulls
    for (int i = 0; i < mainDrawSize; i++) {
      newPairs.add(null);
    }

    List<Integer> seedPositions = AbstractRoundGenerator.getSeedsPositions(mainDrawSize, tournament.getConfig().getNbSeeds());
    int           byeInserted   = 0;
    int           manualIndex   = 0;

    // Place BYE pairs at seed positions first
    for (Integer pos : seedPositions) {
      if (byeInserted < byesNeeded) {
        if (pos % 2 == 0) {
          newPairs.set(pos + 1, PlayerPair.bye());
        } else {
          newPairs.set(pos - 1, PlayerPair.bye());
        }
        byeInserted++;
      }
    }

    // Fill remaining positions
    for (int i = 0; i < mainDrawSize; i++) {
      if (newPairs.get(i) == null) {
        newPairs.set(i, manualPairs.get(manualIndex));
        manualIndex++;
      }
    }

    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(newPairs);

    return tournamentRepository.save(tournament);
  }
}
