package io.github.redouanebali.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("GameTournamentIdMapping DTO Tests")
class GameTournamentIdMappingTest {

  @Test
  @DisplayName("Should create mapping with correct values")
  void testCreateMapping() {
    Long gameId       = 123L;
    Long tournamentId = 456L;

    GameTournamentIdMapping mapping = new GameTournamentIdMapping(gameId, tournamentId);

    assertNotNull(mapping, "Mapping should not be null");
    assertEquals(gameId, mapping.getGameId(), "Game ID should match");
    assertEquals(tournamentId, mapping.getTournamentId(), "Tournament ID should match");
  }

  @ParameterizedTest(name = "{index} - Game: {0}, Tournament: {1}")
  @CsvSource({
      "1, 100",
      "999, 888",
      "42, 42"
  })
  @DisplayName("Should handle various ID values")
  void testMappingWithVariousIds(Long gameId, Long tournamentId) {
    GameTournamentIdMapping mapping = new GameTournamentIdMapping(gameId, tournamentId);

    assertEquals(gameId, mapping.getGameId());
    assertEquals(tournamentId, mapping.getTournamentId());
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void testMappingWithNullValues() {
    GameTournamentIdMapping mapping = new GameTournamentIdMapping(null, null);

    assertNull(mapping.getGameId(), "Game ID can be null");
    assertNull(mapping.getTournamentId(), "Tournament ID can be null");
  }

}

