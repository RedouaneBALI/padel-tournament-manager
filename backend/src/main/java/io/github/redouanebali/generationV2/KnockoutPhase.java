package io.github.redouanebali.generationV2;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
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

  /**
   * Try to place winner into a given slot. If the slot is null -> place. If the slot holds a QUALIFIER placeholder -> replace it. Otherwise do
   * nothing and return false.
   */
  private static boolean assignWinnerToSlot(Game game, boolean sideA, PlayerPair winner) {
    PlayerPair current = sideA ? game.getTeamA() : game.getTeamB();
    if (current == null || (current.getType() == PairType.QUALIFIER)) {
      if (sideA) {
        game.setTeamA(winner);
      } else {
        game.setTeamB(winner);
      }
      return true;
    }
    return false;
  }

  /**
   * Single-pass fallback: scan nextGames once and place into the first QUALIFIER placeholder; if none, the first null.
   */
  private static boolean placeBySingleScan(List<Game> nextGames, PlayerPair winner) {
    // First pass: QUALIFIER placeholders
    for (Game nextGame : nextGames) {
      if (nextGame.getTeamA() != null && nextGame.getTeamA().getType() == PairType.QUALIFIER) {
        nextGame.setTeamA(winner);
        return true;
      }
      if (nextGame.getTeamB() != null && nextGame.getTeamB().getType() == PairType.QUALIFIER) {
        nextGame.setTeamB(winner);
        return true;
      }
    }
    // Second pass: first null slot
    for (Game ng : nextGames) {
      if (ng.getTeamA() == null) {
        ng.setTeamA(winner);
        return true;
      }
      if (ng.getTeamB() == null) {
        ng.setTeamB(winner);
        return true;
      }
    }
    return false;
  }

  /**
   * Checks by reference (==) if the winner is already assigned anywhere in nextGames. This avoids false positives when equals()/seed values collide
   * with unrelated pairs.
   */
  private static boolean isAlreadyAssignedInNextByReference(List<Game> nextGames, PlayerPair winner) {
    for (Game g : nextGames) {
      if (g.getTeamA() == winner || g.getTeamB() == winner) {
        return true;
      }
    }
    return false;
  }


  @Override
  public List<String> validate(final Tournament tournament) {
    return List.of();
  }

  @Override
  public List<Round> initialize(final Tournament t) {
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
        // 2) build Q1..Qn, stop at Q3 or when games==nbQualifiers
        int slots  = drawSize; // pre-qual draw size
        int qIndex = 1;
        // Stop when games == nbQualifiers OR when reaching Q3 as per specs
        while (slots / 2 >= nbQualifiers && qIndex <= 3) {
          rounds.add(buildRound(Stage.fromQualifIndex(qIndex), slots / 2));
          slots /= 2;
          qIndex++;
        }
      }
      case MAIN_DRAW -> {
        // Build R64 / R32 / ... / FINAL using enum mapping
        int slots = drawSize;
        while (slots >= 2) {
          Stage stage = Stage.fromNbTeams(slots);
          rounds.add(buildRound(stage, slots / 2));
          slots /= 2;
        }
      }
      default -> throw new IllegalStateException("Unsupported phaseType: " + phaseType);
    }

    return rounds;
  }

  private Round buildRound(Stage stage, int nbGames) {
    Round r = new Round();
    r.setStage(stage);
    List<Game> games = new ArrayList<>(nbGames);
    for (int i = 0; i < nbGames; i++) {
      games.add(new Game()); // empty teams, empty score
    }
    r.replaceGames(games);
    return r;
  }

  @Override
  public void placeSeedTeams(final Round round, final List<PlayerPair> playerPairs, final int nbSeedsArg) {
    if (round == null || round.getGames() == null || playerPairs == null) {
      throw new IllegalArgumentException("round/games and playerPairs must not be null");
    }

    final List<Game> games     = round.getGames();
    final int        drawSlots = games.size() * 2;
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
  }

  @Override
  public List<Integer> getSeedsPositions(int drawSize, int nbSeeds) {
    List<Integer> allPositions = generateAllSeedPositions(drawSize);
    return allPositions.subList(0, Math.min(nbSeeds, allPositions.size()));
  }

  @Override
  public void placeByeTeams(final Round round,
                            final int totalPairs,
                            final int drawSize,
                            final int nbSeeds) {
    if (round == null || round.getGames() == null) {
      throw new IllegalArgumentException("round/games must not be null");
    }

    final List<Game> games = round.getGames();

    // Sanity checks
    final int slots = games.size() * 2;
    if (slots != drawSize) {
      throw new IllegalStateException("Round games do not match drawSize: games*2=" + slots + ", drawSize=" + drawSize);
    }
    if (drawSize <= 0 || (drawSize & (drawSize - 1)) != 0) {
      throw new IllegalArgumentException("drawSize must be a power of two");
    }
    if (totalPairs > drawSize) {
      throw new IllegalArgumentException("totalPairs cannot exceed drawSize");
    }

    // Count how many BYEs are already present in the round to avoid overfilling
    int existingByes = 0;
    for (Game g : games) {
      if (g.getTeamA() != null && g.getTeamA().isBye()) {
        existingByes++;
      }
      if (g.getTeamB() != null && g.getTeamB().isBye()) {
        existingByes++;
      }
    }

    int byesToPlace = Math.max(0, drawSize - totalPairs - existingByes);
    if (byesToPlace == 0) {
      return; // nothing to do
    }

    // Fallback: if no seeds are considered (manual mode), distribute BYEs opposite already placed teams first
    final int nbSeedsToConsider = Math.max(0, Math.min(nbSeeds, drawSize));
    if (nbSeedsToConsider == 0) {
      // Pass 1: for each game with exactly one non-BYE team already placed, put a BYE on the opposite side
      for (Game g : games) {
        if (byesToPlace == 0) {
          break;
        }
        boolean aOccupied = g.getTeamA() != null && !g.getTeamA().isBye();
        boolean bOccupied = g.getTeamB() != null && !g.getTeamB().isBye();
        if (aOccupied ^ bOccupied) {
          if (!aOccupied && g.getTeamA() == null) {
            g.setTeamA(PlayerPair.bye());
            byesToPlace--;
          } else if (!bOccupied && g.getTeamB() == null) {
            g.setTeamB(PlayerPair.bye());
            byesToPlace--;
          }
        }
      }
      if (byesToPlace == 0) {
        return;
      }

      // Pass 2: fill any remaining empty slots (BYE vs BYE is allowed as last resort)
      for (Game g : games) {
        if (byesToPlace == 0) {
          break;
        }
        if (g.getTeamA() == null) {
          g.setTeamA(PlayerPair.bye());
          byesToPlace--;
          if (byesToPlace == 0) {
            break;
          }
        }
        if (g.getTeamB() == null) {
          g.setTeamB(PlayerPair.bye());
          byesToPlace--;
        }
      }
      if (byesToPlace > 0) {
        throw new IllegalStateException("Not enough empty slots to place all BYEs (manual mode): remaining=" + byesToPlace);
      }
      return;
    }
    // 1) Prefer placing BYEs opposite to seeded teams (TS1 first, then TS2, ...)
    final List<Integer> seedSlots = getSeedsPositions(drawSize, nbSeedsToConsider);

    for (int i = 0; i < seedSlots.size() && byesToPlace > 0; i++) {
      int     slot      = seedSlots.get(i);
      int     gameIndex = slot / 2;
      boolean left      = (slot % 2 == 0);
      Game    g         = games.get(gameIndex);

      if (left) {
        if (g.getTeamB() == null) {
          g.setTeamB(PlayerPair.bye());
          byesToPlace--;
        }
      } else {
        if (g.getTeamA() == null) {
          g.setTeamA(PlayerPair.bye());
          byesToPlace--;
        }
      }
    }

    if (byesToPlace == 0) {
      return;
    }

    // 2) Fill matches where exactly one side is already occupied (avoid BYE vs BYE when possible)
    for (Game g : games) {
      if (byesToPlace == 0) {
        break;
      }
      boolean aEmpty = (g.getTeamA() == null);
      boolean bEmpty = (g.getTeamB() == null);
      if (aEmpty ^ bEmpty) { // exactly one empty side
        if (aEmpty) {
          g.setTeamA(PlayerPair.bye());
        } else {
          g.setTeamB(PlayerPair.bye());
        }
        byesToPlace--;
      }
    }

    if (byesToPlace == 0) {
      return;
    }

    // 3) As a last resort, allow BYE vs BYE to satisfy staggered entries
    for (Game g : games) {
      if (byesToPlace == 0) {
        break;
      }
      if (g.getTeamA() == null) {
        g.setTeamA(PlayerPair.bye());
        byesToPlace--;
        if (byesToPlace == 0) {
          break;
        }
      }
      if (g.getTeamB() == null) {
        g.setTeamB(PlayerPair.bye());
        byesToPlace--;
      }
    }

    if (byesToPlace > 0) {
      throw new IllegalStateException("Not enough empty slots to place all BYEs: remaining=" + byesToPlace);
    }
  }

  @Override
  public void placeRemainingTeamsRandomly(final Round round, final List<PlayerPair> remainingTeams) {
    // TODO implement random placement of remaining teams
  }

  @Override
  public void propagateWinners(final Tournament t) {
    if (t == null || t.getRounds() == null || t.getRounds().size() < 2) {
      return;
    }
    final List<Round> rounds = t.getRounds();
    // Process every boundary left-to-right so earlier rounds feed later ones in the same call
    for (int i = 0; i < rounds.size() - 1; i++) {
      propagateFromIndex(t, i);
    }
  }

  private void propagateFromIndex(final Tournament t, final int roundIndex) {
    final List<Round> rounds = t.getRounds();
    if (roundIndex >= rounds.size() - 1) {
      return; // base case: no next round
    }

    final Round currentRound = rounds.get(roundIndex);
    final Round nextRound    = rounds.get(roundIndex + 1);
    if (currentRound == null || nextRound == null) {
      return;
    }

    final List<Game> curGames  = currentRound.getGames();
    final List<Game> nextGames = nextRound.getGames();
    if (curGames == null || nextGames == null || nextGames.isEmpty()) {
      return;
    }

    // Skip empty rounds (no teams set at all)
    final boolean currentEmpty = curGames.stream().allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
    if (!currentEmpty) {
      final boolean classicRatio  = (nextGames.size() == curGames.size() / 2);
      final boolean sameSizeRatio = (nextGames.size() == curGames.size());
      final boolean expandedRatio = (nextGames.size() > curGames.size());

      for (int i = 0; i < curGames.size(); i++) {
        final Game currentGame = curGames.get(i);
        PlayerPair winner      = currentGame.getWinner();
        if (winner == null) {
          continue; // nothing to propagate yet
        }

        // Avoid duplicate placement only if the *same instance* is already assigned in next round.
        // Using reference identity prevents false positives when different pairs share seed/labels.
        if (!winner.isBye() && isAlreadyAssignedInNextByReference(nextGames, winner)) {
          continue;
        }

        boolean placed = false;

        if (classicRatio) {
          int idx = i / 2;
          if (idx < nextGames.size()) {
            Game ng = nextGames.get(idx);
            placed = assignWinnerToSlot(ng, (i % 2 == 0), winner);
          }
        } else if (sameSizeRatio) {
          if (i < nextGames.size()) {
            Game ng = nextGames.get(i);
            placed = assignWinnerToSlot(ng, true, winner) || assignWinnerToSlot(ng, false, winner);
          }
        } else if (expandedRatio) {
          // Staggered entry (e.g., Q2 -> R64). Prefer QUALIFIER placeholders, then first null slot.
          placed = placeBySingleScan(nextGames, winner);
        }

        if (!placed) {
          // Final fallback for any unusual layout: single scan
          //  placeBySingleScan(nextGames, winner);
        }
      }
    }
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