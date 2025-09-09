package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

public class KnockoutPhaseTests {

  @ParameterizedTest(name = "Tournament with nbPairs={0}, preQualDrawSize={1}, nbQualifiers={2}, mainDrawSize={3}, phaseType={4}")
  @CsvSource({
      // nbPairs, preQualDrawSize, nbQualifiers, mainDrawSize, phaseType, expectedRounds, expectedMatchesPerRound
      // 1. Direct main draw (no qualifiers)
      "32, 0, 0, 32, MAIN_DRAW, R32;R16;QUARTERS;SEMIS;FINAL, 16;8;4;2;1",
      // 2. Direct main draw with 64
      "64, 0, 0, 64, MAIN_DRAW, R64;R32;R16;QUARTERS;SEMIS;FINAL, 32;16;8;4;2;1",
      // 3. Qualification 16->4 feeding main draw 32
      "36, 16, 4, 32, QUALIFS, Q1;Q2, 8;4",
      // 4. Qualification 32->8 feeding main draw 32
      "48, 32, 8, 32, QUALIFS, Q1;Q2, 16;8",
      // 5. Qualification 32->8 feeding main draw 64
      "52, 32, 8, 64, QUALIFS, Q1;Q2, 16;8",
  })
  void testInitializeTournament(int nbPairs,
                                int preQualDrawSize,
                                int nbQualifiers,
                                int mainDrawSize,
                                PhaseType phaseType,
                                String expectedStagesStr,
                                String expectedMatchesStr) {
    // Arrange
    Tournament t = new Tournament();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .build();
    t.setConfig(cfg);

    KnockoutPhase phase = new KnockoutPhase(
        phaseType == PhaseType.MAIN_DRAW ? mainDrawSize : preQualDrawSize,
        0, // nbSeeds not important for structure tests
        phaseType,
        DrawMode.SEEDED
    );

    // Act
    List<Round> initialized = phase.initialize(t);

    // Assert
    List<String> expectedStages = Arrays.asList(expectedStagesStr.split(";"));
    List<String> actualStages = initialized.stream()
                                           .map(r -> r.getStage().name())
                                           .toList();

    assertEquals(expectedStages, actualStages,
                 "Rounds mismatch for config nbPairs=" + nbPairs);

    List<Integer> expectedMatches = Arrays.stream(expectedMatchesStr.split(";"))
                                          .map(String::trim)
                                          .map(Integer::parseInt)
                                          .toList();

    List<Integer> actualMatches = initialized.stream()
                                             .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                             .toList();

    assertEquals(expectedMatches, actualMatches,
                 "Matches per round mismatch for config nbPairs=" + nbPairs + ", stages=" + expectedStagesStr);
  }

  /**
   * CSV-driven test for propagateWinners. For each row in src/test/resources/tournament_scenarios.csv, this test: - builds a minimal tournament with
   * the current round (Total pairs slots) and the next round (half as many games) - populates the current round according to the CSV semantics:
   * DefaultQualif -> create Team vs BYE (auto-qualify) Matches      -> create TeamA vs TeamB and set an explicit winner (TeamA) via a score BYE ->
   * assign remaining BYE entries as BYE vs BYE so they do not produce a winner - calls KnockoutPhase.propagateWinners(t) - asserts that the number of
   * non-null teams in the next round equals (Matches + DefaultQualif) FINAL rows are ignored (no subsequent round). Stage is resolved via
   * Stage.valueOf(roundName.toUpperCase()).
   */
  @ParameterizedTest(name = "CSV propagateWinners: {0} – round={8}")
  @CsvFileSource(resources = "/tournament_scenarios.csv", numLinesToSkip = 1)
  void testPropagateWinners_FromCsvRow(String tournamentId,
                                       int nbPlayerPairs,
                                       int preQualDrawSize,
                                       int nbQualifSeeds,
                                       int nbQualifiers,
                                       int mainDrawSize,
                                       int nbSeeds,
                                       boolean staggeredEntry,
                                       String roundName,
                                       String qualifFrom,
                                       int fromPreviousRound,
                                       int newTeams,
                                       int bye,
                                       int defaultQualif,
                                       int totalPairs,
                                       int pairsNonBye,
                                       int pairsPlaying,
                                       int matches) {
    Stage currentStageEnum = Stage.valueOf(roundName.toUpperCase());
    if (currentStageEnum == Stage.FINAL) {
      return;
    }
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .build();
    Tournament t = new Tournament();
    t.setId(Long.valueOf(tournamentId));
    t.setConfig(cfg);

    KnockoutPhase qualifPhase = null;
    if (preQualDrawSize > 0 && nbQualifiers > 0) {
      qualifPhase = new KnockoutPhase(preQualDrawSize, nbQualifSeeds, PhaseType.QUALIFS, DrawMode.SEEDED);
    }
    KnockoutPhase mainDrawPhase = new KnockoutPhase(mainDrawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);

    // Prepare current round
    Round currentRound = TestFixtures.buildEmptyRound(totalPairs);
    currentRound.setStage(currentStageEnum);

    if (nbSeeds > totalPairs) {
      return;
    }

    KnockoutPhase phaseToUse = currentStageEnum.isQualification() ? qualifPhase : mainDrawPhase;
    if (phaseToUse == null) {
      return;
    }

    boolean isFirstRoundOfPhase = false;
    if (currentStageEnum.isQualification()) {
      isFirstRoundOfPhase = currentStageEnum == Stage.Q1;
    } else {
      isFirstRoundOfPhase = currentStageEnum == Stage.fromNbTeams(mainDrawSize);
    }

    if (isFirstRoundOfPhase) {
      // For initial round, fill it with seeds, BYES, remaining teams and scores
      t.getRounds().clear();
      t.getRounds().add(currentRound);

      List<PlayerPair> allPairs = TestFixtures.createPairs(pairsNonBye);

      // Handle staggered entry
      if (staggeredEntry && !currentStageEnum.isQualification()) {
        // In staggered entry mode, seeds enter progressively
        boolean isFirstMainDrawRound = currentStageEnum == Stage.fromNbTeams(mainDrawSize);

        if (isFirstMainDrawRound) {
          // First main draw round: no seeds enter
          // Use staggered method with isFirstRound=true
          if (phaseToUse instanceof KnockoutPhase ko) {
            ko.placeSeedTeamsStaggered(currentRound, allPairs, currentStageEnum, mainDrawSize, nbSeeds, true);
          }

          // Place non-seeded teams according to CSV data
          List<PlayerPair> nonSeededPairs = allPairs.stream()
                                                    .filter(p -> p.getSeed() <= 0 || p.getSeed() > nbSeeds)
                                                    .toList();

          // Use exact CSV values to fill the round
          int teamsToPlace = Math.min(newTeams + fromPreviousRound, nonSeededPairs.size());
          List<PlayerPair> teamsForThisRound = teamsToPlace > 0 ?
                                               nonSeededPairs.subList(0, teamsToPlace) : new ArrayList<>();

          phaseToUse.placeRemainingTeamsRandomly(currentRound, teamsForThisRound);

          // Place BYEs according to CSV data
          // In staggered mode, BYEs replace seed positions + other empty positions
          int actualByesToPlace = bye;
          for (Game g : currentRound.getGames()) {
            if (actualByesToPlace <= 0) {
              break;
            }

            if (g.getTeamA() == null) {
              g.setTeamA(PlayerPair.bye());
              actualByesToPlace--;
            } else if (g.getTeamB() == null) {
              g.setTeamB(PlayerPair.bye());
              actualByesToPlace--;
            }
          }
        } else {
          // Following rounds - seeds enter according to stage
          if (phaseToUse instanceof KnockoutPhase ko) {
            ko.placeSeedTeamsStaggered(currentRound, allPairs, currentStageEnum, mainDrawSize, nbSeeds, false);
          }

          // Add teams from previous round (simulated)
          Set<PlayerPair> alreadyPlaced = new HashSet<>();
          for (Game g : currentRound.getGames()) {
            if (g.getTeamA() != null && !g.getTeamA().isBye() && g.getTeamA().getType() != PairType.QUALIFIER) {
              alreadyPlaced.add(g.getTeamA());
            }
            if (g.getTeamB() != null && !g.getTeamB().isBye() && g.getTeamB().getType() != PairType.QUALIFIER) {
              alreadyPlaced.add(g.getTeamB());
            }
          }

          List<PlayerPair> remainingPairs = allPairs.stream()
                                                    .filter(p -> !alreadyPlaced.contains(p))
                                                    .limit(fromPreviousRound + newTeams)
                                                    .toList();
          phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);
        }
      } else {
        // Normal mode (non-staggered) - original behavior
        phaseToUse.placeSeedTeams(currentRound, allPairs);
        phaseToUse.placeByeTeams(currentRound, pairsNonBye);
        Set<PlayerPair> alreadyPlaced = new HashSet<>();
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() != null && !g.getTeamA().isBye()) {
            alreadyPlaced.add(g.getTeamA());
          }
          if (g.getTeamB() != null && !g.getTeamB().isBye()) {
            alreadyPlaced.add(g.getTeamB());
          }
        }
        List<PlayerPair> remainingPairs = allPairs.stream()
                                                  .filter(p -> !alreadyPlaced.contains(p))
                                                  .toList();
        phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);
      }

      // Simulate scores to generate winners (improved for staggered entry)
      for (Game game : currentRound.getGames()) {
        if (game.getTeamA() != null && game.getTeamB() != null) {
          boolean teamABye       = game.getTeamA().isBye();
          boolean teamBBye       = game.getTeamB().isBye();
          boolean teamAQualifier = game.getTeamA().getType() == PairType.QUALIFIER;
          boolean teamBQualifier = game.getTeamB().getType() == PairType.QUALIFIER;

          if (!teamABye && !teamBBye && !teamAQualifier && !teamBQualifier) {
            // Real match between two teams
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
          } else if (teamABye && !teamBBye && !teamBQualifier) {
            // TeamB qualified by default (against BYE)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamB()));
          } else if (teamBBye && !teamABye && !teamAQualifier) {
            // TeamA qualified by default (against BYE)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
          } else if (teamAQualifier && !teamBBye && !teamBQualifier) {
            // TeamB qualified by default (QUALIFIER vs real team)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamB()));
          } else if (teamBQualifier && !teamABye && !teamAQualifier) {
            // TeamA qualified by default (real team vs QUALIFIER)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
          } else if (teamAQualifier && teamBBye) {
            // QUALIFIER vs BYE: BYE wins (simulation of seed entering later)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamB()));
          } else if (teamBQualifier && teamABye) {
            // BYE vs QUALIFIER: BYE wins (simulation of seed entering later)
            game.setFormat(TestFixtures.createSimpleFormat(1));
            game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
          }
          // BYE vs BYE or QUALIFIER vs QUALIFIER does not generate a winner
        }
      }

      // Create and add nextRound only now
      Round nextRound = TestFixtures.buildEmptyRound(totalPairs / 2);
      t.getRounds().add(nextRound);

    } else {
      // For intermediate round, create prevRound with teams and scores
      int   prevRoundSize = totalPairs * 2;
      Round prevRound     = TestFixtures.buildEmptyRound(prevRoundSize);
      prevRound.setStage(Stage.fromNbTeams(prevRoundSize));
      t.getRounds().clear();
      t.getRounds().add(prevRound);
      t.getRounds().add(currentRound);

      // Fill prevRound with teams and scores
      List<PlayerPair> prevPairs = TestFixtures.createPairs(prevRoundSize);
      int              idx       = 0;
      for (Game g : prevRound.getGames()) {
        if (idx + 1 < prevPairs.size()) {
          g.setTeamA(prevPairs.get(idx));
          g.setTeamB(prevPairs.get(idx + 1));
          g.setFormat(TestFixtures.createSimpleFormat(1));
          g.setScore(TestFixtures.createScoreWithWinner(g, g.getTeamA()));
          idx += 2;
        }
      }

      // Propagate from prevRound to currentRound
      phaseToUse.propagateWinners(t);

      // Now simulate scores in currentRound to test propagation to nextRound
      for (Game game : currentRound.getGames()) {
        if (game.getTeamA() != null && game.getTeamB() != null
            && !game.getTeamA().isBye() && !game.getTeamB().isBye()) {
          game.setFormat(TestFixtures.createSimpleFormat(1));
          game.setScore(TestFixtures.createScoreWithWinner(game, game.getTeamA()));
        }
      }

      // Create and add nextRound only now
      Round nextRound = TestFixtures.buildEmptyRound(totalPairs / 2);
      t.getRounds().add(nextRound);
    }

    // Act: propagate winners from current round to next
    phaseToUse.propagateWinners(t);

    // Assert: verify propagation in nextRound
    Round nextRound         = t.getRounds().get(t.getRounds().size() - 1);
    int   expectedQualified = matches + defaultQualif;
    long actualNonNullNonByeTeams = nextRound.getGames().stream()
                                             .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                             .filter(Objects::nonNull)
                                             .filter(p -> !p.isBye())
                                             .count();

    assertEquals(expectedQualified, actualNonNullNonByeTeams,
                 "Propagation mismatch for " + tournamentId + " at round " + roundName);
  }

  // Helper methods for staggered entry (moved from original class)
  private int getSeedsEnteringAtStage(Stage stage, int mainDrawSize, int totalSeeds) {
    if (totalSeeds <= 0) {
      return 0;
    }

    Stage topSeedsEnterAt  = Stage.fromNbTeams(mainDrawSize / 2); // R16 for 32-draw, R32 for 64-draw
    Stage nextSeedsEnterAt = Stage.fromNbTeams(mainDrawSize / 4); // R8 for 32-draw, R16 for 64-draw

    if (stage == topSeedsEnterAt) {
      return Math.min(totalSeeds, totalSeeds / 2); // Top half of seeds
    } else if (stage == nextSeedsEnterAt) {
      return Math.max(0, totalSeeds - totalSeeds / 2); // Bottom half of seeds
    }

    return 0; // No seeds enter at this stage
  }

  private int getSeedsEnteredBeforeStage(Stage stage, int mainDrawSize, int totalSeeds) {
    if (totalSeeds <= 0) {
      return 0;
    }

    Stage topSeedsEnterAt  = Stage.fromNbTeams(mainDrawSize / 2);
    Stage nextSeedsEnterAt = Stage.fromNbTeams(mainDrawSize / 4);

    if (stage == nextSeedsEnterAt) {
      return Math.min(totalSeeds, totalSeeds / 2); // Top seeds already entered
    }

    return 0; // No seeds entered yet
  }
}
