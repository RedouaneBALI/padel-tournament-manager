package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.InitializeDrawRequest;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
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
    List<Round>      rounds       = strategy.initializeRounds(tournament, pairsForDraw, manual);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    if (tournament.getFormat() != TournamentFormat.GROUPS_KO) {
      strategy.propagateWinners(tournament);
    }

    log.info("Generated draw for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  /**
   * Initialize a draw from a client-provided first-round assignment (manual construction). Reuses the normal generation flow to build the whole
   * bracket structure, then replaces ONLY the first bracket round games with the provided team assignments.
   */
  public Tournament initializeManualDraw(Tournament tournament, InitializeDrawRequest request) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
    }
    if (request == null || request.getRounds() == null || request.getRounds().isEmpty()) {
      throw new IllegalArgumentException("InitializeDrawRequest.rounds must contain at least one round");
    }

    // 1) Build the whole structure using the existing strategy (manual=true keeps first round empty/placeholder)
    FormatStrategy   strategy     = StrategyResolver.resolve(tournament.getFormat());
    List<PlayerPair> pairsForDraw = capPairsToMax(tournament);
    List<Round>      rounds       = strategy.initializeRounds(tournament, pairsForDraw, true);

    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // 2) Find the first bracket round (skip GROUPS if present)
    List<Round> builtRounds     = tournament.getRounds();
    int         firstBracketIdx = 0;
    while (firstBracketIdx < builtRounds.size() && builtRounds.get(firstBracketIdx).getStage() == Stage.GROUPS) {
      firstBracketIdx++;
    }
    if (firstBracketIdx >= builtRounds.size()) {
      throw new IllegalStateException("No bracket rounds available to initialize");
    }
    Round firstBracket = builtRounds.get(firstBracketIdx);

    // 3) Replace its games with the ones provided in the request (respecting order)
    var providedFirst = request.getRounds().get(0);

    // Clear and rebuild the games of the first bracket round
    firstBracket.getGames().clear();
    for (var gReq : providedFirst.getGames()) {
      Game       g     = new Game(firstBracket.getMatchFormat());
      PlayerPair teamA = null;
      if (gReq.getTeamA() != null) {
        teamA = tournament.getPlayerPairs()
                          .stream()
                          .filter(p -> p.getId().equals(gReq.getTeamA().getPairId()))
                          .findFirst()
                          .orElse(PlayerPair.fromRequest(gReq.getTeamA())); // fallback BYE/QUALIFIER
      }
      g.setTeamA(teamA);
      PlayerPair teamB = null;
      if (gReq.getTeamB() != null) {
        teamB = tournament.getPlayerPairs()
                          .stream()
                          .filter(p -> p.getId().equals(gReq.getTeamB().getPairId()))
                          .findFirst()
                          .orElse(PlayerPair.fromRequest(gReq.getTeamB())); // fallback BYE/QUALIFIER
      }
      g.setTeamB(teamB);
      firstBracket.getGames().add(g);
    }

    // 4) Propagate placeholders/winners to later rounds (structure already exists)
    if (tournament.getFormat() != TournamentFormat.GROUPS_KO) {
      strategy.propagateWinners(tournament);
    }

    log.info("Initialized manual draw for tournament id {} ({} rounds)", tournament.getId(), builtRounds.size());
    return tournamentRepository.save(tournament);
  }

}