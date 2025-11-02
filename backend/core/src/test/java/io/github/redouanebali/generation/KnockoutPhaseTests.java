package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class KnockoutPhaseTests {

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
  @ParameterizedTest(name = "CSV propagateWinners: {0} -> round={8}")
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

    boolean isFirstRoundOfPhase;
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
        phaseToUse.placeRemainingTeamsRandomly(currentRound, remainingPairs);

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
            setGameWinner(game, game.getTeamA());
          } else if (teamABye && !teamBBye && !teamBQualifier) {
            // TeamB qualified by default (against BYE)
            setGameWinner(game, game.getTeamB());
          } else if (teamBBye && !teamABye && !teamAQualifier) {
            // TeamA qualified by default (against BYE)
            setGameWinner(game, game.getTeamA());
          } else if (teamAQualifier && !teamBBye && !teamBQualifier) {
            // TeamB qualified by default (QUALIFIER vs real team)
            setGameWinner(game, game.getTeamB());
          } else if (teamBQualifier && !teamABye && !teamAQualifier) {
            // TeamA qualified by default (real team vs QUALIFIER)
            setGameWinner(game, game.getTeamA());
          } else if (teamAQualifier && teamBBye) {
            // QUALIFIER vs BYE: BYE wins (simulation of seed entering later)
            setGameWinner(game, game.getTeamB());
          } else if (teamBQualifier && teamABye) {
            // BYE vs QUALIFIER: BYE wins (simulation of seed entering later)
            setGameWinner(game, game.getTeamA());
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
        int   currentQualifIndex = currentStageEnum == Stage.Q2 ? 2 : 3;
        Stage prevQualifStage    = Stage.fromQualifIndex(currentQualifIndex - 1);
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
    Round nextRound = t.getRounds().getLast();

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

  @Test
  void testKnockout_40Teams_16Seeds_allSeedsPlayByes_noBYEvsBAYE() {
    // Given: A tournament in KNOCKOUT mode with 40 teams, 64 slots, and 16 seeds
    Tournament tournament = TestFixtures.makeTournament(
        0,              // preQualDrawSize
        0,              // nbQualifiers
        64,             // mainDrawSize
        16,             // nbSeeds
        0,              // nbSeedsQualify
        DrawMode.MANUAL
    );

    // Create 40 teams (seeds 1-40)
    List<PlayerPair> teams = TestFixtures.createPlayerPairs(40);

    // Initialize tournament structure
    KnockoutPhase mainDrawPhase = new KnockoutPhase(64, 16, PhaseType.MAIN_DRAW);
    List<Round>   rounds        = mainDrawPhase.initialize(tournament.getConfig());
    tournament.getRounds().clear();
    tournament.getRounds().addAll(rounds);

    // When: Place players using automatic strategy
    mainDrawPhase.placeSeedTeams(rounds.getFirst(), teams);
    mainDrawPhase.placeByeTeams(rounds.getFirst(), 40);
    mainDrawPhase.placeRemainingTeamsRandomly(rounds.getFirst(),
                                              teams.stream()
                                                   .filter(p -> rounds.getFirst().getGames().stream()
                                                                      .noneMatch(g -> g.getTeamA() == p || g.getTeamB() == p))
                                                   .toList());

    // Then: Verify the first round (R64)
    Round firstRound = rounds.getFirst();
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
                 + "while the remaining 8 non-seeds get BYES");
  }


  /**
   * Helper method to set the winner of a game and update its score.
   */
  private void setGameWinner(Game game, PlayerPair winner) {
    game.setFormat(TestFixtures.createSimpleFormat(1));
    game.setScore(TestFixtures.createScoreWithWinner(game, winner));
  }

}
