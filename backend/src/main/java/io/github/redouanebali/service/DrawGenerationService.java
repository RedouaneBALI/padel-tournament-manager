package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.generation.TournamentBuilder;
import io.github.redouanebali.generation.strategy.DrawStrategy;
import io.github.redouanebali.generation.strategy.DrawStrategyFactory;
import io.github.redouanebali.generation.strategy.ManualDrawStrategy;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
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
  TournamentBuilder tournamentBuilder = new TournamentBuilder();

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

  public void initialize(Tournament tournament) {
    assertCanInitialize(tournament);
    List<Round> emptyRounds = tournamentBuilder.buildQualifKOStructure(tournament.getConfig());
    tournament.getRounds().clear();
    tournament.getRounds().addAll(emptyRounds);
  }

  public Tournament generateDrawAuto(Tournament tournament) {
    initialize(tournament);

    // Use the new strategy pattern instead of calling TournamentBuilder directly
    DrawMode     drawMode     = tournament.getConfig().getDrawMode();
    DrawStrategy drawStrategy = DrawStrategyFactory.createStrategy(drawMode);

    List<PlayerPair> players = capPairsToMax(tournament);
    drawStrategy.placePlayers(tournament, players);

    log.info("Generated draw (auto) for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  public Tournament generateDrawManual(Tournament tournament, List<RoundRequest> initialRounds) {
    initialize(tournament);

    if (initialRounds != null && !initialRounds.isEmpty()) {
      // Use the manual strategy with predefined rounds
      List<Round> convertedRounds = initialRounds.stream()
                                                 .map(req -> RoundRequest.toModel(req, tournament))
                                                 .toList();
      ManualDrawStrategy manualStrategy = new ManualDrawStrategy();
      manualStrategy.replaceInitialRounds(tournament, convertedRounds);
    } else {
      // Use the manual strategy with players in order
      DrawStrategy     drawStrategy = DrawStrategyFactory.createStrategy(DrawMode.MANUAL);
      List<PlayerPair> players      = capPairsToMax(tournament);
      drawStrategy.placePlayers(tournament, players);
    }

    log.info("Generated draw (manual) for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  public void propagateWinners(Tournament tournament) {
    tournamentBuilder.propagateWinners(tournament);
  }

  private void assertCanInitialize(Tournament tournament) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
    }
  }


  public void validate(final Tournament tournament) {
    List<String> errors = tournamentBuilder.validate(tournament);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid tournament configuration: " + String.join(", ", errors));
    }
  }
}