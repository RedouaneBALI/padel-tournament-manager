package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.GroupStageRoundGenerator;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.generation.RoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  private final Map<TournamentFormat, RoundGenerator> generators = Map.of(
      TournamentFormat.KNOCKOUT, new KnockoutRoundGenerator(),
      TournamentFormat.GROUP_STAGE, new GroupStageRoundGenerator()
  );
  @Autowired
  private       TournamentRepository                  tournamentRepository;
  @Autowired
  private       RoundRepository                       roundRepository;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }

  public Tournament createTournament(final Tournament tournament) {
    System.out.println("TournamentService.createTournament");
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    return tournamentRepository.save(tournament);
  }

  public int addPairs(final Long tournamentId, final List<SimplePlayerPairDTO> playerPairsDto) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    List<PlayerPair> playerPairs = playerPairsDto.stream()
                                                 .map(SimplePlayerPairDTO::toPlayerPair)
                                                 .toList();
    tournament.getPlayerPairs().addAll(playerPairs);
    tournamentRepository.save(tournament);
    return tournament.getPlayerPairs().size();
  }

  public Round generateDraw(final Long tournamentId) {
    System.out.println("TournamentService.generateDraw");
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    RoundGenerator generator = new KnockoutRoundGenerator();
    if (tournament.getTournamentFormat() != null) {
      generator = generators.get(tournament.getTournamentFormat());
    } else {
      generator = new KnockoutRoundGenerator();
    }
    Round round = generator.generate(tournament.getPlayerPairs(), tournament.getNbSeeds());

    roundRepository.save(round);

    return round;
  }
}
