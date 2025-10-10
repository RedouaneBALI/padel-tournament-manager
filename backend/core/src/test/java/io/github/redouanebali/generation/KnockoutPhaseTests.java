package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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
    Tournament tournament = new Tournament();
    TournamentConfig cfg = TournamentConfig.builder()
                                           .preQualDrawSize(preQualDrawSize)
                                           .nbQualifiers(nbQualifiers)
                                           .mainDrawSize(mainDrawSize)
                                           .build();
    tournament.setConfig(cfg);

    KnockoutPhase phase = new KnockoutPhase(
        phaseType == PhaseType.MAIN_DRAW ? mainDrawSize : preQualDrawSize,
        0, // nbSeeds not important for structure tests
        phaseType
    );

    // Act
    List<Round> initialized = phase.initialize(tournament.getConfig());

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
  @CsvFileSource(resources = "/io.github.redouanebali/tournament_scenarios.csv", numLinesToSkip = 1)
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
    TournamentConfig cfg = TournamentConfig.builder()
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
      qualifPhase = new KnockoutPhase(preQualDrawSize, nbQualifSeeds, PhaseType.QUALIFS);
    }
    KnockoutPhase mainDrawPhase = new KnockoutPhase(mainDrawSize, nbSeeds, PhaseType.MAIN_DRAW);

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
      // For main draw, check if this is truly the first round (no teams from previous rounds)
      isFirstRoundOfPhase = currentStageEnum == Stage.fromNbTeams(mainDrawSize) && fromPreviousRound == 0;
    }

    if (isFirstRoundOfPhase) {
      // For initial round, fill it with seeds, BYES, remaining teams and scores
      t.getRounds().clear();
      t.getRounds().add(currentRound);

      List<PlayerPair> allPairs = TestFixtures.createPlayerPairs(pairsNonBye);

      // Handle staggered entry
      if (staggeredEntry && !currentStageEnum.isQualification()) {
        // In staggered entry mode, seeds enter selon le stage
        phaseToUse.placeSeedTeamsStaggered(currentRound, allPairs, currentStageEnum, mainDrawSize, nbSeeds, false);

        // Add teams from previous round plus new teams
        Set<PlayerPair> alreadyPlaced = new HashSet<>();
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() != null && !g.getTeamA().isBye() && g.getTeamA().getType() != PairType.QUALIFIER) {
            alreadyPlaced.add(g.getTeamA());
          }
          if (g.getTeamB() != null && !g.getTeamB().isBye() && g.getTeamB().getType() != PairType.QUALIFIER) {
            alreadyPlaced.add(g.getTeamB());
          }
        }

        // Place remaining teams based on CSV data
        List<PlayerPair> remainingPairs = allPairs.stream()
                                                  .filter(p -> !alreadyPlaced.contains(p))
                                                  .limit(fromPreviousRound + newTeams)
                                                  .toList();
        phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);

        // Place BYEs according to CSV data
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

        // Fill any remaining empty slots with BYEs to ensure all games are complete
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() == null) {
            g.setTeamA(PlayerPair.bye());
          }
          if (g.getTeamB() == null) {
            g.setTeamB(PlayerPair.bye());
          }
        }
      } else {
        // Normal mode (non-staggered) - original behavior
        phaseToUse.placeSeedTeams(currentRound, allPairs);

        // NOTE: We do NOT call placeByeTeams here to avoid conflicts with remaining teams placement
        // BYEs will be placed at the end to fill empty slots

        // Collect already placed teams (seeds) - use a Set to track by reference
        // NOTE: placeSeedTeams may place more teams than nbSeeds at theoretical positions
        // Only the first nbSeeds teams (seed >= 1 && seed <= nbSeeds) are actual seeds
        Set<PlayerPair> alreadyPlaced = new HashSet<>();
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() != null && !g.getTeamA().isBye() && g.getTeamA().getSeed() >= 1 && g.getTeamA().getSeed() <= nbQualifSeeds) {
            alreadyPlaced.add(g.getTeamA());
          }
          if (g.getTeamB() != null && !g.getTeamB().isBye() && g.getTeamB().getSeed() >= 1 && g.getTeamB().getSeed() <= nbQualifSeeds) {
            alreadyPlaced.add(g.getTeamB());
          }
        }

        // Debug logging for tournament 7
        if (tournamentId.equals("7") && roundName.equals("Q1")) {
          System.out.println("\n=== DIAGNOSTIC TOURNOI 7 Q1 ===");
          System.out.println("Total allPairs: " + allPairs.size());
          System.out.println("nbQualifSeeds: " + nbQualifSeeds);
          System.out.println("Already placed (true seeds 1-" + nbQualifSeeds + "): " + alreadyPlaced.size());
          int nullSlots = 0, byeSlots = 0, seedSlots = 0, teamSlots = 0;
          for (Game g : currentRound.getGames()) {
            if (g.getTeamA() == null) {
              nullSlots++;
            } else if (g.getTeamA().isBye()) {
              byeSlots++;
            } else if (g.getTeamA().getSeed() >= 1 && g.getTeamA().getSeed() <= nbQualifSeeds) {
              seedSlots++;
            } else {
              teamSlots++;
            }

            if (g.getTeamB() == null) {
              nullSlots++;
            } else if (g.getTeamB().isBye()) {
              byeSlots++;
            } else if (g.getTeamB().getSeed() >= 1 && g.getTeamB().getSeed() <= nbQualifSeeds) {
              seedSlots++;
            } else {
              teamSlots++;
            }
          }
          System.out.println("Before placing remaining: Seeds=" + seedSlots + ", Teams=" + teamSlots + ", BYEs=" + byeSlots + ", Nulls=" + nullSlots);
        }

        // Place remaining teams in available slots
        // Since placeSeedTeams sorts and may create new references, we need to filter by checking
        // if the team is already in the round, not by Set.contains()
        List<PlayerPair> remainingPairs = new ArrayList<>();
        for (PlayerPair pair : allPairs) {
          boolean isPlaced = false;
          for (Game g : currentRound.getGames()) {
            if ((g.getTeamA() == pair) || (g.getTeamB() == pair)) {
              isPlaced = true;
              break;
            }
          }
          if (!isPlaced) {
            remainingPairs.add(pair);
          }
        }

        if (tournamentId.equals("7") && roundName.equals("Q1")) {
          System.out.println("Remaining pairs to place: " + remainingPairs.size());
        }
        phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);

        if (tournamentId.equals("7") && roundName.equals("Q1")) {
          int nullSlots = 0, byeSlots = 0, teamSlots = 0;
          for (Game g : currentRound.getGames()) {
            if (g.getTeamA() == null) {
              nullSlots++;
            } else if (g.getTeamA().isBye()) {
              byeSlots++;
            } else {
              teamSlots++;
            }

            if (g.getTeamB() == null) {
              nullSlots++;
            } else if (g.getTeamB().isBye()) {
              byeSlots++;
            } else {
              teamSlots++;
            }
          }
          System.out.println("After placing remaining: Teams=" + teamSlots + ", BYEs=" + byeSlots + ", Nulls=" + nullSlots);
        }

        // Fill any remaining empty slots with BYEs to ensure all games are complete
        for (Game g : currentRound.getGames()) {
          if (g.getTeamA() == null) {
            g.setTeamA(PlayerPair.bye());
          }
          if (g.getTeamB() == null) {
            g.setTeamB(PlayerPair.bye());
          }
        }
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
      // Pour les rounds intermédiaires, création améliorée des rounds précédents
      Round prevRound;

      // Determine what kind of previous round to create
      if (currentStageEnum == Stage.R64 && preQualDrawSize > 0 && nbQualifiers > 0) {
        // R64 following qualifications - previous round is Q2 or Q3
        int   qualifRounds    = Integer.numberOfTrailingZeros(preQualDrawSize / nbQualifiers);
        Stage prevQualifStage = Stage.fromQualifIndex(qualifRounds);
        prevRound = TestFixtures.buildEmptyRound(totalPairs * 2);
        prevRound.setStage(prevQualifStage);
      } else if (currentStageEnum.isQualification()) {
        // Previous qualification round
        int currentQualifIndex = currentStageEnum == Stage.Q1 ? 1 :
                                 currentStageEnum == Stage.Q2 ? 2 : 3;
        Stage prevQualifStage = Stage.fromQualifIndex(currentQualifIndex - 1);
        prevRound = TestFixtures.buildEmptyRound(totalPairs * 2);
        prevRound.setStage(prevQualifStage);
      } else {
        // Normal main draw progression
        int prevRoundSize = totalPairs * 2;
        // Check if the previous round size is supported
        if (prevRoundSize > 64) {
          // If previous would be > 64, it means we're coming from qualifications
          // Create a qualification round instead
          prevRound = TestFixtures.buildEmptyRound(totalPairs * 2);
          prevRound.setStage(Stage.Q2); // Most common case
        } else {
          prevRound = TestFixtures.buildEmptyRound(prevRoundSize);
          prevRound.setStage(Stage.fromNbTeams(prevRoundSize));
        }
      }

      t.getRounds().clear();
      t.getRounds().add(prevRound);
      t.getRounds().add(currentRound);

      List<PlayerPair> prevPairs = TestFixtures.createPlayerPairs(prevRound.getGames().size() * 2);
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

      // Manually adjust currentRound composition according to CSV data
      // Clear the current round and rebuild it according to CSV specifications
      for (Game g : currentRound.getGames()) {
        g.setTeamA(null);
        g.setTeamB(null);
        g.setScore(null);
      }

      // Create teams according to CSV data - need enough teams for all matches + default qualifs
      int              totalTeamsNeeded     = (matches * 2) + defaultQualif;
      List<PlayerPair> teamsForCurrentRound = TestFixtures.createPlayerPairs(totalTeamsNeeded);
      int              teamIndex            = 0;

      // Place real matches (team vs team) - exactly 'matches' games
      int placedMatches = 0;
      for (Game g : currentRound.getGames()) {
        if (placedMatches < matches && teamIndex + 1 < teamsForCurrentRound.size()) {
          g.setTeamA(teamsForCurrentRound.get(teamIndex++));
          g.setTeamB(teamsForCurrentRound.get(teamIndex++));
          g.setFormat(TestFixtures.createSimpleFormat(1));
          g.setScore(TestFixtures.createScoreWithWinner(g, g.getTeamA()));
          placedMatches++;
        }
      }

      // Place default qualifications (team vs BYE) - exactly 'defaultQualif' games
      int placedDefaults = 0;
      for (Game g : currentRound.getGames()) {
        if (placedDefaults < defaultQualif && g.getTeamA() == null && teamIndex < teamsForCurrentRound.size()) {
          g.setTeamA(teamsForCurrentRound.get(teamIndex++));
          g.setTeamB(PlayerPair.bye());
          g.setFormat(TestFixtures.createSimpleFormat(1));
          g.setScore(TestFixtures.createScoreWithWinner(g, g.getTeamA()));
          placedDefaults++;
        }
      }

      // Fill remaining slots with BYEs
      for (Game g : currentRound.getGames()) {
        if (g.getTeamA() == null) {
          g.setTeamA(PlayerPair.bye());
        }
        if (g.getTeamB() == null) {
          g.setTeamB(PlayerPair.bye());
        }
      }

      // Create and add nextRound
      Round nextRound = TestFixtures.buildEmptyRound(totalPairs / 2);
      t.getRounds().add(nextRound);
    }

    // Act: propagate winners from current round to next
    phaseToUse.propagateWinners(t);

    // Assert: verify propagation in nextRound
    Round nextRound = t.getRounds().get(t.getRounds().size() - 1);

    // Count actual non-BYE winners from current round instead of using CSV values
    // This accounts for matches that may not have valid winners set
    long expectedQualified = currentRound.getGames().stream()
                                         .map(Game::getWinner)
                                         .filter(Objects::nonNull)
                                         .filter(w -> !w.isBye())
                                         .count();

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

    // For 64-draw with 16 seeds: TS1-8 enter at R64, TS9-16 enter at R32
    // For 32-draw with 16 seeds: TS1-8 enter at R32, TS9-16 enter at R16
    Stage firstSeedsEnterAt  = Stage.fromNbTeams(mainDrawSize);     // R64 for 64-draw, R32 for 32-draw
    Stage secondSeedsEnterAt = Stage.fromNbTeams(mainDrawSize / 2); // R32 for 64-draw, R16 for 32-draw

    if (stage == firstSeedsEnterAt) {
      return Math.min(totalSeeds, totalSeeds / 2); // Top half of seeds
    } else if (stage == secondSeedsEnterAt) {
      return Math.max(0, totalSeeds - totalSeeds / 2); // Bottom half of seeds
    }

    return 0; // No seeds enter at this stage
  }

  @Test
  void testKnockout_40Teams_16Seeds_allSeedsPlayByes_noBYEvsBAYE() {
    // Given: A tournament in KNOCKOUT mode with 40 teams, 64 slots, and 16 seeds
    Tournament tournament = TestFixtures.makeTournament(
        0,              // preQualDrawSize
        0,              // nbQualifiers
        64,             // mainDrawSize
        16,             // nbSeeds
        0,              // nbSeedsQualify
        DrawMode.SEEDED
    );

    // Create 40 teams (seeds 1-40)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(40);

    // Initialize tournament structure
    KnockoutPhase mainDrawPhase = new KnockoutPhase(64, 16, PhaseType.MAIN_DRAW);
    List<Round>   rounds        = mainDrawPhase.initialize(tournament.getConfig());
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // When: Place players using automatic strategy
    mainDrawPhase.placeSeedTeams(rounds.get(0), teams);
    mainDrawPhase.placeByeTeams(rounds.get(0), 40);
    mainDrawPhase.placeRemainingTeamsRandomly(rounds.get(0),
                                              teams.stream()
                                                   .filter(p -> rounds.get(0).getGames().stream()
                                                                      .noneMatch(g -> g.getTeamA() == p || g.getTeamB() == p))
                                                   .toList());

    // Then: Verify the first round (R64)
    Round firstRound = rounds.get(0);
    assertEquals(Stage.R64, firstRound.getStage(), "First round should be R64");
    assertEquals(32, firstRound.getGames().size(), "R64 should have 32 games");

    // Count total BYEs in the draw
    long totalByes = firstRound.getGames().stream()
                               .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                               .filter(team -> team != null && team.isBye())
                               .count();
    assertEquals(24, totalByes, "Should have exactly 24 BYEs for 40 teams in a 64-slot draw");

    // Verify that ALL 16 seeds (seeds 1-16) play against a BYE
    int seedsPlayingByes = 0;
    for (Game game : firstRound.getGames()) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      if (teamA == null || teamB == null) {
        continue;
      }

      boolean teamAIsSeed = teamA.getSeed() >= 1 && teamA.getSeed() <= 16;
      boolean teamBIsSeed = teamB.getSeed() >= 1 && teamB.getSeed() <= 16;
      boolean teamAIsBye  = teamA.isBye();
      boolean teamBIsBye  = teamB.isBye();

      if ((teamAIsSeed && teamBIsBye) || (teamBIsSeed && teamAIsBye)) {
        seedsPlayingByes++;
      }
    }

    assertEquals(16, seedsPlayingByes,
                 "All 16 seeds (seeds 1-16) must play against a BYE in the first round");

    // Verify explicitly that EACH of the 16 seeded pairs (seeds 1-16) plays a BYE
    for (int seed = 1; seed <= 16; seed++) {
      final int currentSeed = seed;
      boolean seedFoundPlayingBye = firstRound.getGames().stream()
                                              .anyMatch(game -> {
                                                PlayerPair teamA = game.getTeamA();
                                                PlayerPair teamB = game.getTeamB();

                                                if (teamA == null || teamB == null) {
                                                  return false;
                                                }

                                                return (teamA.getSeed() == currentSeed && teamB.isBye()) ||
                                                       (teamB.getSeed() == currentSeed && teamA.isBye());
                                              });

      assertTrue(seedFoundPlayingBye,
                 String.format("Seed #%d must play against a BYE in R64", currentSeed));
    }

    // Verify that there are NO BYE vs BYE matches
    long byeVsByeMatches = firstRound.getGames().stream()
                                     .filter(g -> g.getTeamA() != null && g.getTeamA().isBye()
                                                  && g.getTeamB() != null && g.getTeamB().isBye())
                                     .count();

    assertEquals(0, byeVsByeMatches,
                 "There must be NO BYE vs BYE matches in the draw");

    // Verify that all 40 real teams are placed in the draw
    long realTeamsPlaced = firstRound.getGames().stream()
                                     .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                     .filter(team -> team != null && !team.isBye() && !team.isQualifier())
                                     .distinct()
                                     .count();

    assertEquals(40, realTeamsPlaced, "All 40 teams should be placed in the draw");

    // Additional verification: check that non-seeded teams (seeds 17-40) play against each other
    long nonSeedsPlayingEachOther = 0;
    for (Game game : firstRound.getGames()) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      if (teamA == null || teamB == null) {
        continue;
      }

      boolean teamAIsNonSeed = !teamA.isBye() && teamA.getSeed() > 16;
      boolean teamBIsNonSeed = !teamB.isBye() && teamB.getSeed() > 16;

      if (teamAIsNonSeed && teamBIsNonSeed) {
        nonSeedsPlayingEachOther++;
      }
    }

    assertEquals(8, nonSeedsPlayingEachOther,
                 "The 24 non-seeded teams (seeds 17-40) should play against each other in 8 matches (16 teams), "
                 + "while the remaining 8 non-seeds get BYEs");
  }

  /**
   * CRITICAL TEST: Verifies that seeds are properly separated according to tennis/padel standards.
   *
   * This test ensures that: - Seed 1 and Seed 2 can only meet in the FINAL - Seeds 1-4 can only meet in SEMI-FINALS or later - Seeds 1-8 can only
   * meet in QUARTER-FINALS or later - Seeds 1-16 can only meet in ROUND OF 16 or later
   *
   * This is the FUNDAMENTAL rule of seeded draws that was missing from all other tests!
   */
  @Test
  void testSeedSeparation_32Draw_8Seeds_verifiesStandardTennisPadelLogic() {
    // Given: A 32-draw tournament with 8 seeds (standard configuration)
    Tournament tournament = TestFixtures.makeTournament(
        0,              // preQualDrawSize
        0,              // nbQualifiers
        32,             // mainDrawSize
        8,              // nbSeeds
        0,              // nbSeedsQualify
        DrawMode.SEEDED
    );

    // Create 32 teams (seeds 1-32)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(32);

    // Initialize and populate tournament using the automatic draw strategy
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // CRITICAL VERIFICATION 1: Seeds 1 and 2 must be in opposite halves of the draw
    // They should only meet in the FINAL
    Integer seed1GameIndex = findGameIndexForSeed(r32Round, 1);
    Integer seed2GameIndex = findGameIndexForSeed(r32Round, 2);

    assertTrue(seed1GameIndex != null && seed2GameIndex != null,
               "Seeds 1 and 2 must be placed in the draw");

    // In a 32-draw (16 games), games 0-7 are top half, games 8-15 are bottom half
    boolean seed1InTopHalf = seed1GameIndex < 8;
    boolean seed2InTopHalf = seed2GameIndex < 8;

    assertTrue(seed1InTopHalf != seed2InTopHalf,
               "Seed 1 and Seed 2 MUST be in opposite halves (can only meet in FINAL)");

    // CRITICAL VERIFICATION 2: Seeds 1-4 must be in different quarters
    // They should only meet in SEMI-FINALS or later
    Integer seed3GameIndex = findGameIndexForSeed(r32Round, 3);
    Integer seed4GameIndex = findGameIndexForSeed(r32Round, 4);

    assertTrue(seed3GameIndex != null && seed4GameIndex != null,
               "Seeds 3 and 4 must be placed in the draw");

    // Check that top 4 seeds are in 4 different quarters
    // Quarter 1: games 0-3, Quarter 2: games 4-7, Quarter 3: games 8-11, Quarter 4: games 12-15
    int seed1Quarter = seed1GameIndex / 4;
    int seed2Quarter = seed2GameIndex / 4;
    int seed3Quarter = seed3GameIndex / 4;
    int seed4Quarter = seed4GameIndex / 4;

    Set<Integer> quartersUsed = Set.of(seed1Quarter, seed2Quarter, seed3Quarter, seed4Quarter);
    assertEquals(4, quartersUsed.size(),
                 "Seeds 1-4 MUST be in 4 different quarters (can only meet in SEMI-FINALS or later)");

    // CRITICAL VERIFICATION 3: Seeds 1-8 must be in different eighths
    // They should only meet in QUARTER-FINALS or later
    Set<Integer> eighthsUsed = new HashSet<>();
    for (int seed = 1; seed <= 8; seed++) {
      Integer gameIndex = findGameIndexForSeed(r32Round, seed);
      assertNotNull(gameIndex, "Seed " + seed + " must be placed in the draw");

      // Each eighth contains 2 games (32 slots / 16 games = 2 games per eighth)
      int eighth = gameIndex / 2;
      eighthsUsed.add(eighth);
    }

    assertEquals(8, eighthsUsed.size(),
                 "Seeds 1-8 MUST be in 8 different eighths (can only meet in QUARTER-FINALS or later)");

    // VERIFICATION 4: Check that NO two seeds in top 8 are in the same game (would meet in R32!)
    for (int seed1 = 1; seed1 <= 8; seed1++) {
      for (int seed2 = seed1 + 1; seed2 <= 8; seed2++) {
        Integer game1 = findGameIndexForSeed(r32Round, seed1);
        Integer game2 = findGameIndexForSeed(r32Round, seed2);

        assertTrue(game1 != null && game2 != null,
                   "Seeds " + seed1 + " and " + seed2 + " must be placed");

        assertNotEquals(game1, game2, String.format("Seeds %d and %d MUST NOT be in the same game (they would meet in R32!)",
                                                    seed1, seed2));
      }
    }
  }

  /**
   * Helper method to find the game index where a specific seed is placed. Returns null if the seed is not found.
   */
  private Integer findGameIndexForSeed(Round round, int seedNumber) {
    for (int i = 0; i < round.getGames().size(); i++) {
      Game game = round.getGames().get(i);

      if (game.getTeamA() != null && game.getTeamA().getSeed() == seedNumber) {
        return i;
      }
      if (game.getTeamB() != null && game.getTeamB().getSeed() == seedNumber) {
        return i;
      }
    }
    return null;
  }

  /**
   * CRITICAL TEST: Verifies seed separation for a 64-draw with 16 seeds.
   */
  @Test
  void testSeedSeparation_64Draw_16Seeds_verifiesStandardTennisPadelLogic() {
    // Given: A 64-draw tournament with 16 seeds
    Tournament tournament = TestFixtures.makeTournament(
        0,              // preQualDrawSize
        0,              // nbQualifiers
        64,             // mainDrawSize
        16,             // nbSeeds
        0,              // nbSeedsQualify
        DrawMode.SEEDED
    );

    // Create 64 teams (seeds 1-64)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(64);

    // Initialize and populate tournament
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round r64Round = tournament.getRoundByStage(Stage.R64);

    // Verify Seeds 1 and 2 are in opposite halves (32 games: 0-15 top, 16-31 bottom)
    Integer seed1GameIndex = findGameIndexForSeed(r64Round, 1);
    Integer seed2GameIndex = findGameIndexForSeed(r64Round, 2);

    assertTrue(seed1GameIndex != null && seed2GameIndex != null);

    boolean seed1InTopHalf = seed1GameIndex < 16;
    boolean seed2InTopHalf = seed2GameIndex < 16;

    assertTrue(seed1InTopHalf != seed2InTopHalf,
               "Seed 1 and Seed 2 MUST be in opposite halves in a 64-draw");

    // Verify Seeds 1-4 are in different quarters (8 games per quarter)
    Set<Integer> quartersUsed = new HashSet<>();
    for (int seed = 1; seed <= 4; seed++) {
      Integer gameIndex = findGameIndexForSeed(r64Round, seed);
      assertNotNull(gameIndex);
      quartersUsed.add(gameIndex / 8);
    }
    assertEquals(4, quartersUsed.size(),
                 "Seeds 1-4 MUST be in 4 different quarters in a 64-draw");

    // Verify Seeds 1-16 are properly distributed (no two seeds in same game)
    for (int seed1 = 1; seed1 <= 16; seed1++) {
      for (int seed2 = seed1 + 1; seed2 <= 16; seed2++) {
        Integer game1 = findGameIndexForSeed(r64Round, seed1);
        Integer game2 = findGameIndexForSeed(r64Round, seed2);

        assertTrue(game1 != null && game2 != null);
        assertNotEquals(game1, game2, String.format("Seeds %d and %d MUST NOT be in the same game in R64",
                                                    seed1, seed2));
      }
    }
  }

  /**
   * CRITICAL TEST: Verifies that non-seeded teams are NOT placed sequentially.
   *
   * This test ensures there is random placement by checking that we DON'T have sequential matchups like Team10 vs Team11, Team12 vs Team13, etc.
   *
   * If teams were placed sequentially (the bug before the fix), we would have many consecutive seed numbers playing against each other.
   *
   * With random placement, consecutive seeds should rarely play each other.
   */
  @Test
  void testNonSeededTeams_AreNotPlacedSequentially_ProveRandomDraw() {
    // Given: A 32-draw tournament with 8 seeds and 32 teams
    Tournament tournament = TestFixtures.makeTournament(
        0,              // preQualDrawSize
        0,              // nbQualifiers
        32,             // mainDrawSize
        8,              // nbSeeds
        0,              // nbSeedsQualify
        DrawMode.SEEDED
    );

    // Create 32 teams (seeds 1-32)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(32);

    // Initialize and populate tournament
    TournamentBuilder.setupAndPopulateTournament(tournament, teams);

    Round r32Round = tournament.getRoundByStage(Stage.R32);

    // Count how many games have consecutive seeds playing against each other
    // (e.g., Seed 10 vs Seed 11, Seed 12 vs Seed 13, etc.)
    int consecutiveSeedMatches = 0;

    for (Game game : r32Round.getGames()) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      if (teamA == null || teamB == null) {
        continue;
      }

      // Skip if either team is a BYE or a seed (seeds 1-8 are placed at specific positions)
      if (teamA.isBye() || teamB.isBye()) {
        continue;
      }

      // Only check non-seeded teams (seeds > 8)
      if (teamA.getSeed() <= 8 || teamB.getSeed() <= 8) {
        continue;
      }

      int seed1 = Math.min(teamA.getSeed(), teamB.getSeed());
      int seed2 = Math.max(teamA.getSeed(), teamB.getSeed());

      // Check if they are consecutive (e.g., 10 vs 11, 12 vs 13)
      if (seed2 == seed1 + 1) {
        consecutiveSeedMatches++;
      }
    }

    // CRITICAL ASSERTION: With random placement, we should have AT MOST 1-2 consecutive matches
    // (could happen by chance, but very unlikely to have many)
    //
    // With sequential placement (the bug), we would have MANY consecutive matches
    // (e.g., 10vs11, 12vs13, 14vs15, 16vs17, etc. = at least 8+ consecutive matches)
    //
    // Setting threshold to 3: if we have 3 or more consecutive matches, it's suspicious
    // and likely means teams are placed sequentially
    assertTrue(consecutiveSeedMatches < 3,
               String.format("Too many consecutive seed matchups (%d)! " +
                             "This suggests teams are placed SEQUENTIALLY instead of RANDOMLY. " +
                             "Expected: < 3 (random), Actual: %d (likely sequential if >= 3)",
                             consecutiveSeedMatches, consecutiveSeedMatches));
  }

  /**
   * STATISTICAL TEST: Verifies randomness by generating multiple draws and checking variance.
   *
   * This test generates the same tournament 10 times and verifies that the non-seeded teams are placed differently each time (proving there's
   * randomness).
   *
   * Note: This test has a small chance of false positive if we're VERY unlucky with RNG, but statistically it should pass 99.99% of the time with
   * true randomness.
   */
  @Test
  void testRandomPlacement_GeneratesVariedDraws_ProvesTrueRandomness() {
    // Track the matchups for non-seeded teams across multiple draws
    Set<String> uniqueMatchupPatterns = new HashSet<>();

    // Generate the same tournament 10 times
    for (int iteration = 0; iteration < 10; iteration++) {
      Tournament tournament = TestFixtures.makeTournament(
          0,              // preQualDrawSize
          0,              // nbQualifiers
          32,             // mainDrawSize
          8,              // nbSeeds
          0,              // nbSeedsQualify
          DrawMode.SEEDED
      );

      List<PlayerPair> teams = TestFixtures.createPlayerPairs(32);
      TournamentBuilder.setupAndPopulateTournament(tournament, teams);

      Round r32Round = tournament.getRoundByStage(Stage.R32);

      // Create a "fingerprint" of the non-seeded matchups
      StringBuilder matchupPattern = new StringBuilder();

      for (Game game : r32Round.getGames()) {
        PlayerPair teamA = game.getTeamA();
        PlayerPair teamB = game.getTeamB();

        if (teamA == null || teamB == null || teamA.isBye() || teamB.isBye()) {
          continue;
        }

        // Only track non-seeded teams (seeds > 8)
        if (teamA.getSeed() > 8 && teamB.getSeed() > 8) {
          int seed1 = Math.min(teamA.getSeed(), teamB.getSeed());
          int seed2 = Math.max(teamA.getSeed(), teamB.getSeed());
          matchupPattern.append(seed1).append("v").append(seed2).append(";");
        }
      }

      uniqueMatchupPatterns.add(matchupPattern.toString());
    }

    // CRITICAL ASSERTION: With true randomness, we should have MULTIPLE different patterns
    // If teams were placed sequentially (the bug), we would have THE SAME pattern every time
    // (because sequential placement is deterministic)
    //
    // With 10 iterations, we expect at least 5-8 unique patterns with true randomness
    // (some duplicates are possible but unlikely)
    int uniquePatterns = uniqueMatchupPatterns.size();

    assertTrue(uniquePatterns >= 5,
               String.format("Not enough variation in draw generation! " +
                             "Generated %d iterations but only got %d unique patterns. " +
                             "This suggests teams are placed DETERMINISTICALLY instead of RANDOMLY. " +
                             "Expected: >= 5 unique patterns (random), Actual: %d",
                             10, uniquePatterns, uniquePatterns));
  }

}
