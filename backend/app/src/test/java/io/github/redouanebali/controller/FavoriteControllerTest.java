package io.github.redouanebali.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.mapper.FavoriteMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.MatchFormatRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.repository.UserRepository;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.FavoriteService;
import io.github.redouanebali.util.TestFixturesApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("h2")
@Transactional
@DisplayName("FavoriteController Tests")
class FavoriteControllerTest {

  @Autowired
  private MockMvc               mockMvc;
  @Autowired
  private UserRepository        userRepository;
  @Autowired
  private TournamentRepository  tournamentRepository;
  @Autowired
  private GameRepository        gameRepository;
  @Autowired
  private MatchFormatRepository matchFormatRepository;
  @Autowired
  private FavoriteService       favoriteService;
  @Autowired
  private FavoriteMapper        favoriteMapper;

  private MockedStatic<SecurityUtil> secMock;

  private Game createPersistedTestGame() {
    io.github.redouanebali.model.MatchFormat format      = TestFixturesApp.createSimpleFormat(2);
    io.github.redouanebali.model.MatchFormat savedFormat = matchFormatRepository.save(format);
    Game                                     game        = new Game(savedFormat);
    return gameRepository.save(game);
  }

  private Tournament createTournamentWithGame(String tournamentName, int seed1, int seed2) {
    Tournament                         tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));
    io.github.redouanebali.model.Round round      = new io.github.redouanebali.model.Round(io.github.redouanebali.model.Stage.GROUPS);
    round.setMatchFormat(TestFixturesApp.createSimpleFormat(2));
    tournament.getRounds().add(round);
    tournament = tournamentRepository.save(tournament);

    var savedRound = tournament.getRounds().getFirst();
    savedRound.addGame(TestFixturesApp.buildPairWithSeed(seed1), TestFixturesApp.buildPairWithSeed(seed2));
    return tournamentRepository.save(tournament);
  }

  @BeforeEach
  public void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
    secMock.when(SecurityUtil::currentUserId).thenReturn("user@test.com");
  }

  @AfterEach
  public void tearDown() {
    if (secMock != null) {
      secMock.close();
    }
  }

  @ParameterizedTest(name = "{index} - Tournament: {0}")
  @CsvSource({
      "Tournament A",
      "Tournament B",
      "Tournament C"
  })
  @DisplayName("Should add tournament to favorites")
  void testAddFavoriteTournament(String tournamentName) throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    mockMvc.perform(post("/favorites/tournaments/{id}", tournament.getId())
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Should retrieve favorite tournaments")
  void testGetFavoriteTournaments() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    Tournament t1 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament 1"));
    Tournament t2 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament 2"));

    favoriteService.addFavoriteTournament("user@test.com", t1.getId());
    favoriteService.addFavoriteTournament("user@test.com", t2.getId());

    mockMvc.perform(get("/favorites/tournaments")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)));
  }

  @ParameterizedTest(name = "{index} - Tournament: {0}")
  @CsvSource({
      "Tournament A",
      "Tournament B"
  })
  @DisplayName("Should remove tournament from favorites")
  void testRemoveFavoriteTournament(String tournamentName) throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    favoriteService.addFavoriteTournament("user@test.com", tournament.getId());

    mockMvc.perform(delete("/favorites/tournaments/{id}", tournament.getId())
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should add game to favorites")
  void testAddFavoriteGame() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));
    Game game = createPersistedTestGame();

    mockMvc.perform(post("/favorites/games/{id}", game.getId())
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Should retrieve favorite games")
  void testGetFavoriteGames() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    Game g1 = createPersistedTestGame();
    Game g2 = createPersistedTestGame();

    favoriteService.addFavoriteGame("user@test.com", g1.getId());
    favoriteService.addFavoriteGame("user@test.com", g2.getId());

    mockMvc.perform(get("/favorites/games")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  @DisplayName("Should remove game from favorites")
  void testRemoveFavoriteGame() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));
    Game game = createPersistedTestGame();

    favoriteService.addFavoriteGame("user@test.com", game.getId());

    mockMvc.perform(delete("/favorites/games/{id}", game.getId())
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return empty list when no favorites exist")
  void testGetEmptyFavoriteTournaments() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    mockMvc.perform(get("/favorites/tournaments")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @DisplayName("Should return empty list when no favorite games exist")
  void testGetEmptyFavoriteGames() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    mockMvc.perform(get("/favorites/games")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(0)));
  }

  @ParameterizedTest(name = "{index} - Tournament: {0}, Seeds: {1},{2}")
  @CsvSource({
      "Test Tournament,1,2",
      "Tournament A,3,4",
      "Tournament B,5,6"
  })
  @DisplayName("Should include tournament and validate DTO structure in favorite games")
  void testGetFavoriteGames_IncludesTournamentWithValidStructure(String tournamentName, int seed1, int seed2) throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));
    Tournament tournament = createTournamentWithGame(tournamentName, seed1, seed2);
    Game       game       = tournament.getRounds().getFirst().getGames().getFirst();

    favoriteService.addFavoriteGame("user@test.com", game.getId());

    mockMvc.perform(get("/favorites/games")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].id").value(game.getId().intValue()))
           .andExpect(jsonPath("$[0].tournament.id").value(tournament.getId().intValue()))
           .andExpect(jsonPath("$[0].tournament.name").value(tournamentName))
           .andExpect(jsonPath("$[0].finished").isBoolean())
           .andExpect(jsonPath("$[0].teamA").exists())
           .andExpect(jsonPath("$[0].teamA.id").isNumber())
           .andExpect(jsonPath("$[0].teamA.seed").isNumber())
           .andExpect(jsonPath("$[0].teamB").exists())
           .andExpect(jsonPath("$[0].teamB.id").isNumber())
           .andExpect(jsonPath("$[0].teamB.seed").isNumber());
  }

  @Test
  @DisplayName("Should load favorite games from multiple tournaments with correct mappings")
  void testGetFavoriteGames_LoadsMultipleTournamentGames() throws Exception {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    Tournament tournament1 = createTournamentWithGame("Tournament 1", 1, 2);
    Tournament tournament2 = createTournamentWithGame("Tournament 2", 3, 4);
    Game       game1       = tournament1.getRounds().getFirst().getGames().getFirst();
    Game       game2       = tournament2.getRounds().getFirst().getGames().getFirst();

    favoriteService.addFavoriteGame("user@test.com", game1.getId());
    favoriteService.addFavoriteGame("user@test.com", game2.getId());

    mockMvc.perform(get("/favorites/games")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[*].tournament").exists())
           .andExpect(jsonPath("$[?(@.id == " + game1.getId() + ")].tournament.id").value(tournament1.getId().intValue()))
           .andExpect(jsonPath("$[?(@.id == " + game2.getId() + ")].tournament.id").value(tournament2.getId().intValue()))
           .andExpect(jsonPath("$[0].teamA.id").isNumber())
           .andExpect(jsonPath("$[0].teamA.seed").exists())
           .andExpect(jsonPath("$[0].teamB.id").isNumber())
           .andExpect(jsonPath("$[0].teamB.seed").exists());
  }

}
