package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.generation.TournamentBuilder;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.AuthorizationService;
import io.github.redouanebali.security.SecurityUtil;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawGenerationService {

  private final TournamentRepository tournamentRepository;
  private final AuthorizationService authorizationService;
  private final EntityManager        entityManager;

  public static List<PlayerPair> capPairsToMax(Tournament tournament) {
    List<PlayerPair> pairs    = tournament.getPlayerPairs();
    int              maxPairs = tournament.getConfig().getNbMaxPairs();

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

  @Transactional
  public Tournament generateDrawManual(Tournament tournament, List<RoundRequest> initialRounds) {
    assertCanInitialize(tournament);

    if (initialRounds == null || initialRounds.isEmpty()) {
      throw new IllegalArgumentException(
          "Manual draw generation requires initial rounds to be provided.");
    }

    List<Round> convertedRounds = initialRounds.stream()
                                               .map(req -> RoundRequest.toModel(req, tournament))
                                               .toList();
    TournamentBuilder.setupTournamentWithInitialRounds(tournament, convertedRounds);

    log.info("Generated draw (manual) for tournament id {}", tournament.getId());
    Tournament saved = tournamentRepository.save(tournament);

    // CRITICAL: Force flush to database before returning
    // This ensures data is persisted immediately, especially important for remote databases (Cloud SQL)
    // Without this, H2 works fine but Cloud SQL may have latency issues
    entityManager.flush();

    log.debug("Flushed tournament {} to database", saved.getId());
    return saved;
  }

  @Transactional
  public void propagateWinners(Tournament tournament) {
    TournamentBuilder.propagateWinners(tournament);
    // Ensure changes are flushed to database
    entityManager.flush();
    log.debug("Propagated and flushed winners for tournament {}", tournament.getId());
  }

  @Transactional
  public void propagateWinnersFromGame(Tournament tournament, Game game) {
    TournamentBuilder.propagateWinnersFromGame(tournament, game);
    entityManager.flush();
    log.debug("Propagated and flushed winners from game {} for tournament {}", game.getId(), tournament.getId());
  }


  private void assertCanInitialize(Tournament tournament) {
    authorizationService.requireTournamentEditPermission(tournament, SecurityUtil.currentUserId());
  }


  public void validate(final Tournament tournament) {
    List<String> errors = TournamentBuilder.validate(tournament);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid tournament configuration: " + String.join(", ", errors));
    }
  }

  public void initializeStructure(Tournament tournament) {
    if (tournament == null || tournament.getConfig() == null || tournament.getConfig().getFormat() == null) {
      throw new IllegalArgumentException("Tournoi ou configuration invalide");
    }
    TournamentBuilder.initializeEmptyRounds(tournament);
  }
}