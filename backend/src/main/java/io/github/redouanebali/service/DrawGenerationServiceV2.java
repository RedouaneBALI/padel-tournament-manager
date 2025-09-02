package io.github.redouanebali.service;

import io.github.redouanebali.generationV2.TournamentBuilder;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawGenerationServiceV2 {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;

  public Tournament generateDraw(Tournament tournament) {
    assertCanInitialize(tournament);

    TournamentBuilder tournamentBuilder = new TournamentBuilder();
    List<Round>       emptyRounds       = tournamentBuilder.buildQualifKOStructure(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(emptyRounds);
    tournamentBuilder.drawLotsAndFillInitialRounds(tournament, tournament.getPlayerPairs());
    log.info("Generated draw for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  public Tournament setDraw(Tournament tournament, List<PlayerPair> qualifPairs, List<PlayerPair> mainDrawPairs) {
    assertCanInitialize(tournament);

    TournamentBuilder tournamentBuilder = new TournamentBuilder();
    List<Round>       emptyRounds       = tournamentBuilder.buildQualifKOStructure(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(emptyRounds);
    tournamentBuilder.fillInitialRoundsManual(tournament, qualifPairs, mainDrawPairs);
    log.info("Generated draw for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  private void assertCanInitialize(Tournament tournament) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
    }
  }


}