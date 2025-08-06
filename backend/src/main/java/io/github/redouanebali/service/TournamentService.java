package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.AbstractRoundGenerator;
import io.github.redouanebali.generation.GroupRoundGenerator;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {


  private final TournamentRepository tournamentRepository;

  private final TournamentProgressionService progressionService;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }

  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    Tournament savedTournament = tournamentRepository.save(tournament);
    log.info("Created tournament with id {}", savedTournament.getId());

    if (savedTournament.getNbMaxPairs() <= 1) {
      return savedTournament;
    }

    AbstractRoundGenerator generator = getGenerator(savedTournament);

    Set<Round> rounds = generator.createRounds(savedTournament);
    savedTournament.setRounds(rounds);
    return tournamentRepository.save(savedTournament);
  }

  private AbstractRoundGenerator getGenerator(Tournament tournament) {
    return switch (tournament.getTournamentFormat()) {
      case KNOCKOUT -> new KnockoutRoundGenerator(tournament.getNbSeeds());
      case GROUP_STAGE -> new GroupRoundGenerator(tournament.getNbSeeds(), tournament.getNbPools(), tournament.getNbPairsPerPool());
      default -> new KnockoutRoundGenerator(tournament.getNbSeeds());
    };
  }


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

  public Tournament addPairs(Long tournamentId, List<SimplePlayerPairDTO> playerPairsDto) {
    Tournament tournament = getTournamentById(tournamentId);

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

  /**
   * Call generator.generate() and dispatch all the players into games from the created round
   *
   * @param tournamentId the id of the tournament
   * @return the new Tournament
   */
  public Tournament generateDraw(Long tournamentId) {
    Tournament             tournament = getTournamentById(tournamentId);
    AbstractRoundGenerator generator  = getGenerator(tournament);
    Round                  newRound   = generator.generate();

    Round existingRound = getRoundByStage(tournament, newRound.getStage());

    updatePools(existingRound, newRound);
    updateGames(existingRound, newRound);

    tournament.setRounds(new LinkedHashSet<>(tournament.getRounds()));

    if (tournament.getTournamentFormat() != TournamentFormat.GROUP_STAGE) {
      progressionService.propagateWinners(tournament);
    }

    log.info("Generated draw for tournament id {}", tournamentId);
    return tournamentRepository.save(tournament);
  }

  private void updatePools(Round existingRound, Round newRound) {
    for (Pool pool : existingRound.getPools()) {
      for (Pool newPool : newRound.getPools()) {
        if (pool.getName().equals(newPool.getName())) {
          pool.initPairs(newPool.getPairs());
        }
      }
    }
  }

  private void updateGames(Round existingRound, Round newRound) {
    List<Game> existingGames = existingRound.getGames();
    List<Game> newGames      = newRound.getGames();
    for (int i = 0; i < existingGames.size() && i < newGames.size(); i++) {
      Game existingGame = existingGames.get(i);
      Game newGame      = newGames.get(i);
      existingGame.setTeamA(newGame.getTeamA());
      existingGame.setTeamB(newGame.getTeamB());
    }
  }


  public MatchFormat getMatchFormatForRound(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);
    return tournament.getRounds().stream()
                     .filter(round -> round.getStage() == stage)
                     .findFirst()
                     .map(Round::getMatchFormat)
                     .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
  }

  public MatchFormat updateMatchFormatForRound(Long tournamentId, Stage stage, MatchFormat newFormat) {
    Tournament tournament = getTournamentById(tournamentId);
    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
    MatchFormat currentFormat = round.getMatchFormat();

    if (currentFormat == null) {
      round.setMatchFormat(newFormat);
    } else {
      currentFormat.setNumberOfSetsToWin(newFormat.getNumberOfSetsToWin());
      currentFormat.setPointsPerSet(newFormat.getPointsPerSet());
      currentFormat.setAdvantage(newFormat.isAdvantage());
      currentFormat.setSuperTieBreakInFinalSet(newFormat.isSuperTieBreakInFinalSet());
    }
    tournamentRepository.save(tournament);
    return newFormat;
  }


  public List<PlayerPair> getPairsByTournamentId(Long tournamentId) {
    Tournament tournament = getTournamentById(tournamentId);
    return tournament.getPlayerPairs();
  }

  public Set<Game> getGamesByTournamentAndStage(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));

    return new LinkedHashSet<>(round.getGames());
  }

  private Round getRoundByStage(Tournament tournament, Stage stage) {
    return tournament.getRounds().stream()
                     .filter(r -> r.getStage() == stage)
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
  }

}
