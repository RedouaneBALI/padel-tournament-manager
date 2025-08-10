package io.github.redouanebali.service;

import io.github.redouanebali.generation.AbstractRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {


  private final TournamentRepository tournamentRepository;

  private final DrawGenerationService drawGenerationService;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }


  @Transactional
  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    if (tournament.getNbMaxPairs() > 1) {
      AbstractRoundGenerator generator = AbstractRoundGenerator.of(tournament);
      List<Round>            rounds    = generator.initRoundsAndGames(tournament);
      tournament.getRounds().clear();
      tournament.getRounds().addAll(rounds);
    }

    Tournament savedTournament = tournamentRepository.save(tournament);
    log.info("Created tournament with id {}", savedTournament.getId());
    return savedTournament;
  }


  @Transactional
  public Tournament updateTournament(Long tournamentId, Tournament updatedTournament) {
    Tournament existing = getTournamentById(tournamentId);
    existing.setName(updatedTournament.getName());
    existing.setStartDate(updatedTournament.getStartDate());
    existing.setEndDate(updatedTournament.getEndDate());
    existing.setDescription(updatedTournament.getDescription());
    existing.setCity(updatedTournament.getCity());
    existing.setClub(updatedTournament.getClub());
    existing.setGender(updatedTournament.getGender());
    existing.setLevel(updatedTournament.getLevel());
    existing.setTournamentFormat(updatedTournament.getTournamentFormat());
    existing.setNbSeeds(updatedTournament.getNbSeeds());
    existing.setNbMaxPairs(updatedTournament.getNbMaxPairs());
    return tournamentRepository.save(existing);
  }


  /**
   * Call generator.generate() and dispatch all the players into games from the created round
   *
   * @param tournamentId the id of the tournament
   * @param manual flag indicating manual draw generation
   * @return the new Tournament
   */
  public Tournament generateDraw(Long tournamentId, boolean manual) {
    Tournament tournament = getTournamentById(tournamentId);
    return drawGenerationService.generateDraw(tournament, manual);
  }

  public Set<Game> getGamesByTournamentAndStage(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));

    return new LinkedHashSet<>(round.getGames());
  }

}
