package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.github.redouanebali.repository.TournamentRepository;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GameServiceTest {

  private TournamentRepository         tournamentRepository;
  private TournamentProgressionService progressionService;
  private TournamentService            tournamentService;
  private MatchFormatService           matchFormatService;
  private GameService                  gameService;

  @BeforeEach
  void setUp() {
    tournamentRepository = mock(TournamentRepository.class);
    progressionService   = mock(TournamentProgressionService.class);
    tournamentService    = mock(TournamentService.class);
    matchFormatService   = mock(MatchFormatService.class);

    MatchFormat matchFormat = new MatchFormat();
    when(matchFormatService.getMatchFormatForRound(any(), any())).thenReturn(matchFormat);

    // Setup a Tournament with a Round and a Game, and stub the getTournamentById method.
    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game(matchFormat);
    game.setId(10L);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Round round = new Round();
    round.addGames(List.of(game));

    Tournament tournament = new Tournament();
    tournament.setRounds(new LinkedList<>(List.of(round)));

    when(tournamentService.getTournamentById(any())).thenReturn(tournament);

    gameService = new GameService(tournamentRepository, progressionService, tournamentService);
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

    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);

    Game game = new Game(new MatchFormat());
    game.setId(gameId);
    game.setTeamA(teamA);
    game.setTeamB(teamB);

    Round round = new Round();
    round.addGames(List.of(game));

    Tournament tournament = new Tournament();
    tournament.setRounds(new LinkedList<>(List.of(round)));

    Score score = new Score();
    score.setSets(List.of(
        new SetScore(a1, b1),
        new SetScore(a2, b2)
    ));

    //   when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    //   when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(tournamentService.getTournamentById(tournamentId)).thenReturn(tournament);
    //   when(gameRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    if (expectedWinner.equals("TEAM_A")) {
      when(progressionService.getWinner(game)).thenReturn(teamA);
    } else if (expectedWinner.equals("TEAM_B")) {
      when(progressionService.getWinner(game)).thenReturn(teamB);
    }

    ScoreUpdateResponse response = gameService.updateGameScore(tournamentId, gameId, score);

    if (expectedWinner.equals("TEAM_A")) {
      assertEquals(TeamSide.TEAM_A, response.getWinner());
      assertTrue(response.isTournamentUpdated());
    } else if (expectedWinner.equals("TEAM_B")) {
      assertEquals(TeamSide.TEAM_B, response.getWinner());
      assertTrue(response.isTournamentUpdated());
    } else {
      assertNull(response.getWinner());
      assertFalse(response.isTournamentUpdated());
    }

    //   verify(scoreRepository).save(score);
    //   verify(gameRepository).save(game);
  }
}
