package io.github.redouanebali.model.format.knockout;

import io.github.redouanebali.dto.request.InitializeDrawRequest;
import io.github.redouanebali.dto.request.PlayerPairRequest;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.StageKey;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.List;

public class KnockoutStrategy implements FormatStrategy {

  @Override
  public void validate(TournamentFormatConfig cfg, List<String> errors) {
    if (!DrawMath.isPowerOfTwo(cfg.getMainDrawSize())) {
      errors.add("mainDrawSize must be a power of two.");
    }
    if (cfg.getNbSeeds() > cfg.getMainDrawSize()) {
      errors.add("nbSeeds > mainDrawSize.");
    }
  }

  @Override
  public List<StageKey> stages(TournamentFormatConfig cfg) {
    return List.of(StageKey.MAIN_DRAW);
  }


  @Override
  public List<Round> initializeRounds(Tournament t, List<PlayerPair> pairs, boolean manual) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    if (!manual) {
      return generator.generateAlgorithmicRounds(pairs);
    }

    // Manual: build structure only (empty games), do not auto-assign pairs.
    return generator.buildEmptyStructure(t);
  }

  @Override
  public void applyManualInitialization(final Tournament tournament, final InitializeDrawRequest req) {
    if (req == null || req.getRounds() == null || req.getRounds().isEmpty()) {
      return; // nothing to apply
    }

    // Knockout has no group stage: the first round is the first bracket round
    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return; // nothing to apply
    }
    Round firstBracket = rounds.get(0);

    var providedFirst = req.getRounds().get(0);

    // Replace games with the ones provided
    firstBracket.getGames().clear();
    providedFirst.getGames().forEach(gReq -> {
      Game g = new Game(firstBracket.getMatchFormat());
      g.setTeamA(resolveTeamSlot(tournament, gReq.getTeamA()));
      g.setTeamB(resolveTeamSlot(tournament, gReq.getTeamB()));
      g.setScore(null);
      firstBracket.getGames().add(g);
    });
    this.propagateWinners(tournament);
  }

  private PlayerPair resolveTeamSlot(final Tournament t, final PlayerPairRequest slot) {
    if (slot == null) {
      return null;
    }
    if (slot.getPairId() != null) {
      for (PlayerPair p : t.getPlayerPairs()) {
        if (slot.getPairId().equals(p.getId())) {
          return p; // reuse existing entity so names/seeds are present
        }
      }
    }
    // BYE / QUALIFIER (or PAIR not found) -> let the domain factory create the placeholder
    return PlayerPair.fromRequest(slot);
  }

  @Override
  public void propagateWinners(Tournament t) {
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(t.getConfig().getNbSeeds());
    generator.propagateWinners(t);
  }


}