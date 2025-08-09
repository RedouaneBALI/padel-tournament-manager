package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TournamentServiceTest {

  private TournamentService            tournamentService;
  private TournamentRepository         tournamentRepository;
  private TournamentProgressionService progressionService;
  private DrawGenerationService        drawGenerationService;
  private PlayerPairService            playerPairService;

  @BeforeEach
  void setUp() {
    tournamentRepository  = mock(TournamentRepository.class);
    drawGenerationService = mock(DrawGenerationService.class);

    tournamentService = new TournamentService(
        tournamentRepository,
        drawGenerationService
    );
  }

  @Test
  void testGenerateDraw_shouldDelegateToDrawGenerationService() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(drawGenerationService.generateDraw(tournament, false)).thenReturn(tournament);

    Tournament result = tournamentService.generateDraw(1L, false);

    verify(drawGenerationService, times(1)).generateDraw(tournament, false);
    assertEquals(tournament, result);
  }

  @Test
  void testGetTournamentById_shouldReturnTournament() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    Tournament result = tournamentService.getTournamentById(1L);

    assertEquals(tournament, result);
  }

  @Test
  void testGetGamesByTournamentAndStage_shouldReturnGames() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    Round round = new Round(Stage.R16);

    tournament.getRounds().clear();
    tournament.getRounds().add(round);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertEquals(0, tournamentService.getGamesByTournamentAndStage(1L, Stage.R16).size());
  }
}