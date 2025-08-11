package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.DrawGenerationService;
import java.util.Collections;
import java.util.List;
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

    PlayerPair pair1 = new PlayerPair(1L, new Player("A1"), new Player("B1"), 1);
    PlayerPair pair2 = new PlayerPair(2L, new Player("A2"), new Player("B2"), 2);
    PlayerPair pair3 = new PlayerPair(3L, new Player("A3"), new Player("B3"), 3);
    PlayerPair pair4 = new PlayerPair(4L, new Player("A4"), new Player("B4"), 4);
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(List.of(pair1, pair2, pair3, pair4));

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

    PlayerPair pair1 = new PlayerPair();
    PlayerPair pair2 = new PlayerPair();
    PlayerPair pair3 = new PlayerPair();
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(List.of(pair1, pair2, pair3));

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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
