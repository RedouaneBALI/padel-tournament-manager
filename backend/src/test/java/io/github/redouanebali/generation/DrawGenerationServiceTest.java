package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.DrawGenerationService;
import io.github.redouanebali.util.TestFixtures;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DrawGenerationServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @Mock
  private SecurityProps securityProps;

  @InjectMocks
  private DrawGenerationService drawGenerationService;

  @BeforeEach
  void setUp() {
    Jwt jwt = Jwt.withTokenValue("fake")
                 .header("alg", "none")
                 .claim("email", "bali.redouane@gmail.com")
                 .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList(), "bali.redouane@gmail.com");
    SecurityContextHolder.getContext().setAuthentication(auth);

    org.mockito.Mockito.lenient().when(securityProps.getSuperAdmins()).thenReturn(Collections.emptySet());
  }

  @Test
  void testGenerateDrawAlgorithmicKnockout() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setTournamentFormat(TournamentFormat.KNOCKOUT);
    tournament.setNbSeeds(2);
    tournament.setOwnerId("bali.redouane@gmail.com");

    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    Round existingRound = new Round();
    existingRound.setStage(Stage.SEMIS);
    tournament.getRounds().clear();
    tournament.getRounds().add(existingRound);

    // Prepare a mock generator and stub the static factory
    AbstractRoundGenerator generatorMock = Mockito.mock(KnockoutRoundGenerator.class);
    Round                  newRound      = new Round();
    newRound.setStage(Stage.SEMIS);

    try (MockedStatic<AbstractRoundGenerator> mocked = Mockito.mockStatic(AbstractRoundGenerator.class)) {
      mocked.when(() -> AbstractRoundGenerator.of(tournament)).thenReturn(generatorMock);
      when(generatorMock.generateAlgorithmicRound(any())).thenReturn(newRound);

      // Mock the repository save
      when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      Tournament updated = drawGenerationService.generateDraw(tournament, false);

      assertEquals(tournament, updated);
      verify(tournamentRepository).save(tournament);
      verify(generatorMock).generateAlgorithmicRound(any());
      verify(generatorMock).propagateWinners(tournament);
    }
  }

  @Test
  void testGenerateDrawDrawGroupStage() {
    Tournament tournament = new Tournament();
    tournament.setId(2L);
    tournament.setTournamentFormat(TournamentFormat.GROUP_STAGE);
    tournament.setNbSeeds(3);
    tournament.setNbPools(1);
    tournament.setNbPairsPerPool(3);
    tournament.setOwnerId("bali.redouane@gmail.com");

    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(3));

    Round existingRound = new Round();
    existingRound.setStage(Stage.GROUPS);

    tournament.getRounds().clear();
    tournament.getRounds().add(existingRound);

    AbstractRoundGenerator generatorMock = Mockito.mock(GroupRoundGenerator.class);
    Round                  newRound      = new Round();
    newRound.setStage(Stage.GROUPS);

    try (MockedStatic<AbstractRoundGenerator> mocked = Mockito.mockStatic(AbstractRoundGenerator.class)) {
      mocked.when(() -> AbstractRoundGenerator.of(tournament)).thenReturn(generatorMock);
      when(generatorMock.generateManualRound(any())).thenReturn(newRound);

      when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      Tournament updated = drawGenerationService.generateDraw(tournament, true);

      assertEquals(tournament, updated);
      verify(tournamentRepository).save(tournament);
      verify(generatorMock).generateManualRound(any());
      verify(generatorMock, never()).propagateWinners(any());
    }
  }

  @Test
  void capPairsToMax_shouldTruncateToNbMaxPairs() {
    Tournament t = new Tournament();
    t.setId(42L);
    t.setNbMaxPairs(32);

    // Build 36 pairs using shared test factory
    var pairs36 = TestFixtures.createPairs(36);
    t.getPlayerPairs().addAll(pairs36);

    var result = DrawGenerationService.capPairsToMax(t);

    assertEquals(32, result.size(), "Should keep only the first 32 pairs");
    assertEquals(1, result.get(0).getSeed(), "Order must be preserved (first)");
    assertEquals(32, result.get(31).getSeed(), "Order must be preserved (last kept)");
  }

  @Test
  void capPairsToMax_shouldNotTruncateWhenUnderLimitOrNoLimit() {
    Tournament t1 = new Tournament();
    t1.setId(43L);
    t1.setNbMaxPairs(40); // greater than actual size
    t1.getPlayerPairs().addAll(TestFixtures.createPairs(36));

    var result1 = DrawGenerationService.capPairsToMax(t1);
    assertEquals(36, result1.size(), "No truncation when size <= max");

    Tournament t2 = new Tournament();
    t2.setId(44L);
    t2.setNbMaxPairs(0); // no limit defined
    t2.getPlayerPairs().addAll(TestFixtures.createPairs(5));

    var result2 = DrawGenerationService.capPairsToMax(t2);
    assertEquals(5, result2.size(), "No truncation when max is null");
  }


  // Version alternative du test avec mocking complet pour plus de contrôle
  @Test
  public void testTwiceLessTeamThanMaximum_WithMocking() {
    // Given: KO tournament with max 16 pairs, but only 8 registered
    Tournament t1 = new Tournament();
    t1.setId(44L);
    t1.setTournamentFormat(TournamentFormat.KNOCKOUT);
    t1.setOwnerId("bali.redouane@gmail.com");
    t1.setNbMaxPairs(16);
    t1.getPlayerPairs().addAll(TestFixtures.createPairs(8));

    // Create rounds structure
    Round r16Round = new Round(Stage.R16);
    // Add some empty games to R16
    for (int i = 0; i < 8; i++) {
      Game emptyGame = new Game();
      emptyGame.setFormat(r16Round.getMatchFormat());
      r16Round.addGame(emptyGame);
    }

    Round quartersRound = new Round(Stage.QUARTERS);
    // Add some empty games to QUARTERS
    for (int i = 0; i < 4; i++) {
      Game emptyGame = new Game();
      emptyGame.setFormat(quartersRound.getMatchFormat());
      quartersRound.addGame(emptyGame);
    }

    t1.getRounds().clear();
    t1.getRounds().add(r16Round);
    t1.getRounds().add(quartersRound);

    // Repository should just echo the saved tournament
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // Mock the generator to return a round with populated games for QUARTERS stage
    AbstractRoundGenerator generatorMock    = Mockito.mock(KnockoutRoundGenerator.class);
    Round                  newQuartersRound = new Round(Stage.QUARTERS);

    // Create games with teams assigned
    List<PlayerPair> pairs = TestFixtures.createPairs(8);
    for (int i = 0; i < 4; i++) {
      Game gameWithTeams = new Game();
      gameWithTeams.setTeamA(pairs.get(i * 2));
      gameWithTeams.setTeamB(pairs.get(i * 2 + 1));
      newQuartersRound.addGame(gameWithTeams);
    }

    try (MockedStatic<AbstractRoundGenerator> mocked = Mockito.mockStatic(AbstractRoundGenerator.class)) {
      mocked.when(() -> AbstractRoundGenerator.of(t1)).thenReturn(generatorMock);
      when(generatorMock.generateAlgorithmicRound(any())).thenReturn(newQuartersRound);

      // When: generate the draw algorithmically
      Tournament result = drawGenerationService.generateDraw(t1, false);

      // Then: service mutates and returns the same tournament
      assertEquals(t1, result);

      // Verify R16 games remain empty
      Round r16 = t1.getRounds().stream().filter(r -> r.getStage() == Stage.R16).findFirst().orElse(null);
      if (r16 != null) {
        for (Game g : r16.getGames()) {
          Assertions.assertNull(g.getTeamA(), "R16 should keep TeamA empty");
          Assertions.assertNull(g.getTeamB(), "R16 should keep TeamB empty");
        }
      }

      // Verify QUARTERS games have teams assigned
      Round quarters = t1.getRounds().stream()
                         .filter(r -> r.getStage() == Stage.QUARTERS)
                         .findFirst()
                         .orElseThrow(() -> new AssertionError("QUARTERS round should exist"));

      assertTrue(quarters.getGames().size() > 0, "QUARTERS should have games");
      assertEquals(4, quarters.getGames().size(), "Should have exactly 4 quarter-final matches");

      for (Game g : quarters.getGames()) {
        assertNotNull(g.getTeamA(), "QUARTERS TeamA should be assigned");
        assertNotNull(g.getTeamB(), "QUARTERS TeamB should be assigned");
      }

      // Verify mocks were called correctly
      verify(generatorMock).generateAlgorithmicRound(any());
      verify(generatorMock).propagateWinners(t1);
      verify(tournamentRepository).save(t1);
    }
  }

  private void testStageSelection(int nbTeams, Stage expectedStage, int expectedMatches) {
    Tournament tournament = new Tournament();
    tournament.setId((long) nbTeams);
    tournament.setTournamentFormat(TournamentFormat.KNOCKOUT);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setNbMaxPairs(32); // Max élevé pour ne pas limiter
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(nbTeams));

    // Init rounds avec des jeux vides
    KnockoutRoundGenerator realGen = new KnockoutRoundGenerator(0);
    List<Round>            initial = realGen.initRoundsAndGames(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(initial);

    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // Mock the generator to return appropriate round
    AbstractRoundGenerator generatorMock = Mockito.mock(KnockoutRoundGenerator.class);
    Round                  mockRound     = new Round(expectedStage);

    // Create games with teams for the expected stage
    List<PlayerPair> pairs = TestFixtures.createPairs(nbTeams);
    for (int i = 0; i < expectedMatches; i++) {
      Game gameWithTeams = new Game();
      gameWithTeams.setTeamA(pairs.get(i * 2));
      gameWithTeams.setTeamB(pairs.get(i * 2 + 1));
      mockRound.addGame(gameWithTeams);
    }

    try (MockedStatic<AbstractRoundGenerator> mocked = Mockito.mockStatic(AbstractRoundGenerator.class)) {
      mocked.when(() -> AbstractRoundGenerator.of(tournament)).thenReturn(generatorMock);
      when(generatorMock.generateAlgorithmicRound(any())).thenReturn(mockRound);

      // Generate draw
      Tournament result = drawGenerationService.generateDraw(tournament, false);

      // Find the target round that should have been populated
      Round targetRound = result.getRounds().stream()
                                .filter(r -> r.getStage() == expectedStage)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Expected stage " + expectedStage + " should exist"));

      // Verify teams are assigned in the correct stage
      assertEquals(expectedMatches, targetRound.getGames().size(),
                   "Should have " + expectedMatches + " matches in " + expectedStage);

      for (Game game : targetRound.getGames()) {
        assertNotNull(game.getTeamA(), expectedStage + " TeamA should be assigned");
        assertNotNull(game.getTeamB(), expectedStage + " TeamB should be assigned");
      }

      verify(generatorMock).generateAlgorithmicRound(any());
      verify(generatorMock).propagateWinners(tournament);
    }
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
