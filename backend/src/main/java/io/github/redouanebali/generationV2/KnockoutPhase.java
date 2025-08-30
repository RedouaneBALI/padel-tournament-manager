package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KnockoutPhase implements TournamentPhase {

  private int       drawSize;
  private int       nbSeeds;
  private PhaseType phaseType;
  private DrawMode  drawMode;


  @Override
  public Tournament initialize(final Tournament t) {
    // 0) Sanity
    if (t == null) {
      throw new IllegalArgumentException("tournament is null");
    }
    if (drawSize <= 0 || (drawSize & (drawSize - 1)) != 0) {
      throw new IllegalArgumentException("drawSize must be a power of two");
    }

    final List<Round> rounds = new ArrayList<>();

    switch (phaseType) {
      case QUALIFS -> {
        // 1) fetch nbQualifiers from config
        final int nbQualifiers =
            (t.getConfig() != null) ? t.getConfig().getNbQualifiers() : 0;
        if (nbQualifiers <= 0 || (nbQualifiers & (nbQualifiers - 1)) != 0 || nbQualifiers > drawSize) {
          throw new IllegalArgumentException("nbQualifiers must be a power of two in (0, drawSize]");
        }
        // 2) build Q1..Qn
        int slots  = drawSize; // preQual draw size
        int qIndex = 1;
        while (slots / 2 >= nbQualifiers) { // stop when games==nbQualifiers
          rounds.add(buildRound(qualifStage(qIndex), slots / 2));
          slots /= 2;
          qIndex++;
        }
      }
      case MAIN_DRAW -> {
        // build R64 / R32 / ... / FINALE
        int   slots = drawSize;
        Stage stage = stageForSlots(slots); // R64/R32/R16/QUARTERS/SEMIS/FINALE
        while (slots >= 2) {
          rounds.add(buildRound(stage, slots / 2));
          slots /= 2;
          stage = stageForSlots(slots); // next stage label
        }
      }
      default -> throw new IllegalStateException("Unsupported phaseType: " + phaseType);
    }

    // 3) attach to tournament (append or set depending on your model)
    //    If your Tournament already has a list of rounds per phase,
    //    either t.getRounds().addAll(rounds) or set them on a phase object.
    t.getRounds().addAll(rounds);
    return t;
  }

  private Round buildRound(Stage stage, int nbGames) {
    Round r = new Round();
    r.setStage(stage);
    List<Game> games = new ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      games.add(new Game()); // empty teams, empty score
    }
    r.getGames().clear();
    r.getGames().addAll(games); // si tu n’as pas de setter, utilise le builder/all-args
    return r;
  }

  private Stage qualifStage(int qIndex) {
    return switch (qIndex) {
      case 1 -> Stage.Q1;
      case 2 -> Stage.Q2;
      case 3 -> Stage.Q3;
      default -> throw new IllegalArgumentException("App supports up to Q3 as per specs");
    };
  }

  private Stage stageForSlots(int slots) {
    return switch (slots) {
      case 64 -> Stage.R64;
      case 32 -> Stage.R32;
      case 16 -> Stage.R16;
      case 8 -> Stage.QUARTERS;
      case 4 -> Stage.SEMIS;
      case 2 -> Stage.FINAL;
      case 1 -> Stage.WINNER;
      default -> throw new IllegalArgumentException("Unsupported slots: " + slots);
    };
  }

  @Override
  public Round placeSeedTeams(final List<Game> games, final List<PlayerPair> playerPairs, final int nbSeedsArg) {
    if (games == null || playerPairs == null) {
      throw new IllegalArgumentException("games and playerPairs must not be null");
    }

    final int drawSlots = games.size() * 2;
    if (this.drawSize != 0 && this.drawSize != drawSlots) {
      throw new IllegalStateException("Configured drawSize=" + this.drawSize + " but games provide drawSlots=" + drawSlots);
    }
    final int nbSeedsToPlace = Math.min(nbSeedsArg, Math.min(this.nbSeeds, playerPairs.size()));

    // Sort pairs by ascending seed (1 = best seed)
    final List<PlayerPair> sortedBySeed = new ArrayList<>(playerPairs);
    sortedBySeed.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Theoretical seed positions in a bracket of size drawSlots
    final List<Integer> seedSlots = getSeedsPositions(drawSlots, nbSeedsToPlace);

    // Placement: slot -> (gameIndex, side)
    for (int i = 0; i < nbSeedsToPlace; i++) {
      int      slot      = seedSlots.get(i);
      int      gameIndex = slot / 2;
      TeamSide side      = (slot % 2 == 0) ? TeamSide.TEAM_A : TeamSide.TEAM_B;

      Game       g        = games.get(gameIndex);
      PlayerPair seedPair = sortedBySeed.get(i);

      // Place the pair on the correct side, without overwriting any existing assignment
      if (side == TeamSide.TEAM_A) {
        if (g.getTeamA() == null) {
          g.setTeamA(seedPair);
        } else {
          throw new IllegalStateException("Seed slot already occupied: game=" + gameIndex + ", side=TEAM_A");
        }
      } else { // TEAM_B
        if (g.getTeamB() == null) {
          g.setTeamB(seedPair);
        } else {
          throw new IllegalStateException("Seed slot already occupied: game=" + gameIndex + ", side=TEAM_B");
        }
      }
    }

    // Return a Round containing these games (reuse existing if present)
    Round r = new Round();
    r.getGames().clear();
    r.getGames().addAll(games);
    return r;
  }


  @Override
  public List<Integer> getSeedsPositions(int drawSize, int nbSeeds) {
    List<Integer> allPositions = generateAllSeedPositions(drawSize);
    return allPositions.subList(0, Math.min(nbSeeds, allPositions.size()));
  }

  @Override
  public Round placeByeTeams(final List<Game> games, final int totalPairs, final int drawSize, final int nbSeeds) {
    return null;
  }

  @Override
  public Round placeRemainingTeamsRandomly(final List<Game> g, final List<PlayerPair> remainingTeams) {
    return null;
  }

  @Override
  public Tournament propagateWinners(final Tournament t) {
    return null;
  }

  @Override
  public Round setRoundGames(final Round r, final List<Game> g) {
    return null;
  }

  /**
   * Generate seed positions for a perfect bracket (nbTeams must be a power of 2)
   */
  private List<Integer> generatePerfectSeedPositions(int nbTeams) {
    if (nbTeams == 1) {
      return Collections.singletonList(0);
    }

    List<Integer> prev   = generatePerfectSeedPositions(nbTeams / 2);
    List<Integer> result = new ArrayList<>();

    for (int i = 0; i < prev.size(); i++) {
      int pos = prev.get(i);
      if (i % 2 == 0) {
        // For even indices, place the position in the first half
        result.add(pos);
        result.add(nbTeams - 1 - pos);
      } else {
        // For odd indices, invert the order
        result.add(nbTeams - 1 - pos);
        result.add(pos);
      }
    }

    return result;
  }

  /**
   * Generate all the possible position recursively from the bracket structure
   */
  private List<Integer> generateAllSeedPositions(int drawSize) {
    if (drawSize <= 1) {
      return Collections.singletonList(0);
    }

    int powerOfTwo = 1;
    while (powerOfTwo < drawSize) {
      powerOfTwo *= 2;
    }

    List<Integer> fullPositions = generatePerfectSeedPositions(powerOfTwo);

    return fullPositions.subList(0, drawSize);
  }
}
