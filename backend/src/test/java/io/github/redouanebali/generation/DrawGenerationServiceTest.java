package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.DrawGenerationService;
import io.github.redouanebali.service.TournamentProgressionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DrawGenerationServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @Mock
  private TournamentProgressionService progressionService;

  @InjectMocks
  private DrawGenerationService drawGenerationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGenerateDrawAlgorithmicKnockout() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setTournamentFormat(TournamentFormat.KNOCKOUT);
    tournament.setNbSeeds(2);

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

    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(2);
    Round                  newRound  = generator.generateAlgorithmicRound(tournament.getPlayerPairs());

    // inject the same stage to match
    newRound.setStage(Stage.SEMIS);

    // Mock the repository save
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = drawGenerationService.generateDraw(tournament, false);

    assertEquals(tournament, updated);
    verify(tournamentRepository).save(tournament);
    verify(progressionService).propagateWinners(tournament);
  }

  @Test
  void testGenerateDrawDrawGroupStage() {
    Tournament tournament = new Tournament();
    tournament.setId(2L);
    tournament.setTournamentFormat(TournamentFormat.GROUP_STAGE);
    tournament.setNbSeeds(3);
    tournament.setNbPools(1);
    tournament.setNbPairsPerPool(3);

    PlayerPair pair1 = new PlayerPair();
    PlayerPair pair2 = new PlayerPair();
    PlayerPair pair3 = new PlayerPair();
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(List.of(pair1, pair2, pair3));

    GroupRoundGenerator generator = new GroupRoundGenerator(3, 1, 3);
    Round               newRound  = generator.generateManualRound(tournament.getPlayerPairs());
    newRound.setStage(Stage.GROUPS);

    Round existingRound = new Round();
    existingRound.setStage(Stage.GROUPS);

    tournament.getRounds().clear();
    tournament.getRounds().add(existingRound);

    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = drawGenerationService.generateDraw(tournament, true);

    assertEquals(tournament, updated);
    verify(tournamentRepository).save(tournament);
    verify(progressionService, never()).propagateWinners(tournament);
  }
}
