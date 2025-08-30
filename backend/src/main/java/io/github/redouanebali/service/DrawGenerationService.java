package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.InitializeDrawRequest;
import io.github.redouanebali.generation.AbstractRoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.repository.PlayerPairRepository;
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
  private final PlayerPairRepository playerPairRepository;

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

    strategy.propagateWinners(tournament);

    log.info("Generated draw for tournament id {}", tournament.getId());
    return tournamentRepository.save(tournament);
  }

  /**
   * Initialize a draw from a client-provided first-round assignment (manual construction). Reuses the normal generation flow to build the whole
   * bracket structure, then replaces ONLY the first bracket round games with the provided team assignments.
   */
  public Tournament initializeManualDraw(Tournament tournament, InitializeDrawRequest request) {
    assertCanInitialize(tournament);
    if (request == null || request.getRounds() == null || request.getRounds().isEmpty()) {
      throw new IllegalArgumentException("InitializeDrawRequest.rounds must contain at least one round");
    }

    // Persist any BYE/QUALIFIER pairs that haven't been persisted yet (id==null)
    // (in case they are present in the manual assignment)
    List<PlayerPair> specialPairs = tournament.getPlayerPairs().stream()
                                              .filter(pp -> (pp.isBye() || pp.isQualifier()) && pp.getId() == null)
                                              .toList();
    if (!specialPairs.isEmpty()) {
      playerPairRepository.saveAll(specialPairs);
    }

    // Build the whole structure (manual = true keeps first round empty/placeholder)
    buildStructure(tournament);

    // Let the format strategy decide how to apply the manual initialization (KO, QUALIF_KO, GROUPS_KO...)
    FormatStrategy strategy = StrategyResolver.resolve(tournament.getFormat());
    strategy.applyManualInitialization(tournament, request);

    log.info("Initialized manual draw for tournament id {} ({} rounds)", tournament.getId(), tournament.getRounds().size());
    return tournamentRepository.save(tournament);
  }

  private void assertCanInitialize(Tournament tournament) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
    }
  }

  private void buildStructure(Tournament tournament) {
    FormatStrategy   strategy     = StrategyResolver.resolve(tournament.getFormat());
    List<PlayerPair> pairsForDraw = capPairsToMax(tournament);
    List<Round>      rounds       = strategy.initializeRounds(tournament, pairsForDraw, true);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);
  }

  public Tournament sortManualPairs(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    if (tournament.getConfig().getDrawMode() != DrawMode.MANUAL) {
      return tournament;
    }
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to modify pairs for this tournament");
    }

    Integer mainDrawSize = tournament.getConfig().getMainDrawSize();
    if (mainDrawSize == null) {
      throw new IllegalStateException("Main draw size is not set");
    }

    List<PlayerPair> manualPairs = tournament.getPlayerPairs().stream()
                                             .filter(pp -> !pp.isBye())
                                             .toList();
    int byesNeeded = mainDrawSize - manualPairs.size();
    if (byesNeeded < 0) {
      throw new IllegalStateException("More pairs than main draw size");
    }

    List<PlayerPair> newPairs = new ArrayList<>(mainDrawSize);
    // Initialize with nulls
    for (int i = 0; i < mainDrawSize; i++) {
      newPairs.add(null);
    }

    List<Integer> seedPositions = AbstractRoundGenerator.getSeedsPositions(mainDrawSize, tournament.getConfig().getNbSeeds());
    int           byeInserted   = 0;
    int           manualIndex   = 0;

    // Place BYE pairs at seed positions first
    for (Integer pos : seedPositions) {
      if (byeInserted < byesNeeded) {
        if (pos % 2 == 0) {
          newPairs.set(pos + 1, PlayerPair.bye());
        } else {
          newPairs.set(pos - 1, PlayerPair.bye());
        }
        byeInserted++;
      }
    }

    // Fill remaining positions
    for (int i = 0; i < mainDrawSize; i++) {
      if (newPairs.get(i) == null) {
        newPairs.set(i, manualPairs.get(manualIndex));
        manualIndex++;
      }
    }

    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(newPairs);

    return tournamentRepository.save(tournament);
  }

}