package io.github.redouanebali.service;

import io.github.redouanebali.generation.AbstractRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawGenerationService {

  private final TournamentRepository tournamentRepository;

  public Tournament generateDraw(Tournament tournament, boolean manual) {
    AbstractRoundGenerator generator = AbstractRoundGenerator.of(tournament);
    Round                  newRound;
    if (manual) {
      newRound = generator.generateManualRound(tournament.getPlayerPairs());
    } else {
      newRound = generator.generateAlgorithmicRound(tournament.getPlayerPairs());
    }

    Round existingRound = tournament.getRounds().stream()
                                    .filter(r -> r.getStage() == newRound.getStage())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + newRound.getStage()));
    updatePools(existingRound, newRound);
    updateGames(existingRound, newRound);

    if (tournament.getTournamentFormat() != TournamentFormat.GROUP_STAGE) {
      generator.propagateWinners(tournament);
    }

    log.info("Generated draw for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }


  private void updatePools(Round existingRound, Round newRound) {
    for (Pool pool : existingRound.getPools()) {
      // Reset pool state before updating
      pool.getPairs().clear();
      pool.getPoolRanking().getDetails().clear();
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
}
