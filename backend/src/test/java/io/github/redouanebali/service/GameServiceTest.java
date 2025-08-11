package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class GameServiceTest {

  private TournamentRepository tournamentRepository;
  private TournamentService    tournamentService;
  private GameService          gameService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("bali.redouane@gmail.com", null, List.of())
    );
    tournamentRepository = mock(TournamentRepository.class);
    tournamentService    = mock(TournamentService.class);
    gameService          = new GameService(tournamentRepository, tournamentService);
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
    MatchFormat format = new MatchFormat();
    PlayerPair  teamA  = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game(format);
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Round round = new Round();
    round.addGames(List.of(game));

    Tournament tournament = new Tournament();
    tournament.setId(tournamentId);
    tournament.setTournamentFormat(TournamentFormat.KNOCKOUT);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(new LinkedList<>(List.of(round)));

    when(tournamentService.getTournamentById(any())).thenReturn(tournament);

    Score score = new Score();
    score.setSets(List.of(
        new SetScore(a1, b1),
        new SetScore(a2, b2)
    ));

    // Call service (no mocks for game.getWinner(); Game computes it from score)
    ScoreUpdateResponse response = gameService.updateGameScore(tournamentId, gameId, score);

    if ("TEAM_A".equals(expectedWinner)) {
      assertEquals(TeamSide.TEAM_A, response.getWinner());
    } else if ("TEAM_B".equals(expectedWinner)) {
      assertEquals(TeamSide.TEAM_B, response.getWinner());
    } else {
      // null -> no winner decided
      assertNull(response.getWinner());
    }
  }
}
