package io.github.redouanebali.api.service;

import io.github.redouanebali.api.dto.request.RoundRequest;
import io.github.redouanebali.generation.TournamentBuilder;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
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

  public Tournament generateDrawAuto(Tournament tournament) {
    assertCanInitialize(tournament);

    TournamentBuilder.setupAndPopulateTournament(tournament, capPairsToMax(tournament));

    log.info("Generated draw (auto) for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  public Tournament generateDrawManual(Tournament tournament, List<RoundRequest> initialRounds) {
    assertCanInitialize(tournament);

    if (initialRounds == null || initialRounds.isEmpty()) {
      throw new IllegalArgumentException(
          "Manual draw generation requires initial rounds to be provided. Use generateDrawAuto() for automatic generation instead.");
    }

    List<Round> convertedRounds = initialRounds.stream()
                                               .map(req -> RoundRequest.toModel(req, tournament))
                                               .toList();
    TournamentBuilder.setupTournamentWithInitialRounds(tournament, convertedRounds);

    log.info("Generated draw (manual) for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  public void propagateWinners(Tournament tournament) {
    TournamentBuilder.propagateWinners(tournament);
  }

  private void assertCanInitialize(Tournament tournament) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
    }
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