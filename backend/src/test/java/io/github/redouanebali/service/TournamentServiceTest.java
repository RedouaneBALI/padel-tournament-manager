package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TournamentServiceTest {

  private TournamentService            tournamentService;
  private TournamentRepository         tournamentRepository;
  private PlayerRepository             playerRepository;
  private PlayerPairRepository         playerPairRepository;
  private RoundRepository              roundRepository;
  private GameRepository               gameRepository;
  private MatchFormatRepository        matchFormatRepository;
  private TournamentProgressionService progressionService;

  @BeforeEach
  void setUp() {
    tournamentRepository  = mock(TournamentRepository.class);
    playerRepository      = mock(PlayerRepository.class);
    playerPairRepository  = mock(PlayerPairRepository.class);
    roundRepository       = mock(RoundRepository.class);
    gameRepository        = mock(GameRepository.class);
    matchFormatRepository = mock(MatchFormatRepository.class);
    progressionService    = mock(TournamentProgressionService.class);

    tournamentService = new TournamentService(
        playerPairRepository,
        playerRepository,
        tournamentRepository,
        roundRepository,
        gameRepository,
        matchFormatRepository,
        progressionService
    );
  }

  @Test
  void testAddPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setPlayerPairs(new ArrayList<>());
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerPairRepository.save(any(PlayerPair.class))).thenAnswer(invocation -> invocation.getArgument(0));

    SimplePlayerPairDTO dto    = new SimplePlayerPairDTO("Alice", "Bob", 1);
    int                 result = tournamentService.addPairs(1L, List.of(dto));

    assertEquals(1, result);
    verify(tournamentRepository, times(2)).save(any(Tournament.class));
  }

  @Test
  void testGetMatchFormatForRound_shouldReturnMatchFormat() {
    Tournament  tournament  = new Tournament();
    Round       round       = new Round(Stage.R16);
    MatchFormat matchFormat = new MatchFormat();
    round.setMatchFormat(matchFormat);
    tournament.setRounds(Set.of(round));

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    MatchFormat result = tournamentService.getMatchFormatForRound(1L, Stage.R16);

    assertEquals(matchFormat, result);
  }
}