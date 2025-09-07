package io.github.redouanebali.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    // Allow placing more teams than the original nbSeeds if needed (for BYE protection)
    int nbSeedsToPlace;
    if (playerPairs.size() < nbSeedsArg) {
      nbSeedsToPlace = io.github.redouanebali.model.format.DrawMath.largestPowerOfTwoLE(playerPairs.size());
    } else {
      nbSeedsToPlace = nbSeedsArg;
    }
    if (nbSeedsToPlace == 0) {
      return;
    }

    // Sort pairs: real seeds (1, 2, 3...) first, then non-seeds (seed <= 0) at the end
    final List<PlayerPair> sortedBySeed = new ArrayList<>(playerPairs);
    sortedBySeed.sort((pair1, pair2) -> {
      int seed1 = pair1.getSeed();
      int seed2 = pair2.getSeed();

      // If both have real seeds (> 0), sort by ascending seed
      if (seed1 > 0 && seed2 > 0) {
        return Integer.compare(seed1, seed2);
      }
      // If one has real seed and other doesn't, real seed comes first
      if (seed1 > 0) {
        return -1;
      }
      if (seed2 > 0) {
        return 1;
      }
      // If both have no real seed (both <= 0), maintain original order
      return 0;
    });

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
    if (nbSeeds == 0) {
      return new ArrayList<>();
    }

    return loadSeedPositionsFromJson(drawSize, nbSeeds);
  }

  /**
   * Load seed positions from JSON file and randomly assign positions for each seed group. TS1 and TS2 are fixed, TS3+ are randomly selected from
   * available positions.
   */
  private List<Integer> loadSeedPositionsFromJson(int drawSize, int nbSeeds) {
    try {
      // Load JSON from resources
      InputStream inputStream = getClass().getResourceAsStream("/seed_positions.json");
      if (inputStream == null) {
        throw new IllegalStateException("seed_positions.json not found in resources");
      }

      ObjectMapper mapper   = new ObjectMapper();
      JsonNode     rootNode = mapper.readTree(inputStream);

      // Navigate to the correct drawSize and nbSeeds
      JsonNode drawSizeNode = rootNode.get(String.valueOf(drawSize));
      if (drawSizeNode == null) {
        throw new IllegalArgumentException("DrawSize " + drawSize + " not supported in seed_positions.json");
      }

      JsonNode nbSeedsNode = drawSizeNode.get(String.valueOf(nbSeeds));
      if (nbSeedsNode == null) {
        throw new IllegalArgumentException("NbSeeds " + nbSeeds + " not supported for drawSize " + drawSize);
      }

      List<Integer> positions = new ArrayList<>();

      // Process seed groups in order: TS1, TS2, TS3-4, TS5-8, etc.
      Iterator<String> fieldNames   = nbSeedsNode.fieldNames();
      List<String>     sortedFields = new ArrayList<>();
      fieldNames.forEachRemaining(sortedFields::add);

      // Sort to ensure proper order: TS1, TS2, TS3-4, TS5-8, TS9-16, TS17-32
      sortedFields.sort((a, b) -> {
        if (a.equals("TS1")) {
          return -1;
        }
        if (b.equals("TS1")) {
          return 1;
        }
        if (a.equals("TS2")) {
          return -1;
        }
        if (b.equals("TS2")) {
          return 1;
        }
        return a.compareTo(b);
      });

      for (String groupKey : sortedFields) {
        JsonNode      positionsArray = nbSeedsNode.get(groupKey);
        List<Integer> groupPositions = new ArrayList<>();

        // Collect all positions for this group
        for (JsonNode posNode : positionsArray) {
          groupPositions.add(posNode.asInt());
        }

        // For TS1 and TS2, use the fixed position
        if ("TS1".equals(groupKey) || "TS2".equals(groupKey)) {
          positions.addAll(groupPositions);
        } else {
          // For TS3+, shuffle the positions to simulate random draw
          Collections.shuffle(groupPositions);
          positions.addAll(groupPositions);
        }
      }

      return positions.subList(0, Math.min(nbSeeds, positions.size()));

    } catch (Exception e) {
      throw new RuntimeException("Failed to load seed positions from JSON", e);
    }
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

    // Count how many BYES are already present in the round to avoid overfilling
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

    // Only place BYES opposite to seeded teams (optimal tournament strategy)
    final int nbSeedsToConsider = Math.max(0, Math.min(nbSeeds, drawSize));
    if (nbSeedsToConsider > 0) {
      // Place BYEs opposite to seeded teams (TS1 first, then TS2, ...)
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
    }

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
    if (round == null || round.getGames() == null || remainingTeams == null || remainingTeams.isEmpty()) {
      return;
    }
    List<PlayerPair> shuffled = new ArrayList<>(remainingTeams);
    Collections.shuffle(shuffled);

    int index = 0;
    for (Game game : round.getGames()) {
      if (game.getTeamA() == null && index < shuffled.size()) {
        game.setTeamA(shuffled.get(index++));
      }
      if (game.getTeamB() == null && index < shuffled.size()) {
        game.setTeamB(shuffled.get(index++));
      }
      if (index >= shuffled.size()) {
        break;
      }
    }
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

        // Skip if no winner determined
        if (winner == null) {
          continue;
        }

        // Special case: BYE vs BYE should propagate a BYE
        boolean teamABye   = currentGame.getTeamA() != null && currentGame.getTeamA().isBye();
        boolean teamBBye   = currentGame.getTeamB() != null && currentGame.getTeamB().isBye();
        boolean isByeVsBye = teamABye && teamBBye;

        // Skip BYE winners unless it's from a BYE vs BYE match
        if (winner.isBye() && !isByeVsBye) {
          continue;
        }

        // Avoid duplicate placement only if the *same instance* is already assigned in next round.
        // Using reference identity prevents false positives when different pairs share seed/labels.
        if (isAlreadyAssignedInNextByReference(nextGames, winner)) {
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
          placeBySingleScan(nextGames, winner);
        }
      }
    }
  }

  @Override
  public Stage getInitialStage() {
    return switch (phaseType) {
      case QUALIFS -> Stage.Q1;
      case MAIN_DRAW -> Stage.fromNbTeams(drawSize);
      default -> throw new IllegalStateException("Unsupported phaseType: " + phaseType);
    };
  }
}
