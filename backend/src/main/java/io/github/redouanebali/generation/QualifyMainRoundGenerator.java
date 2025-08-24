package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QualifyMainRoundGenerator extends AbstractRoundGenerator {

  private final int mainDrawSize;
  private final int nbQualifiers;
  private final int preQualDrawSize; // new
  // @todo add nbSeeds for qualification rounds too

  public QualifyMainRoundGenerator(int nbSeeds, int mainDrawSize, int nbQualifiers, int preQualDrawSize) {
    super(nbSeeds);
    this.mainDrawSize    = mainDrawSize;
    this.nbQualifiers    = nbQualifiers;
    this.preQualDrawSize = preQualDrawSize;
  }

  @Override
  public List<Round> generateManualRounds(final List<PlayerPair> allPairs) {
    List<Round> rounds = new ArrayList<>();

    // 1) Validation
    if (allPairs == null || allPairs.isEmpty()) {
      throw new IllegalArgumentException("allPairs must not be empty");
    }

    // 2) Sort pairs by ascending seed
    List<PlayerPair> sortedPairs = new ArrayList<>(allPairs);
    sortedPairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // 3) Determine if we only have qualifications pairs or full field (main + prequal)
    int     directSlots             = Math.max(0, mainDrawSize - nbQualifiers);
    boolean onlyQualificationsInput = sortedPairs.size() <= directSlots;

    List<PlayerPair> qualificationPairs;
    List<PlayerPair> directEntrants = new ArrayList<>();

    if (onlyQualificationsInput) {
      qualificationPairs = new ArrayList<>(sortedPairs);
    } else {
      int directMainDrawPairs = Math.max(0, Math.min(directSlots, sortedPairs.size()));
      directEntrants     = new ArrayList<>(sortedPairs.subList(0, directMainDrawPairs));
      qualificationPairs = new ArrayList<>(sortedPairs.subList(directMainDrawPairs, sortedPairs.size()));
    }

    // 4) Build ALL qualification rounds (Q1..Qk) from config: preQualDrawSize and nbQualifiers
    int pre = preQualDrawSize;
    if (pre > 0 && nbQualifiers > 0 && DrawMath.isPowerOfTwo(pre) && pre % nbQualifiers == 0 && DrawMath.isPowerOfTwo(pre / nbQualifiers)) {
      int ratio   = pre / nbQualifiers;
      int qRounds = Math.min(3, log2Safe(ratio));

      // Create Q1..Qk rounds - FIXED: Last round should have exactly nbQualifiers games
      for (int i = 1; i <= qRounds; i++) {
        Stage stage = Stage.valueOf("Q" + i);
        int   games;
        if (i == qRounds) {
          // Last qualification round must produce exactly nbQualifiers winners
          games = nbQualifiers;
        } else {
          // Earlier rounds follow the pyramid structure
          games = pre >> i;
        }
        rounds.add(createEmptyRound(stage, games));
      }

      // Pad the provided qualification pairs with BYEs to reach exactly preQualDrawSize for Q1
      while (qualificationPairs.size() < pre) {
        qualificationPairs.add(PlayerPair.bye());
      }

      // Populate Q1 manually (A then B) with current qualificationPairs
      if (!rounds.isEmpty() && rounds.get(0).getStage().isQualification()) {
        Round q1        = rounds.get(0);
        int   teamIndex = 0;
        for (Game g : q1.getGames()) {
          if (teamIndex < qualificationPairs.size()) {
            g.setTeamA(qualificationPairs.get(teamIndex++));
          }
          if (teamIndex < qualificationPairs.size()) {
            g.setTeamB(qualificationPairs.get(teamIndex++));
          }
        }
      }
    }

    // 5) Build main draw only if we were given the full field
    if (!onlyQualificationsInput && mainDrawSize > 0) {
      int main = mainDrawSize;
      if (!DrawMath.isPowerOfTwo(main)) {
        main = DrawMath.nextPowerOfTwo(main);
      }

      Stage stage          = Stage.fromNbTeams(main);
      Round mainFirstRound = createEmptyRound(stage, main / 2);

      // Pre-seed with direct entrants, leave nbQualifiers null slots
      int idx = 0;
      for (Game g : mainFirstRound.getGames()) {
        if (idx < directEntrants.size()) {
          g.setTeamA(directEntrants.get(idx++));
        }
        if (idx < directEntrants.size()) {
          g.setTeamB(directEntrants.get(idx++));
        }
      }
      rounds.add(mainFirstRound);

      // Add remaining main rounds (QUARTERS, SEMIS, FINAL...)
      int   teams = main / 2;
      Stage cur   = stage.next();
      while (cur != null && cur != Stage.WINNER) {
        Round r = createEmptyRound(cur, teams / 2);
        rounds.add(r);
        teams >>= 1;
        cur = cur.next();
      }
    }

    return rounds;
  }

  @Override
  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    List<Round> rounds = tournament.getRounds();

    // Identifier les rounds de préqualification et de phase finale
    List<Round> qualifyingRounds = new ArrayList<>();
    List<Round> finalRounds      = new ArrayList<>();

    for (Round round : rounds) {
      if (round.getStage().isQualification()) {
        qualifyingRounds.add(round);
      } else {
        finalRounds.add(round);
      }
    }

    // Sort qualifying rounds by stage (Q1 < Q2 < Q3)
    qualifyingRounds.sort(Comparator.comparing(r -> r.getStage().name()));

    // Étape 1 : Propager les vainqueurs dans les rounds de préqualification
    for (int roundIndex = 0; roundIndex < qualifyingRounds.size() - 1; roundIndex++) {
      Round currentRound = qualifyingRounds.get(roundIndex);
      Round nextRound    = qualifyingRounds.get(roundIndex + 1);
      propagateWinnersBetweenRounds(currentRound, nextRound);
    }

    // Étape 2 : Injecter les vainqueurs du dernier round de qualification dans la phase finale
    if (!qualifyingRounds.isEmpty() && !finalRounds.isEmpty()) {
      // Check if all qualifying rounds are finished
      if (!qualifyingRounds.stream().allMatch(Round::isFinished)) {
        return;
      }
      Round lastQualifyingRound = qualifyingRounds.get(qualifyingRounds.size() - 1);
      Round firstFinalRound     = finalRounds.get(0);

      propagateWinnersBetweenRounds(lastQualifyingRound, firstFinalRound);
    }

    // Étape 3 : Propager dans les rounds finaux
    for (int roundIndex = 0; roundIndex < finalRounds.size() - 1; roundIndex++) {
      Round currentRound = finalRounds.get(roundIndex);
      Round nextRound    = finalRounds.get(roundIndex + 1);
      propagateWinnersBetweenRounds(currentRound, nextRound);
    }
  }

  /**
   * Propage les gagnants entre deux rounds.
   */
  private void propagateWinnersBetweenRounds(Round currentRound, Round nextRound) {
    if (currentRound == null || nextRound == null || currentRound.getGames() == null || nextRound.getGames() == null) {
      return;
    }

    List<Game> currentGames = currentRound.getGames();
    List<Game> nextGames    = nextRound.getGames();

    // Vérifie si le round actuel est vide
    boolean currentRoundEmpty = currentGames.stream()
                                            .allMatch(game -> game.getTeamA() == null && game.getTeamB() == null);
    if (currentRoundEmpty) {
      return;
    }

    // 1) Collect winners from current round (BYE or finished)
    List<PlayerPair> winners = new ArrayList<>();
    for (Game currentGame : currentGames) {
      PlayerPair winner = null;
      if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
        winner = currentGame.getTeamB();
      } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
        winner = currentGame.getTeamA();
      } else if (currentGame.isFinished()) {
        winner = currentGame.getWinner();
      }
      if (winner != null) {
        winners.add(winner);
      }
    }

    if (winners.isEmpty()) {
      return; // rien à propager
    }

    // 2) If next round is MAIN (non-qualification), fill first available reserved spots (null or QUALIFIER)
    if (!nextRound.getStage().isQualification()) {
      int wi = 0;
      for (Game next : nextGames) {
        // teamA
        if (wi < winners.size()) {
          PlayerPair t = next.getTeamA();
          if (t == null || (t.getType() == PairType.QUALIFIER)) {
            next.setTeamA(winners.get(wi++));
            if (wi >= winners.size()) {
              break;
            }
          }
        }
        // teamB
        if (wi < winners.size()) {
          PlayerPair t = next.getTeamB();
          if (t == null || (t.getType() == PairType.QUALIFIER)) {
            next.setTeamB(winners.get(wi++));
            if (wi >= winners.size()) {
              break;
            }
          }
        }
      }
      return;
    }

    // 3) Else: Q→Q positional propagation (i/2 mapping)
    for (int i = 0; i < currentGames.size(); i++) {
      Game       currentGame = currentGames.get(i);
      PlayerPair winner      = null;

      if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
        winner = currentGame.getTeamB();
      } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
        winner = currentGame.getTeamA();
      } else if (currentGame.isFinished()) {
        winner = currentGame.getWinner();
      }

      int targetGameIndex = i / 2;
      if (targetGameIndex >= nextGames.size()) {
        continue;
      }

      Game nextGame = nextGames.get(targetGameIndex);
      if (winner == null) {
        if (i % 2 == 0) {
          nextGame.setTeamA(null);
        } else {
          nextGame.setTeamB(null);
        }
      } else {
        if (i % 2 == 0) {
          nextGame.setTeamA(winner);
        } else {
          nextGame.setTeamB(winner);
        }
      }
    }
  }

  /**
   * Picks nbQualifiers random empty slots in the first main-draw round to reserve for future qualifiers. Returns a list of reserved slot coordinates
   * [gameIndex, sideIndex] where sideIndex=0 for A and 1 for B. This method does NOT attach transient PlayerPair to avoid persistence issues; it only
   * decides which slots must remain empty.
   */
  private List<int[]> reserveRandomQualifierPlaceholders(final Round mainFirstRound, final int nbQualifiers) {
    List<int[]> reserved = new ArrayList<>();
    if (mainFirstRound == null || mainFirstRound.getGames() == null || nbQualifiers <= 0) {
      return reserved;
    }

    // Build list of all currently empty slots with their coordinates
    class SlotCoord {

      final int gi;
      final int side;

      SlotCoord(int gi, int side) {
        this.gi   = gi;
        this.side = side;
      }
    }
    List<SlotCoord> empties = new ArrayList<>();
    List<Game>      games   = mainFirstRound.getGames();
    for (int gi = 0; gi < games.size(); gi++) {
      Game g = games.get(gi);
      if (g.getTeamA() == null) {
        empties.add(new SlotCoord(gi, 0));
      }
      if (g.getTeamB() == null) {
        empties.add(new SlotCoord(gi, 1));
      }
    }
    if (empties.isEmpty()) {
      return reserved;
    }

    // Balance across halves when possible
    int totalSlots = games.size() * 2;
    int mid        = totalSlots / 2;

    // Map slot to linear index to split halves
    class Indexed {

      final int       lin;
      final SlotCoord sc;

      Indexed(int lin, SlotCoord sc) {
        this.lin = lin;
        this.sc  = sc;
      }
    }
    List<Indexed> indexed = new ArrayList<>();
    int           lin     = 0;
    for (Game ignored : games) {
      // A side
      if (ignored.getTeamA() == null) {
        indexed.add(new Indexed(lin, new SlotCoord(lin / 2, 0)));
      }
      lin++;
      // B side
      if (ignored.getTeamB() == null) {
        indexed.add(new Indexed(lin, new SlotCoord((lin - 1) / 2, 1)));
      }
      lin++;
    }
    if (indexed.isEmpty()) {
      return reserved;
    }

    List<Indexed> firstHalf  = new ArrayList<>();
    List<Indexed> secondHalf = new ArrayList<>();
    for (Indexed ix : indexed) {
      if (ix.lin < mid) {
        firstHalf.add(ix);
      } else {
        secondHalf.add(ix);
      }
    }

    java.util.Collections.shuffle(firstHalf);
    java.util.Collections.shuffle(secondHalf);

    List<Indexed> picked = new ArrayList<>();

    if (nbQualifiers >= 2 && !firstHalf.isEmpty() && !secondHalf.isEmpty()) {
      int qFirst  = (nbQualifiers + 1) / 2;
      int qSecond = nbQualifiers - qFirst;
      for (int i = 0; i < qFirst && i < firstHalf.size(); i++) {
        picked.add(firstHalf.get(i));
      }
      for (int i = 0; i < qSecond && i < secondHalf.size(); i++) {
        picked.add(secondHalf.get(i));
      }
      // complete if one side lacked room
      if (picked.size() < nbQualifiers) {
        List<Indexed> pool = firstHalf.size() > secondHalf.size() ? firstHalf : secondHalf;
        int           i    = 0;
        while (picked.size() < nbQualifiers && i < pool.size()) {
          Indexed cand = pool.get(i++);
          if (!picked.contains(cand)) {
            picked.add(cand);
          }
        }
      }
    } else {
      // Fallback: random across all empties
      java.util.Collections.shuffle(indexed);
      for (int i = 0; i < Math.min(nbQualifiers, indexed.size()); i++) {
        picked.add(indexed.get(i));
      }
    }

    int limit = Math.min(nbQualifiers, picked.size());
    for (int i = 0; i < limit; i++) {
      SlotCoord sc = picked.get(i).sc;
      reserved.add(new int[]{sc.gi, sc.side});
      // Intentionally DO NOT attach a transient PlayerPair qualifier here.
      // Leaving the slot empty avoids TransientObjectException at flush time.
      // The front-end can render (Q) for these reserved coordinates, or a DTO mapper can decorate them.
    }
    return reserved;
  }

  /**
   * Distributes teams into the non-reserved empty slots of the given round.
   */
  private void placeIntoNonReserved(final Round round, final List<PlayerPair> teams, final List<int[]> reserved) {
    if (round == null || round.getGames() == null || teams == null || teams.isEmpty()) {
      return;
    }
    java.util.Collections.shuffle(teams);

    // Build a list of fillable slots
    boolean[][] isReserved = new boolean[round.getGames().size()][2];
    if (reserved != null) {
      for (int[] rc : reserved) {
        if (rc[0] >= 0 && rc[0] < isReserved.length && (rc[1] == 0 || rc[1] == 1)) {
          isReserved[rc[0]][rc[1]] = true;
        }
      }
    }

    List<int[]> fillable = new ArrayList<>();
    for (int gi = 0; gi < round.getGames().size(); gi++) {
      Game g = round.getGames().get(gi);
      if (g.getTeamA() == null && !isReserved[gi][0]) {
        fillable.add(new int[]{gi, 0});
      }
      if (g.getTeamB() == null && !isReserved[gi][1]) {
        fillable.add(new int[]{gi, 1});
      }
    }
    java.util.Collections.shuffle(fillable);

    int ti = 0;
    for (int[] slot : fillable) {
      if (ti >= teams.size()) {
        break;
      }
      Game       g = round.getGames().get(slot[0]);
      PlayerPair p = teams.get(ti++);
      if (slot[1] == 0) {
        g.setTeamA(p);
      } else {
        g.setTeamB(p);
      }
    }
  }

  @Override
  public List<Round> generateAlgorithmicRounds(final List<PlayerPair> allPairs) {
    List<Round> rounds = new ArrayList<>();

    // 1) Validation
    if (allPairs == null || allPairs.isEmpty()) {
      throw new IllegalArgumentException("allPairs must not be empty");
    }

    // 2) Sort pairs by ascending seed (seeded pairs first)
    List<PlayerPair> sortedPairs = new ArrayList<>(allPairs);
    sortedPairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // 3) Split into direct main-draw entrants and qualification pool
    int     directSlots             = Math.max(0, mainDrawSize - nbQualifiers);
    boolean onlyQualificationsInput = sortedPairs.size() <= directSlots;

    List<PlayerPair> directEntrants     = new ArrayList<>();
    List<PlayerPair> qualificationPairs = new ArrayList<>();

    if (onlyQualificationsInput) {
      qualificationPairs = new ArrayList<>(sortedPairs);
    } else {
      int directMainDrawPairs = Math.max(0, Math.min(directSlots, sortedPairs.size()));
      directEntrants     = new ArrayList<>(sortedPairs.subList(0, directMainDrawPairs));
      qualificationPairs = new ArrayList<>(sortedPairs.subList(directMainDrawPairs, sortedPairs.size()));
    }

    // 4) Build ALL qualification rounds (Q1..Qk) driven by preQualDrawSize & nbQualifiers
    int pre = preQualDrawSize;
    if (pre > 0 && nbQualifiers > 0 && DrawMath.isPowerOfTwo(pre) && pre % nbQualifiers == 0 && DrawMath.isPowerOfTwo(pre / nbQualifiers)) {
      int ratio   = pre / nbQualifiers;
      int qRounds = Math.min(3, log2Safe(ratio));

      // Create Q1..Qk rounds with correct number of games - FIXED: Last round has nbQualifiers games
      for (int i = 1; i <= qRounds; i++) {
        Stage stage = Stage.valueOf("Q" + i);
        int   games;
        if (i == qRounds) {
          // Last qualification round must produce exactly nbQualifiers winners
          games = nbQualifiers;
        } else {
          // Earlier rounds follow the pyramid structure
          games = pre >> i;
        }
        rounds.add(createEmptyRound(stage, games));
      }

      // Fill Q1 algorithmically: pad to preQualDrawSize with BYEs, then distribute randomly
      while (qualificationPairs.size() < pre) {
        qualificationPairs.add(PlayerPair.bye());
      }

      if (!rounds.isEmpty() && rounds.get(0).getStage().isQualification()) {
        Round q1 = rounds.get(0);
        // Use the existing random placement helper (no seeds in qualis)
        placeRemainingTeamsRandomly(q1.getGames(), qualificationPairs);
      }
    }

    // 5) Build main draw structure & seed algorithmically only when full field provided
    if (!onlyQualificationsInput && mainDrawSize > 0) {
      int main = mainDrawSize;
      if (!DrawMath.isPowerOfTwo(main)) {
        main = DrawMath.nextPowerOfTwo(main);
      }

      Stage stage          = Stage.fromNbTeams(main);
      Round mainFirstRound = createEmptyRound(stage, main / 2);

      // Algorithmic seeding for direct entrants: place seeds into bracket positions
      List<PlayerPair> remaining = placeSeedAndByeTeams(mainFirstRound.getGames(), new ArrayList<>(directEntrants), getNbSeeds());

      // Reserve nbQualifiers random slots for future qualifiers
      List<int[]> reserved = reserveRandomQualifierPlaceholders(mainFirstRound, nbQualifiers);

      // Then distribute remaining direct entrants randomly into non-reserved empty slots
      placeIntoNonReserved(mainFirstRound, remaining, reserved);

      rounds.add(mainFirstRound);

      // Add remaining main rounds (QUARTERS, SEMIS, FINAL...)
      int   teams = main / 2;
      Stage cur   = stage.next();
      while (cur != null && cur != Stage.WINNER) {
        Round r = createEmptyRound(cur, teams / 2);
        rounds.add(r);
        teams >>= 1;
        cur = cur.next();
      }
    }

    return rounds;
  }
}
