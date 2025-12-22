package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.UserFavoriteGame;
import io.github.redouanebali.model.UserFavoriteTournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.MatchFormatRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.repository.UserFavoriteGameRepository;
import io.github.redouanebali.repository.UserFavoriteTournamentRepository;
import io.github.redouanebali.repository.UserRepository;
import io.github.redouanebali.util.TestFixturesApp;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@DisplayName("FavoriteService Tests")
class FavoriteServiceTest {

  @Autowired
  private FavoriteService                  favoriteService;
  @Autowired
  private UserRepository                   userRepository;
  @Autowired
  private TournamentRepository             tournamentRepository;
  @Autowired
  private GameRepository                   gameRepository;
  @Autowired
  private MatchFormatRepository            matchFormatRepository;
  @Autowired
  private UserFavoriteTournamentRepository userFavoriteTournamentRepository;
  @Autowired
  private UserFavoriteGameRepository       userFavoriteGameRepository;

  private Game createPersistedTestGame() {
    io.github.redouanebali.model.MatchFormat format      = TestFixturesApp.createSimpleFormat(2);
    io.github.redouanebali.model.MatchFormat savedFormat = matchFormatRepository.save(format);
    Game                                     game        = new Game(savedFormat);
    return gameRepository.save(game);
  }

  @ParameterizedTest(name = "{index} - User: {0}, Tournament: {1}")
  @CsvSource({
      "user1@test.com, Tournament A",
      "user2@test.com, Tournament B",
      "user3@test.com, Tournament C"
  })
  @DisplayName("Should add tournament to favorites")
  void testAddFavoriteTournament(String userEmail, String tournamentName) {
    userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    favoriteService.addFavoriteTournament(userEmail, tournament.getId());

    assertTrue(favoriteService.isTournamentFavorite(userEmail, tournament.getId()),
               "Tournament should be marked as favorite");
  }

  @ParameterizedTest(name = "{index} - User: {0}, Tournament: {1}")
  @CsvSource({
      "user1@test.com, Tournament A",
      "user2@test.com, Tournament B"
  })
  @DisplayName("Should not duplicate tournament in favorites")
  void testAddFavoriteTournamentIdempotent(String userEmail, String tournamentName) {
    var        user       = userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    favoriteService.addFavoriteTournament(userEmail, tournament.getId());
    favoriteService.addFavoriteTournament(userEmail, tournament.getId());

    List<UserFavoriteTournament> favorites = userFavoriteTournamentRepository.findByUserOrderByAddedAtDesc(user);
    assertEquals(1, favorites.size(), "Should have only 1 favorite tournament");
  }

  @ParameterizedTest(name = "{index} - User: {0}, Tournament: {1}")
  @CsvSource({
      "user1@test.com, Tournament A",
      "user2@test.com, Tournament B"
  })
  @DisplayName("Should remove tournament from favorites")
  void testRemoveFavoriteTournament(String userEmail, String tournamentName) {
    userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    favoriteService.addFavoriteTournament(userEmail, tournament.getId());
    favoriteService.removeFavoriteTournament(userEmail, tournament.getId());

    assertFalse(favoriteService.isTournamentFavorite(userEmail, tournament.getId()),
                "Tournament should not be marked as favorite");
  }

  @ParameterizedTest(name = "{index} - Removing non-existent favorite for User: {0}, Tournament: {1}")
  @CsvSource({
      "user1@test.com, Tournament A",
      "user2@test.com, Tournament B"
  })
  @DisplayName("Should handle removal of non-existent favorite gracefully")
  void testRemoveNonExistentFavoriteTournament(String userEmail, String tournamentName) {
    userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Tournament tournament = tournamentRepository.save(TestFixturesApp.createTestTournament(tournamentName));

    favoriteService.removeFavoriteTournament(userEmail, tournament.getId());

    assertFalse(favoriteService.isTournamentFavorite(userEmail, tournament.getId()));
  }

  @Test
  @DisplayName("Should retrieve multiple favorite tournaments ordered by recency")
  void testGetFavoriteTournamentsOrdered() {
    var user = userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    Tournament t1 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament 1"));
    Tournament t2 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament 2"));
    Tournament t3 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament 3"));

    favoriteService.addFavoriteTournament("user@test.com", t1.getId());
    favoriteService.addFavoriteTournament("user@test.com", t2.getId());
    favoriteService.addFavoriteTournament("user@test.com", t3.getId());

    List<UserFavoriteTournament> favorites = favoriteService.getFavoriteTournaments("user@test.com");

    assertEquals(3, favorites.size(), "Should have 3 favorite tournaments");
    assertEquals(t3.getId(), favorites.get(0).getTournament().getId(),
                 "Most recent should be first");
    assertEquals(t1.getId(), favorites.get(2).getTournament().getId(),
                 "Oldest should be last");
  }

  @ParameterizedTest(name = "{index} - User: {0}")
  @CsvSource({
      "user1@test.com",
      "user2@test.com",
      "user3@test.com"
  })
  @DisplayName("Should add game to favorites")
  void testAddFavoriteGame(String userEmail) {
    userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Game game = createPersistedTestGame();

    favoriteService.addFavoriteGame(userEmail, game.getId());

    assertTrue(favoriteService.isGameFavorite(userEmail, game.getId()),
               "Game should be marked as favorite");
  }

  @ParameterizedTest(name = "{index} - User: {0}")
  @CsvSource({
      "user1@test.com",
      "user2@test.com"
  })
  @DisplayName("Should not duplicate game in favorites")
  void testAddFavoriteGameIdempotent(String userEmail) {
    var  user = userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Game game = createPersistedTestGame();

    favoriteService.addFavoriteGame(userEmail, game.getId());
    favoriteService.addFavoriteGame(userEmail, game.getId());

    List<UserFavoriteGame> favorites = userFavoriteGameRepository.findByUserOrderByAddedAtDesc(user);
    assertEquals(1, favorites.size(), "Should have only 1 favorite game");
  }

  @ParameterizedTest(name = "{index} - User: {0}")
  @CsvSource({
      "user1@test.com",
      "user2@test.com"
  })
  @DisplayName("Should remove game from favorites")
  void testRemoveFavoriteGame(String userEmail) {
    userRepository.save(TestFixturesApp.createTestUser(userEmail));
    Game game = createPersistedTestGame();

    favoriteService.addFavoriteGame(userEmail, game.getId());
    favoriteService.removeFavoriteGame(userEmail, game.getId());

    assertFalse(favoriteService.isGameFavorite(userEmail, game.getId()),
                "Game should not be marked as favorite");
  }

  @Test
  @DisplayName("Should retrieve multiple favorite games ordered by recency")
  void testGetFavoriteGamesOrdered() {
    var user = userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    Game g1 = createPersistedTestGame();
    Game g2 = createPersistedTestGame();
    Game g3 = createPersistedTestGame();

    favoriteService.addFavoriteGame("user@test.com", g1.getId());
    favoriteService.addFavoriteGame("user@test.com", g2.getId());
    favoriteService.addFavoriteGame("user@test.com", g3.getId());

    List<UserFavoriteGame> favorites = favoriteService.getFavoriteGames("user@test.com");

    assertEquals(3, favorites.size(), "Should have 3 favorite games");
    assertEquals(g3.getId(), favorites.get(0).getGame().getId(),
                 "Most recent should be first");
    assertEquals(g1.getId(), favorites.get(2).getGame().getId(),
                 "Oldest should be last");
  }

  @Test
  @DisplayName("Should isolate favorites between different users")
  void testFavoritesIsolationBetweenUsers() {
    var user1 = userRepository.save(TestFixturesApp.createTestUser("user1@test.com"));
    var user2 = userRepository.save(TestFixturesApp.createTestUser("user2@test.com"));

    Tournament t1 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament A"));
    Tournament t2 = tournamentRepository.save(TestFixturesApp.createTestTournament("Tournament B"));

    favoriteService.addFavoriteTournament("user1@test.com", t1.getId());
    favoriteService.addFavoriteTournament("user2@test.com", t2.getId());

    List<UserFavoriteTournament> user1Favorites = favoriteService.getFavoriteTournaments("user1@test.com");
    List<UserFavoriteTournament> user2Favorites = favoriteService.getFavoriteTournaments("user2@test.com");

    assertEquals(1, user1Favorites.size());
    assertEquals(1, user2Favorites.size());
    assertEquals(t1.getId(), user1Favorites.get(0).getTournament().getId());
    assertEquals(t2.getId(), user2Favorites.get(0).getTournament().getId());
  }

  @Test
  @DisplayName("Should handle empty favorites list gracefully")
  void testGetEmptyFavorites() {
    userRepository.save(TestFixturesApp.createTestUser("user@test.com"));

    List<UserFavoriteTournament> favoriteTournaments = favoriteService.getFavoriteTournaments("user@test.com");
    List<UserFavoriteGame>       favoriteGames       = favoriteService.getFavoriteGames("user@test.com");

    assertTrue(favoriteTournaments.isEmpty());
    assertTrue(favoriteGames.isEmpty());
  }

}

