package io.github.redouanebali.service;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.strategy.StrategyResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawGenerationService {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;

  public static List<PlayerPair> capPairsToMax(Tournament tournament) {
    List<PlayerPair> pairs    = tournament.getPlayerPairs();
    int              maxPairs = tournament.getConfig().getNbMaxPairs(tournament.getFormat());

    if (pairs == null || pairs.isEmpty()) {
      return pairs == null ? List.of() : pairs;
    }
    if (maxPairs <= 0) {
      return pairs;
    }

    if (pairs.size() <= maxPairs) {
      return pairs;
    }

    log.warn("Tournament id {} has {} pairs; max is {} — using first {} only for draw generation.",
             tournament.getId(), pairs.size(), maxPairs, maxPairs);
    return new ArrayList<>(pairs.subList(0, maxPairs));
  }

  public Tournament generateDraw(Tournament tournament, boolean manual) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to generate draw for this tournament");
    }

    FormatStrategy   strategy     = StrategyResolver.resolve(tournament.getFormat());
    List<PlayerPair> pairsForDraw = capPairsToMax(tournament);
    // @todo change to void to have a full control on the tournament
    Round newRound = strategy.initializeTournament(tournament, pairsForDraw, manual);

    Round existingRound = tournament.getRounds().stream()
                                    .filter(r -> r.getStage() == newRound.getStage())
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + newRound.getStage()));
    updatePools(existingRound, newRound);
    updateGames(existingRound, newRound);

    if (tournament.getFormat() != TournamentFormat.GROUPS_KO) {
      strategy.propagateWinners(tournament);
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