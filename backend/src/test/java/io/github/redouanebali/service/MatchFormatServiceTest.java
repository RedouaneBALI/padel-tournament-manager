package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MatchFormatServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @InjectMocks
  private MatchFormatService matchFormatService;

  private Tournament  tournament;
  private Round       round;
  private MatchFormat existingFormat;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    existingFormat = new MatchFormat();
    existingFormat.setNumberOfSetsToWin(2);
    existingFormat.setPointsPerSet(6);
    existingFormat.setAdvantage(true);
    existingFormat.setSuperTieBreakInFinalSet(false);

    round = new Round();
    round.setStage(Stage.R32);
    round.setMatchFormat(existingFormat);

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.getRounds().clear();
    tournament.getRounds().add(round);
  }

  @Test
  void testGetMatchFormatForRound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    MatchFormat result = matchFormatService.getMatchFormatForRound(1L, Stage.R32);

    assertEquals(existingFormat, result);
    verify(tournamentRepository).findById(1L);
  }

  @Test
  void testUpdateMatchFormatForRound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    MatchFormat newFormat = new MatchFormat();
    newFormat.setNumberOfSetsToWin(3);
    newFormat.setPointsPerSet(7);
    newFormat.setAdvantage(false);
    newFormat.setSuperTieBreakInFinalSet(true);

    MatchFormat updated = matchFormatService.updateMatchFormatForRound(1L, Stage.R32, newFormat);

    assertEquals(3, existingFormat.getNumberOfSetsToWin());
    assertEquals(7, existingFormat.getPointsPerSet());
    assertFalse(existingFormat.isAdvantage());
    assertTrue(existingFormat.isSuperTieBreakInFinalSet());
    assertEquals(newFormat, updated);

    verify(tournamentRepository).save(tournament);
  }
}
