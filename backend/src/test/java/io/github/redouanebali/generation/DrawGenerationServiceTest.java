package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.DrawGenerationService;
import io.github.redouanebali.util.TestFixtures;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
