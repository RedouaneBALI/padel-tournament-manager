package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.redouanebali.TestFixtures;
import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GameServiceTest {

  private TournamentRepository  tournamentRepository;
  private TournamentService     tournamentService;
  private DrawGenerationService drawGenerationService;
  private GameService           gameService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
    tournamentRepository  = mock(TournamentRepository.class);
    tournamentService     = mock(TournamentService.class);
    drawGenerationService = mock(DrawGenerationService.class);
    gameService           = new GameService(tournamentRepository, tournamentService, drawGenerationService);
  }


  @ParameterizedTest
  @CsvSource({
      "6,3,6,4,TEAM_A",
      "3,6,4,6,TEAM_B",
      "6,3,3,6,null"
  })
  void testUpdateGameScore_returnsCorrectWinningTeamSide(
      int a1, int b1, int a2, int b2, String expectedWinner) {

    Long tournamentId = 1L;
    Long gameId       = 10L;

    // Real domain objects
    MatchFormat      format = new MatchFormat();
    List<PlayerPair> pairs  = TestFixtures.createPlayerPairs(2);
    PlayerPair       teamA  = pairs.getFirst();
    PlayerPair       teamB  = pairs.get(1);

    Game game = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Round round = new Round();
    round.addGames(List.of(game));

    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(new LinkedList<>(List.of(round)));
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());

    when(tournamentService.getTournamentById(any())).thenReturn(tournament);

    Score score = new Score();
    score.setSets(List.of(
        new SetScore(a1, b1),
        new SetScore(a2, b2)
    ));

    // Call io.github.redouanebali.api.service (no mocks for game.getWinner(); Game computes it from score)
    UpdateScoreDTO response = gameService.updateGameScore(tournamentId, gameId, score);

    if ("TEAM_A".equals(expectedWinner)) {
      assertEquals(TeamSide.TEAM_A, response.getWinner());
    } else if ("TEAM_B".equals(expectedWinner)) {
      assertEquals(TeamSide.TEAM_B, response.getWinner());
    } else {
      // null -> no winner decided
      assertNull(response.getWinner());
    }
  }

  @org.junit.jupiter.api.Test
  void testUpdateGame_updatesScoreAndCourt() {
    Long             tournamentId = 2L;
    Long             gameId       = 20L;
    MatchFormat      format       = new MatchFormat();
    List<PlayerPair> pairs        = TestFixtures.createPlayerPairs(2);
    PlayerPair       teamA        = pairs.getFirst();
    PlayerPair       teamB        = pairs.get(1);
    Game             game         = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Round round = new Round();
    round.addGames(List.of(game));
    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().add(round);
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);
    io.github.redouanebali.dto.request.UpdateGameRequest req = new io.github.redouanebali.dto.request.UpdateGameRequest();
    req.setCourt("Court 1");
    Score score = new Score();
    score.setSets(List.of(new SetScore(6, 3), new SetScore(6, 4)));
    req.setScore(score);
    UpdateScoreDTO result = gameService.updateGame(tournamentId, gameId, req);
    assertEquals("Court 1", game.getCourt());
    assertEquals(6, game.getScore().getSets().getFirst().getTeamAScore());
    assertEquals(TeamSide.TEAM_A, result.getWinner());
  }

  @org.junit.jupiter.api.Test
  void testUpdateGameScore_throwsIfGameNotFound() {
    Long       tournamentId = 3L;
    Long       gameId       = 999L;
    Tournament tournament   = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().clear();
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);
    Score score = new Score();
    assertThrows(IllegalArgumentException.class, () -> gameService.updateGameScore(tournamentId, gameId, score));
  }

  @org.junit.jupiter.api.Test
  void testUpdateGame_throwsIfTournamentNotFound() {
    Long tournamentId = 4L;
    Long gameId       = 888L;
    when(tournamentService.getTournamentById(tournamentId)).thenThrow(new IllegalArgumentException("Tournament not found"));
    io.github.redouanebali.dto.request.UpdateGameRequest req = new io.github.redouanebali.dto.request.UpdateGameRequest();
    assertThrows(IllegalArgumentException.class, () -> gameService.updateGame(tournamentId, gameId, req));
  }
}
