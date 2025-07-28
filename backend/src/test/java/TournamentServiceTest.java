import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.PlayerRepository;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.TournamentService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TournamentServiceTest {

  private TournamentService tournamentService;

  private PlayerPairRepository playerPairRepository;
  private PlayerRepository     playerRepository;
  private TournamentRepository tournamentRepository;
  private RoundRepository      roundRepository;
  private GameRepository       gameRepository;

  @BeforeEach
  void setUp() {
    playerPairRepository = mock(PlayerPairRepository.class);
    playerRepository     = mock(PlayerRepository.class);
    tournamentRepository = mock(TournamentRepository.class);
    roundRepository      = mock(RoundRepository.class);
    gameRepository       = mock(GameRepository.class);

    tournamentService = new TournamentService();
    tournamentService.setPlayerPairRepository(playerPairRepository);
    tournamentService.setPlayerRepository(playerRepository);
    tournamentService.setTournamentRepository(tournamentRepository);
    tournamentService.setRoundRepository(roundRepository);
    tournamentService.setGameRepository(gameRepository);
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 4, 8, 16, 32, 64})
  void testCreateTournament_createsCorrectNumberOfRoundsAndGames(int nbMaxPairs) {
    Tournament tournament = new Tournament();
    tournament.setNbMaxPairs(nbMaxPairs);
    tournament.setRounds(new LinkedHashSet<>());

    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament result = tournamentService.createTournament(tournament);

    assertNotNull(result);

    // Le nombre de rounds devrait être log2(nbMaxPairs)
    int expectedRounds = (int) (Math.log(nbMaxPairs) / Math.log(2));
    assertEquals(expectedRounds, result.getRounds().size(), "Unexpected number of rounds for " + nbMaxPairs + " pairs");

    for (Round round : result.getRounds()) {
      int expectedGames = round.getStage().getNbTeams() / 2;
      assertEquals(expectedGames, round.getGames().size(),
                   "Incorrect number of games in round: " + round.getStage());
    }

    verify(gameRepository, atLeastOnce()).save(any(Game.class));
    verify(roundRepository, atLeastOnce()).save(any(Round.class));
    verify(tournamentRepository, times(2)).save(any(Tournament.class));
  }

  @Test
  void testAddPairs_replacesOldPairsAndReturnsCorrectCount() {
    Tournament tournament = new Tournament();
    tournament.setPlayerPairs(new ArrayList<>());

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(playerRepository.save(any())).thenAnswer(invocation -> {
      Player player = invocation.getArgument(0);
      player.setId(new Random().nextLong());
      return player;
    });
    when(playerPairRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<SimplePlayerPairDTO> inputPairs = List.of(
        new SimplePlayerPairDTO("Alice", "Bob", 1),
        new SimplePlayerPairDTO("Charlie", "David", 2)
    );

    int result = tournamentService.addPairs(1L, inputPairs);

    assertEquals(2, result);
    assertEquals(2, tournament.getPlayerPairs().size());

    verify(tournamentRepository, times(2)).save(tournament);
  }

  @Test
  void testGenerateDraw_createsCorrectMatchAssignments() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    PlayerPair p1 = new PlayerPair(1L, new Player("Alice"), new Player("Bob"), 1);
    PlayerPair p2 = new PlayerPair(2L, new Player("Charlie"), new Player("David"), 2);
    PlayerPair p3 = new PlayerPair(3L, new Player("Eve"), new Player("Frank"), 3);
    PlayerPair p4 = new PlayerPair(4L, new Player("Gina"), new Player("Hank"), 4);

    tournament.setPlayerPairs(new ArrayList<>(List.of(p1, p2, p3, p4)));

    Round semiGames = new Round(Stage.SEMIS);
    Game  g1        = new Game();
    g1.setId(1L);
    Game g2 = new Game();
    g2.setId(2L);
    semiGames.setGames(List.of(g1, g2));

    tournament.setRounds(Set.of(semiGames));

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(gameRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(roundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    KnockoutRoundGenerator generator      = new KnockoutRoundGenerator(List.of(p1, p2, p3, p4), 0);
    Round                  generatedRound = generator.generate();

    when(gameRepository.save(any())).thenReturn(new Game());

    Tournament result = tournamentService.generateDraw(1L);

    assertNotNull(result);
    assertEquals(1, result.getRounds().size());

    Round round = result.getRounds().iterator().next();
    for (Game game : round.getGames()) {
      assertNotNull(game.getTeamA());
      assertNotNull(game.getTeamB());
    }
  }

  @Test
  void testGetMatchFormatForRound_returnsCorrectFormat() {
    Tournament  tournament = new Tournament();
    Round       round      = new Round(Stage.QUARTERS);
    MatchFormat format     = new MatchFormat();
    round.setMatchFormat(format);
    tournament.setRounds(Set.of(round));

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    MatchFormat result = tournamentService.getMatchFormatForRound(1L, Stage.QUARTERS);
    assertEquals(format, result);
  }

  @Test
  void testUpdateMatchFormatForRound_updatesCorrectly() {
    Tournament tournament = new Tournament();
    Round      round      = new Round(Stage.SEMIS);
    tournament.setRounds(Set.of(round));

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(roundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    MatchFormat newFormat = new MatchFormat();
    MatchFormat result    = tournamentService.updateMatchFormatForRound(1L, Stage.SEMIS, newFormat);

    assertEquals(newFormat, round.getMatchFormat());
    assertEquals(newFormat, result);
  }
}