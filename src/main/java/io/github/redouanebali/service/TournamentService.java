package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentHelper;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  @Autowired
  private TournamentRepository tournamentRepository;

  @Autowired
  private GameRepository gameRepository;

  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    return tournamentRepository.save(tournament);
  }

  public List<Game> generateDraw(final Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    List<Game> games = TournamentHelper.generateGames(tournament.getPlayerPairs(), tournament.getNbSeeds());

    // Optionnel : persister les games
    gameRepository.saveAll(games);

    return games;
  }
}
