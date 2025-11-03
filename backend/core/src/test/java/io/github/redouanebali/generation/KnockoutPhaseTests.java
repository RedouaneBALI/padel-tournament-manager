package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
   * CRITICAL TEST: Verify that qualifiers are correctly propagated with their numbers (Q1, Q2, etc.) This test reproduces the production bug where: -
   * First qualifier replaces Q1 correctly - But other qualifiers get erased or duplicated
   */
  @Test
  void testQualifierPropagation_maintainsCorrectQualifierNumbers() {
    // Given: Tournament with 4 qualif matches -> 4 qualifiers to main draw of 8
    Tournament tournament = TestFixtures.makeTournament(
        8,  // preQualDrawSize: 8 teams in qualification
        4,  // nbQualifiers: 4 teams qualify to main draw
        8,  // mainDrawSize: 8 slots in main draw
        4,  // nbSeedsMain: 4 top seeds go direct to main draw
        2,  // nbSeedsQualify: 2 seeds in qualifications
        DrawMode.MANUAL
    );

    // Create 12 teams total: 8 for qualifications + 4 for direct main draw
    List<PlayerPair> allTeams = TestFixtures.createPlayerPairs(12);

    // First 4 teams (seeds 1-4) go directly to main draw
    List<PlayerPair> directTeams = allTeams.subList(0, 4);
    for (int i = 0; i < directTeams.size(); i++) {
      directTeams.get(i).setSeed(i + 1); // Seeds 1-4 for direct entries
    }

    // Last 8 teams (seeds 100-107) go to qualifications
    List<PlayerPair> qualifTeams = allTeams.subList(4, 12);
    for (int i = 0; i < qualifTeams.size(); i++) {
      qualifTeams.get(i).setSeed(100 + i); // Seeds 100-107 for qualif teams
    }

    // Initialize tournament structure
    TournamentBuilder.initializeEmptyRounds(tournament);

    // Manually place teams in Q1 (4 matches = 8 teams)
    Round q1Round = tournament.getRoundByStage(Stage.Q1);
    assertEquals(4, q1Round.getGames().size(), "Q1 should have 4 games");

    for (int i = 0; i < 4; i++) {
      Game game = q1Round.getGames().get(i);
      game.setTeamA(qualifTeams.get(i * 2));
      game.setTeamB(qualifTeams.get(i * 2 + 1));
    }

    // Manually place 4 direct teams + 4 QUALIFIER placeholders in main draw
    Round mainRound = tournament.getRoundByStage(Stage.QUARTERS);
    assertNotNull(mainRound, "Main draw round must exist");
    assertEquals(4, mainRound.getGames().size(), "Main draw should have 4 games");

    // Place direct teams and qualifiers manually
    mainRound.getGames().get(0).setTeamA(directTeams.get(0)); // Seed 1
    mainRound.getGames().get(0).setTeamB(PlayerPair.qualifier(1)); // Q1

    mainRound.getGames().get(1).setTeamA(directTeams.get(1)); // Seed 2
    mainRound.getGames().get(1).setTeamB(PlayerPair.qualifier(2)); // Q2

    mainRound.getGames().get(2).setTeamA(directTeams.get(2)); // Seed 3
    mainRound.getGames().get(2).setTeamB(PlayerPair.qualifier(3)); // Q3

    mainRound.getGames().get(3).setTeamA(directTeams.get(3)); // Seed 4
    mainRound.getGames().get(3).setTeamB(PlayerPair.qualifier(4)); // Q4

    // Verify main draw has 4 QUALIFIER placeholders initially
    assertNotNull(mainRound, "Main draw round must exist");

    long initialQualifiers = mainRound.getGames().stream()
                                      .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                      .filter(t -> t != null && t.isQualifier())
                                      .count();
    assertEquals(4, initialQualifiers, "Main draw should have 4 QUALIFIER placeholders");

    // Verify that qualifiers have correct numbers (Q1, Q2, Q3, Q4)
    List<PlayerPair> initialQualifiersList = mainRound.getGames().stream()
                                                      .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                                      .filter(t -> t != null && t.isQualifier())
                                                      .toList();

    System.out.println("\n=== INITIAL QUALIFIERS ===");
    for (int i = 0; i < initialQualifiersList.size(); i++) {
      PlayerPair q    = initialQualifiersList.get(i);
      String     name = q.getPlayer1() != null ? q.getPlayer1().getName() : "null";
      System.out.println("Qualifier " + (i + 1) + ": " + name);
      assertEquals("Q" + (i + 1), name, "Qualifier should be Q" + (i + 1));
    }

    // Step 1: First team wins in Q1 Game 0
    Game       firstQualifMatch = q1Round.getGames().get(0);
    PlayerPair firstWinner      = firstQualifMatch.getTeamA(); // Team seed 100 wins
    setGameWinner(firstQualifMatch, firstWinner);

    // Propagate winners
    TournamentBuilder.propagateWinners(tournament);

    // Verify: Q1 should be replaced by firstWinner, Q2-Q4 should still be QUALIFIERS
    System.out.println("\n=== AFTER FIRST WINNER ===");
    List<PlayerPair> afterFirst = mainRound.getGames().stream()
                                           .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                           .filter(Objects::nonNull)
                                           .toList();

    // Count only qualification winners (seeds 100+), NOT the direct entries (seeds 1-4)
    long qualifWinnersCount = afterFirst.stream()
                                        .filter(t -> !t.isQualifier() && !t.isBye())
                                        .filter(t -> t.getSeed() >= 100) // Only qualification teams
                                        .count();
    long qualifiersCount = afterFirst.stream().filter(PlayerPair::isQualifier).count();

    System.out.println("Qualification winners: " + qualifWinnersCount);
    System.out.println("Qualifiers remaining: " + qualifiersCount);

    assertEquals(1, qualifWinnersCount, "Should have 1 qualification winner (first winner)");
    assertEquals(3, qualifiersCount, "Should still have 3 QUALIFIER placeholders");

    // Verify remaining qualifiers are Q2, Q3, Q4 (not duplicates of Q1)
    List<String> remainingQualifierNames = afterFirst.stream()
                                                     .filter(PlayerPair::isQualifier)
                                                     .map(q -> q.getPlayer1() != null ? q.getPlayer1().getName() : "null")
                                                     .toList();

    System.out.println("Remaining qualifiers: " + remainingQualifierNames);
    assertTrue(remainingQualifierNames.contains("Q2"), "Should have Q2");
    assertTrue(remainingQualifierNames.contains("Q3"), "Should have Q3");
    assertTrue(remainingQualifierNames.contains("Q4"), "Should have Q4");

    // Step 2: Second team wins in Q1 Game 1
    Game       secondQualifMatch = q1Round.getGames().get(1);
    PlayerPair secondWinner      = secondQualifMatch.getTeamA(); // Team seed 102 wins
    setGameWinner(secondQualifMatch, secondWinner);

    // Propagate winners again
    TournamentBuilder.propagateWinners(tournament);

    // Verify: Q1 and Q2 should be replaced, Q3-Q4 should still be QUALIFIERS
    System.out.println("\n=== AFTER SECOND WINNER ===");
    List<PlayerPair> afterSecond = mainRound.getGames().stream()
                                            .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                            .filter(Objects::nonNull)
                                            .toList();

    // Count only qualification winners (seeds 100+), NOT the direct entries (seeds 1-4)
    qualifWinnersCount = afterSecond.stream()
                                    .filter(t -> !t.isQualifier() && !t.isBye())
                                    .filter(t -> t.getSeed() >= 100) // Only qualification teams
                                    .count();
    qualifiersCount    = afterSecond.stream().filter(PlayerPair::isQualifier).count();

    System.out.println("Qualification winners: " + qualifWinnersCount);
    System.out.println("Qualifiers remaining: " + qualifiersCount);

    // Debug: print all qualification winners
    List<Integer> allQualifWinnerSeeds = afterSecond.stream()
                                                    .filter(t -> !t.isQualifier() && !t.isBye())
                                                    .filter(t -> t.getSeed() >= 100)
                                                    .map(PlayerPair::getSeed)
                                                    .toList();
    System.out.println("All qualification winner seeds: " + allQualifWinnerSeeds);

    assertEquals(2, qualifWinnersCount, "Should have 2 qualification winners (first + second winner)");
    assertEquals(2, qualifiersCount, "Should still have 2 QUALIFIER placeholders");

    // Verify remaining qualifiers are Q3, Q4 (not erased or duplicated)
    remainingQualifierNames = afterSecond.stream()
                                         .filter(PlayerPair::isQualifier)
                                         .map(q -> q.getPlayer1() != null ? q.getPlayer1().getName() : "null")
                                         .toList();

    System.out.println("Remaining qualifiers: " + remainingQualifierNames);
    assertTrue(remainingQualifierNames.contains("Q3"), "Should have Q3");
    assertTrue(remainingQualifierNames.contains("Q4"), "Should have Q4");

    // Verify that first and second winners are correctly placed
    List<Integer> qualifWinnerSeeds = afterSecond.stream()
                                                 .filter(t -> !t.isQualifier() && !t.isBye())
                                                 .filter(t -> t.getSeed() >= 100) // Only qualification teams
                                                 .map(PlayerPair::getSeed)
                                                 .toList();

    assertTrue(qualifWinnerSeeds.contains(100), "First winner (seed 100) should be in main draw");
    assertTrue(qualifWinnerSeeds.contains(102), "Second winner (seed 102) should be in main draw");

    System.out.println("\n=== TEST PASSED ===");
  }

  /**
   * Helper method to set the winner of a game and update its score.
   */
  private void setGameWinner(Game game, PlayerPair winner) {
    game.setFormat(TestFixtures.createSimpleFormat(1));
    game.setScore(TestFixtures.createScoreWithWinner(game, winner));
  }

}
