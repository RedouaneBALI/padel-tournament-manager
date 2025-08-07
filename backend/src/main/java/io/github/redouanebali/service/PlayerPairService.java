package io.github.redouanebali.service;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerPairService {

  private final TournamentRepository tournamentRepository;

  public Tournament addPairs(Long tournamentId, List<PlayerPair> playerPairs) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
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
