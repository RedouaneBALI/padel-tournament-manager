package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlayerPairServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @InjectMocks
  private PlayerPairService playerPairService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testAddPairs_shouldUpdateTournamentWithNewPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    PlayerPair       dto1    = new PlayerPair("Alice", "Bob", 1);
    PlayerPair       dto2    = new PlayerPair("Charlie", "Dave", 2);
    List<PlayerPair> dtoList = List.of(dto1, dto2);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, dtoList);

    assertEquals(2, updated.getPlayerPairs().size());
  }

  @Test
  void testAddPairs_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.addPairs(1L, List.of()));
  }

  @Test
  void testGetPairsByTournamentId_shouldReturnPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    PlayerPair pair = new PlayerPair();
    tournament.getPlayerPairs().add(pair);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    List<PlayerPair> result = playerPairService.getPairsByTournamentId(1L);
    assertEquals(1, result.size());
  }

  @Test
  void testGetPairsByTournamentId_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.getPairsByTournamentId(1L));
  }
}
