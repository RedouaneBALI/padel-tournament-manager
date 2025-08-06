package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.model.Player;
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

  public Tournament addPairs(Long tournamentId, List<SimplePlayerPairDTO> playerPairsDto) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    List<PlayerPair> newPairs = playerPairsDto.stream().map(dto -> {
      Player p1 = new Player();
      p1.setName(dto.getPlayer1());

      Player p2 = new Player();
      p2.setName(dto.getPlayer2());

      return new PlayerPair(null, p1, p2, dto.getSeed());
    }).toList();

    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(newPairs);
    return tournamentRepository.save(tournament);
  }

  public List<PlayerPair> getPairsByTournamentId(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    return tournament.getPlayerPairs();
  }
}
