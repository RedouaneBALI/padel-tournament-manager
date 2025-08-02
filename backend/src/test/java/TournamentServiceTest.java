import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.ScoreRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.TournamentProgressionService;
import io.github.redouanebali.service.TournamentService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TournamentServiceTest {

  private TournamentService            tournamentService;
  private TournamentProgressionService progressionService;
  private TournamentRepository         tournamentRepository;
  private GameRepository               gameRepository;
  private ScoreRepository              scoreRepository;

  @BeforeEach
  void setUp() {
    tournamentRepository = mock(TournamentRepository.class);
    gameRepository       = mock(GameRepository.class);
    scoreRepository      = mock(ScoreRepository.class);

    progressionService = mock(TournamentProgressionService.class);

    tournamentService = new TournamentService();
    tournamentService.setTournamentRepository(tournamentRepository);
    tournamentService.setGameRepository(gameRepository);
    tournamentService.setScoreRepository(scoreRepository);
    tournamentService.setProgressionService(progressionService);
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
    round.setGames(List.of(game));

    Tournament tournament = new Tournament();
    tournament.setRounds(new LinkedHashSet<>(List.of(round)));

    Score score = new Score();
    score.setSets(List.of(
        new SetScore(a1, b1),
        new SetScore(a2, b2)
    ));

    // Le score doit être persisté
    when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
    when(gameRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    boolean matchFinished = !expectedWinner.equals("null");
    //  when(any().isFinished()).thenReturn(matchFinished);

    if (expectedWinner.equals("TEAM_A")) {
      when(progressionService.getWinner(game)).thenReturn(teamA);
    } else if (expectedWinner.equals("TEAM_B")) {
      when(progressionService.getWinner(game)).thenReturn(teamB);
    }

    ScoreUpdateResponse response = tournamentService.updateGameScore(tournamentId, gameId, score);

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

    verify(scoreRepository).save(score);
    verify(gameRepository).save(game);
  }
}
