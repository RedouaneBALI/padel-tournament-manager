package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.response.UpdateScoreDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.GamePoint;
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
import io.github.redouanebali.util.TestFixturesApp;
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
  private TournamentMapper      tournamentMapper;
  private GamePointManager      gamePointManager;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
    tournamentRepository  = mock(TournamentRepository.class);
    tournamentService     = mock(TournamentService.class);
    drawGenerationService = mock(DrawGenerationService.class);
    tournamentMapper      = mock(TournamentMapper.class);
    gamePointManager      = new GamePointManager(); // Use real instance for game point logic
    gameService           = new GameService(tournamentRepository, tournamentService, drawGenerationService, tournamentMapper, gamePointManager);
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
    MatchFormat      format = TestFixturesApp.createSimpleFormat(2);
    List<PlayerPair> pairs  = TestFixturesApp.createPlayerPairs(2);
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
    List<PlayerPair> pairs        = TestFixturesApp.createPlayerPairs(2);
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

  @org.junit.jupiter.api.Test
  void testMixUpdateGamePointAndDirectScoreIncrement() {
    Long        tournamentId = 5L;
    Long        gameId       = 50L;
    MatchFormat format       = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);
    List<PlayerPair> pairs = TestFixturesApp.createPlayerPairs(2);
    PlayerPair       teamA = pairs.getFirst();
    PlayerPair       teamB = pairs.get(1);
    Game             game  = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    // Explicit initialization of the score for consistency
    Score initialScore = new Score();
    game.setScore(initialScore);
    Round round = new Round();
    round.addGames(List.of(game));
    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().add(round);
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);

    // 1. Increment point by point (method 1)
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A); // 15-0
    assertEquals(GamePoint.QUINZE, game.getScore().getCurrentGamePointA());
    assertEquals(GamePoint.ZERO, game.getScore().getCurrentGamePointB());
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A); // 30-0
    assertEquals(GamePoint.TRENTE, game.getScore().getCurrentGamePointA());
    assertEquals(GamePoint.ZERO, game.getScore().getCurrentGamePointB());

    io.github.redouanebali.dto.request.UpdateGameRequest req = new io.github.redouanebali.dto.request.UpdateGameRequest();
    req.setCourt("Court 2");
    Score score = new Score();
    score.setSets(List.of(new SetScore(6, 3), new SetScore(6, 4)));
    req.setScore(score);
    UpdateScoreDTO result = gameService.updateGame(tournamentId, gameId, req);
    assertEquals("Court 2", game.getCourt());
    assertEquals(6, game.getScore().getSets().getFirst().getTeamAScore());
    assertEquals(TeamSide.TEAM_A, result.getWinner());
    // Check that current points are reset
    assertNull(game.getScore().getCurrentGamePointA());
    assertNull(game.getScore().getCurrentGamePointB());
  }

  @org.junit.jupiter.api.Test
  void testMixDirectScoreUpdateAndIncrementGamePoint() {
    Long             tournamentId = 6L;
    Long             gameId       = 60L;
    MatchFormat      format       = new MatchFormat();
    List<PlayerPair> pairs        = TestFixturesApp.createPlayerPairs(2);
    PlayerPair       teamA        = pairs.getFirst();
    PlayerPair       teamB        = pairs.get(1);
    Game             game         = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score initialScore = new Score();
    game.setScore(initialScore);
    Round round = new Round();
    round.addGames(List.of(game));
    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().add(round);
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);

    io.github.redouanebali.dto.request.UpdateGameRequest req = new io.github.redouanebali.dto.request.UpdateGameRequest();
    req.setCourt("Court 3");
    Score score = new Score();
    score.setSets(List.of(new SetScore(4, 6), new SetScore(2, 6)));
    req.setScore(score);
    UpdateScoreDTO result = gameService.updateGame(tournamentId, gameId, req);
    assertEquals("Court 3", game.getCourt());
    assertEquals(4, game.getScore().getSets().getFirst().getTeamAScore());
    assertEquals(TeamSide.TEAM_B, result.getWinner());
    // Check that current points are reset
    assertNull(game.getScore().getCurrentGamePointA());
    assertNull(game.getScore().getCurrentGamePointB());

    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_B); // 4-6 2-6, 0-15
    assertEquals(GamePoint.QUINZE, game.getScore().getCurrentGamePointB());
    assertEquals(GamePoint.ZERO, game.getScore().getCurrentGamePointA());
    assertEquals(4, game.getScore().getSets().getFirst().getTeamAScore());
    assertEquals(6, game.getScore().getSets().getFirst().getTeamBScore());
    assertEquals(2, game.getScore().getSets().get(1).getTeamAScore());
    assertEquals(6, game.getScore().getSets().get(1).getTeamBScore());
  }

  @org.junit.jupiter.api.Test
  void testAddSetAfterDirectScoreUpdateThenIncrement() {
    Long             tournamentId = 7L;
    Long             gameId       = 70L;
    MatchFormat      format       = new MatchFormat();
    List<PlayerPair> pairs        = TestFixturesApp.createPlayerPairs(2);
    PlayerPair       teamA        = pairs.getFirst();
    PlayerPair       teamB        = pairs.get(1);
    Game             game         = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score initialScore = new Score();
    game.setScore(initialScore);
    Round round = new Round();
    round.addGames(List.of(game));
    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().add(round);
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);

    io.github.redouanebali.dto.request.UpdateGameRequest req = new io.github.redouanebali.dto.request.UpdateGameRequest();
    req.setCourt("Court Test");
    Score score = new Score();
    score.setSets(List.of(new SetScore(6, 2)));
    req.setScore(score);
    gameService.updateGame(tournamentId, gameId, req);
    assertEquals(2, game.getScore().getSets().size());
    assertEquals(6, game.getScore().getSets().get(0).getTeamAScore());
    assertEquals(2, game.getScore().getSets().get(0).getTeamBScore());
    assertEquals(0, game.getScore().getSets().get(1).getTeamAScore());
    assertEquals(0, game.getScore().getSets().get(1).getTeamBScore());

    // Increment 4 points for TEAM_A to win a game
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A);
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A);
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A);
    gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A);
    assertEquals(2, game.getScore().getSets().size());
    assertEquals(6, game.getScore().getSets().get(0).getTeamAScore());
    assertEquals(2, game.getScore().getSets().get(0).getTeamBScore());
    assertEquals(1, game.getScore().getSets().get(1).getTeamAScore());
    assertEquals(0, game.getScore().getSets().get(1).getTeamBScore());
  }

  @org.junit.jupiter.api.Test
  void testIncrementGamePoint_createsNewSetAfterSetWin() {
    Long             tournamentId = 8L;
    Long             gameId       = 80L;
    MatchFormat      format       = TestFixturesApp.createSimpleFormat(2);
    List<PlayerPair> pairs        = TestFixturesApp.createPlayerPairs(2);
    PlayerPair       teamA        = pairs.getFirst();
    PlayerPair       teamB        = pairs.get(1);
    Game             game         = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    Score initialScore = new Score();
    // Set 1: 6-2, Set 2: 2-5
    initialScore.setSets(new LinkedList<>(List.of(
        new SetScore(6, 2),
        new SetScore(2, 5)
    )));
    game.setScore(initialScore);
    Round round = new Round();
    round.addGames(List.of(game));
    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.getRounds().add(round);
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);

    // Incrémente pour faire passer le 2e set à 2-6 (TEAM_B gagne le set)
    for (int i = 0; i < 4; i++) {
      gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_B);
    }
    // After set win, a 3rd empty set should be created
    assertEquals(3, game.getScore().getSets().size(), "A new set should be created after set win");
    assertEquals(0, game.getScore().getSets().get(2).getTeamAScore());
    assertEquals(0, game.getScore().getSets().get(2).getTeamBScore());

    // Continue incrementing, games should be added to the new set
    for (int i = 0; i < 4; i++) {
      gameService.incrementGamePoint(tournamentId, gameId, TeamSide.TEAM_A);
    }
    assertEquals(1, game.getScore().getSets().get(2).getTeamAScore(), "Games should be added to the new set");
    assertEquals(0, game.getScore().getSets().get(2).getTeamBScore());
  }
}
