package io.github.redouanebali.service;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentHelper;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  @Autowired
  private TournamentRepository tournamentRepository;

  @Autowired
  private RoundRepository roundRepository;

  public Tournament createTournament(final Tournament tournament) {
    System.out.println("TournamentService.createTournament");
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    return tournamentRepository.save(tournament);
  }

  public Round generateDraw(final Long tournamentId) {
    System.out.println("TournamentService.generateDraw");
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    Round round = TournamentHelper.generateGames(tournament.getPlayerPairs(), tournament.getNbSeeds());

    // Optionnel : persister les rounds
    roundRepository.save(round);

    return round;
  }
}
