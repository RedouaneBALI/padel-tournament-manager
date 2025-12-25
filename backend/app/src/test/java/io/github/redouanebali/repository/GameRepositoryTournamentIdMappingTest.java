package io.github.redouanebali.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.util.TestFixturesApp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@DisplayName("GameRepository Tests")
class GameRepositoryTournamentIdMappingTest {

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private MatchFormatRepository matchFormatRepository;

  private io.github.redouanebali.model.MatchFormat testFormat;

  @BeforeEach
  void setUp() {
    testFormat = TestFixturesApp.createSimpleFormat(2);
    matchFormatRepository.save(testFormat);
  }

  @Test
  @DisplayName("Should save and retrieve games without errors")
  void testGameCRUDOperations() {
    Game game      = new Game(testFormat);
    Game savedGame = gameRepository.save(game);

    assertNotNull(savedGame.getId(), "Saved game should have an ID");
    assertTrue(gameRepository.existsById(savedGame.getId()), "Game should exist in repository");
  }

  @Test
  @DisplayName("Should handle multiple games without errors")
  void testMultipleGameCreation() {
    int        gameCount = 5;
    List<Game> games     = new java.util.ArrayList<>();

    for (int i = 0; i < gameCount; i++) {
      Game game = new Game(testFormat);
      games.add(gameRepository.save(game));
    }

    assertFalse(games.isEmpty(), "Should create multiple games");
    assertEquals(gameCount, games.size(), "Should have created correct number of games");
  }

}

