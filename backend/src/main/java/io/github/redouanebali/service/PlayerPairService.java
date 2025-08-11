package io.github.redouanebali.service;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.config.SecurityUtil;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerPairService {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;

  public Tournament addPairs(Long tournamentId, List<PlayerPair> playerPairs) {
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
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(playerPairs);
    return tournamentRepository.save(tournament);
  }

  public List<PlayerPair> getPairsByTournamentId(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    return tournament.getPlayerPairs().stream().filter(pp -> !pp.isBye()).toList();
  }
}
