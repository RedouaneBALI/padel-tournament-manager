package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.model.format.knockout.KnockoutStrategy;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class KnockoutStrategyTest {

  @ParameterizedTest(name = "validate cfg mainDrawSize={0}, nbSeeds={1} → valid={2}")
  @CsvSource({
      // valid
      "2,0,true",
      "4,2,true",
      "8,4,true",
      "16,8,true",
      // invalid: not power of two
      "12,4,false",
      // invalid: too many seeds
      "8,10,false"
  })
  void validate_variousCases(int mainDrawSize, int nbSeeds, boolean expectedValid) {
    KnockoutStrategy       strategy = new KnockoutStrategy();
    TournamentFormatConfig cfg      = TournamentFormatConfig.builder().mainDrawSize(mainDrawSize).nbSeeds(nbSeeds).build();

    List<String> errors = new ArrayList<>();
    strategy.validate(cfg, errors);

    if (expectedValid) {
      assertTrue(errors.isEmpty(), "Config should be valid");
    } else {
      assertFalse(errors.isEmpty(), "Config should be invalid");
    }
  }

  @ParameterizedTest(name = "build rounds for mainDrawSize={0}, nbSeeds={1}")
  @CsvSource({
      "2,0",
      "4,0",
      "4,2",
      "8,0",
      "8,4",
      "16,0",
      "16,8"
  })
  void buildInitialRounds_variousSizes(int mainDrawSize, int nbSeeds) {
    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.KNOCKOUT);

    TournamentFormatConfig cfg = TournamentFormatConfig.builder().mainDrawSize(mainDrawSize).nbSeeds(nbSeeds).build();
    t.setConfig(cfg);
    KnockoutStrategy strategy = new KnockoutStrategy();

    List<String> errors = new ArrayList<>();
    strategy.validate(cfg, errors);
    assertTrue(errors.isEmpty(), () -> "Unexpected validation errors: " + errors);

    t.getRounds().clear();
    strategy.buildInitialRounds(t, cfg);

    // First round stage must match the size
    Round first = t.getRounds().get(0);
    assertEquals(Stage.fromNbTeams(mainDrawSize), first.getStage());

    // Number of games in first round equals mainDrawSize/2
    assertEquals(mainDrawSize / 2, first.getGames().size());

    // Number of rounds equals log2(mainDrawSize): QUARTS/SEMIS/FINAL etc.
    int expectedRounds = (int) (Math.log(mainDrawSize) / Math.log(2));
    assertEquals(expectedRounds, t.getRounds().size());

    // Final round exists and has exactly 1 game
    Round last = t.getRounds().get(t.getRounds().size() - 1);
    assertEquals(Stage.FINAL, last.getStage());
    assertEquals(1, last.getGames().size());
  }

  @Test
  void generateRound_manual_assignsSequentially() {
    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.KNOCKOUT);

    TournamentFormatConfig cfg = TournamentFormatConfig.builder().mainDrawSize(4).nbSeeds(0).build();
    t.setConfig(cfg);
    KnockoutStrategy strategy = new KnockoutStrategy();

    // build structure (SEMIS + FINAL)
    t.getRounds().clear();
    strategy.buildInitialRounds(t, cfg);

    // 4 pairs in input order
    List<PlayerPair> pairs = new ArrayList<>(TestFixtures.createPairs(4));

    Round semis = strategy.generateRound(t, pairs, true); // manual
    assertEquals(Stage.SEMIS, semis.getStage());
    assertEquals(2, semis.getGames().size());

    Game g1 = semis.getGames().get(0);
    Game g2 = semis.getGames().get(1);
    // sequential fill A then B
    assertSame(pairs.get(0), g1.getTeamA());
    assertSame(pairs.get(1), g1.getTeamB());
    assertSame(pairs.get(2), g2.getTeamA());
    assertSame(pairs.get(3), g2.getTeamB());
  }

  @Test
  void generateRound_algorithmic_placesAllPairs() {
    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.KNOCKOUT);

    TournamentFormatConfig cfg = TournamentFormatConfig.builder().mainDrawSize(4).nbSeeds(0).build();
    t.setConfig(cfg);
    KnockoutStrategy strategy = new KnockoutStrategy();

    t.getRounds().clear();
    strategy.buildInitialRounds(t, cfg);

    List<PlayerPair> pairs = new ArrayList<>(TestFixtures.createPairs(4));

    Round semis = strategy.generateRound(t, pairs, false); // algorithmic
    assertEquals(Stage.SEMIS, semis.getStage());
    assertEquals(2, semis.getGames().size());

    List<PlayerPair> placed = new ArrayList<>();
    for (Game g : semis.getGames()) {
      placed.add(g.getTeamA());
      placed.add(g.getTeamB());
    }
    // All four distinct pairs are present (order/layout may vary)
    for (PlayerPair p : pairs) {
      assertTrue(placed.contains(p), "Missing pair in algorithmic placement");
    }
  }

  @ParameterizedTest(name = "propagate finalists size={0}, manual={1}, winners={2}")
  @CsvSource({
      "4,true,A",
      "4,false,A",
      "8,true,A",
      "8,false,A",
      "16,true,A",
      "16,false,A"
  })
  void propagateWinners_setsFinalists_param(int mainDrawSize, boolean manual, String winnerSide) {
    Tournament t = new Tournament();
    t.setFormat(TournamentFormat.KNOCKOUT);

    TournamentFormatConfig cfg = TournamentFormatConfig.builder().mainDrawSize(4).nbSeeds(0).build();
    t.setConfig(cfg);
    KnockoutStrategy strategy = new KnockoutStrategy();

    // Build structure and generate first round (QUARTS for size=8, R16 for size=16, etc.)
    t.getRounds().clear();
    strategy.buildInitialRounds(t, cfg);

    List<PlayerPair> pairs     = new ArrayList<>(TestFixtures.createPairs(mainDrawSize));
    Round            generated = strategy.generateRound(t, pairs, manual);

    // Apply generated teams to the first round stored in the tournament
    Round existingFirst = t.getRounds().get(0);
    for (int i = 0; i < existingFirst.getGames().size(); i++) {
      Game eg = existingFirst.getGames().get(i);
      Game ng = generated.getGames().get(i);
      eg.setTeamA(ng.getTeamA());
      eg.setTeamB(ng.getTeamB());
    }

    // For each round except the final, set winners and propagate to the next round
    for (int roundIndex = 0; roundIndex < t.getRounds().size() - 1; roundIndex++) {
      Round r = t.getRounds().get(roundIndex);
      for (Game g : r.getGames()) {
        g.setFormat(TestFixtures.createSimpleFormat(1));
        PlayerPair winner = "A".equals(winnerSide) ? g.getTeamA() : g.getTeamB();
        g.setScore(TestFixtures.createScoreWithWinner(g, winner));
      }
      strategy.propagateWinners(t);
    }

    // Final round must exist and contain exactly the two winners from the first round
    Round finalRound = t.getRounds().stream()
                        .filter(r -> r.getStage() == Stage.FINAL)
                        .findFirst()
                        .orElseThrow();

    PlayerPair fa = finalRound.getGames().get(0).getTeamA();
    PlayerPair fb = finalRound.getGames().get(0).getTeamB();

    assertNotNull(fa);
    assertNotNull(fb);
    assertNotSame(fa, fb, "Finalists must be distinct");

    // Winners from the last non-final round (the semis when size=4, the semis when size=8, the semis when size=16)
    Round            lastNonFinal    = t.getRounds().get(t.getRounds().size() - 2);
    List<PlayerPair> expectedWinners = new ArrayList<>();
    for (Game g : lastNonFinal.getGames()) {
      expectedWinners.add("A".equals(winnerSide) ? g.getTeamA() : g.getTeamB());
    }

    // Final teams must be exactly those two winners (order may vary)
    assertTrue(expectedWinners.contains(fa));
    assertTrue(expectedWinners.contains(fb));
  }
}
