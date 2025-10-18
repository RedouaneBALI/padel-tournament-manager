package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

/**
 * Parameterized test to verify that getPairsByTournamentId correctly reorders BYEs at the right absolute positions (opposite seeds) to improve UX in
 * manual mode.
 */
class PlayerPairServiceByeReorderingTest {

  @ParameterizedTest(name = "Reorder BYEs: {0} teams, {1} seeds, {2} BYEs (drawSize={3})")
  @CsvSource({
      "6,  2,  2,  8",    // 6 teams, 2 seeds, 2 BYEs, drawSize 8
      "14, 4,  2,  16",   // 14 teams, 4 seeds, 2 BYEs, drawSize 16
      "12, 4,  4,  16",   // 12 teams, 4 seeds, 4 BYEs, drawSize 16
      "28, 8,  4,  32",   // 28 teams, 8 seeds, 4 BYEs, drawSize 32
      "40, 16, 24, 64"    // 40 teams, 16 seeds, 24 BYEs, drawSize 64
  })
  void testGetPairsByTournamentId_shouldReorderByesAtCorrectPositions(
      int nbTeams,
      int nbSeeds,
      int nbByes,
      int drawSize
  ) {
    // Given: Setup mocks
    TournamentRepository tournamentRepository = Mockito.mock(TournamentRepository.class);
    SecurityProps        securityProps        = Mockito.mock(SecurityProps.class);
    TournamentMapper     tournamentMapper     = Mockito.mock(TournamentMapper.class);
    PlayerPairService    service              = new PlayerPairService(tournamentRepository, securityProps, tournamentMapper);

    // Load expected BYE positions from JSON file
    List<Integer> expectedByePositions = loadByePositionsFromJson(drawSize, nbSeeds, nbByes);

    // Create tournament with configuration
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setConfig(TournamentConfig.builder()
                                         .mainDrawSize(drawSize)
                                         .nbSeeds(nbSeeds)
                                         .build());

    // Create real pairs (first nbSeeds have seeds, rest are unseeded)
    for (int i = 1; i <= nbTeams; i++) {
      int seed = (i <= nbSeeds) ? i : 0;
      tournament.getPlayerPairs().add(new PlayerPair("P" + i + "-1", "P" + i + "-2", seed));
    }

    // Add BYEs at the end (simulating current DB order)
    for (int i = 0; i < nbByes; i++) {
      tournament.getPlayerPairs().add(PlayerPair.bye());
    }

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    // When: Get pairs with BYEs included
    List<PlayerPair> result = service.getPairsByTournamentId(1L, true, false);

    // Then: Verify structure
    assertEquals(drawSize, result.size(),
                 String.format("Should have %d positions total", drawSize));

    // Count BYEs
    long byeCount = result.stream().filter(PlayerPair::isBye).count();
    assertEquals(nbByes, byeCount,
                 String.format("Should have exactly %d BYEs", nbByes));

    // CRITICAL: Verify that BYEs are at the expected absolute positions
    List<Integer> actualByePositions = new ArrayList<>();
    for (int i = 0; i < result.size(); i++) {
      if (result.get(i).isBye()) {
        actualByePositions.add(i);
      }
    }

    // Sort both lists before comparing (order doesn't matter, only the positions)
    List<Integer> sortedExpected = new ArrayList<>(expectedByePositions);
    List<Integer> sortedActual   = new ArrayList<>(actualByePositions);
    sortedExpected.sort(Integer::compareTo);
    sortedActual.sort(Integer::compareTo);

    assertEquals(sortedExpected, sortedActual,
                 String.format("BYEs should be at positions %s but were at positions %s",
                               sortedExpected, sortedActual));

    // CRITICAL: Verify that teams at seed positions play against BYEs
    // Load seed positions from JSON to check that BYEs are properly placed opposite seeds
    List<Integer> seedPositions = loadSeedPositionsFromJson(drawSize, nbSeeds);

    // For each of the first min(nbSeeds, nbByes) seed positions, verify the opposite position has a BYE
    int expectedSeedsWithByes = Math.min(nbSeeds, nbByes);
    int actualSeedsWithByes   = 0;

    for (int i = 0; i < Math.min(expectedSeedsWithByes, seedPositions.size()); i++) {
      int seedPos     = seedPositions.get(i);
      int oppositePos = (seedPos % 2 == 0) ? seedPos + 1 : seedPos - 1;

      // Verify the opposite position has a BYE
      if (oppositePos < result.size() && result.get(oppositePos).isBye()) {
        actualSeedsWithByes++;
      } else {
        System.err.printf(
            "❌ Seed position %d (should have seed %d) does not have BYE at opposite position %d%n",
            seedPos, i + 1, oppositePos);
      }
    }

    assertEquals(expectedSeedsWithByes, actualSeedsWithByes,
                 String.format("All %d first seeds should have BYEs at opposite positions", expectedSeedsWithByes));

    // Verify that all real pairs are present
    long realPairsCount = result.stream().filter(p -> !p.isBye()).count();
    assertEquals(nbTeams, realPairsCount,
                 String.format("Should have all %d real pairs in result", nbTeams));
  }

  /**
   * Loads seed positions from the seed_positions.json file
   */
  private List<Integer> loadSeedPositionsFromJson(int drawSize, int nbSeeds) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      InputStream  is     = getClass().getClassLoader().getResourceAsStream("seed_positions.json");

      if (is == null) {
        throw new IllegalStateException("seed_positions.json not found in resources");
      }

      JsonNode root     = mapper.readTree(is);
      JsonNode drawNode = root.path(String.valueOf(drawSize));

      if (drawNode.isMissingNode()) {
        return new ArrayList<>();
      }

      // Reconstruct the flat list of seed positions in order
      List<Integer> positions = new ArrayList<>();

      // For each seed group in order
      String[] groupKeys = {"TS1", "TS2", "TS3-4", "TS5-8", "TS9-16", "TS17-32"};

      JsonNode seedsNode = drawNode.path(String.valueOf(nbSeeds));
      if (seedsNode.isMissingNode()) {
        return new ArrayList<>();
      }

      // Parse each group in the correct order
      for (String key : groupKeys) {
        JsonNode groupNode = seedsNode.path(key);
        if (!groupNode.isMissingNode() && groupNode.isArray()) {
          for (JsonNode pos : groupNode) {
            positions.add(pos.asInt());
          }
        }
      }

      return positions;

    } catch (Exception e) {
      throw new RuntimeException("Failed to load seed positions from JSON", e);
    }
  }

  /**
   * Loads BYE positions from the bye_positions.json file
   */
  private List<Integer> loadByePositionsFromJson(int drawSize, int nbSeeds, int nbByes) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      InputStream  is     = getClass().getClassLoader().getResourceAsStream("bye_positions.json");

      if (is == null) {
        throw new IllegalStateException("bye_positions.json not found in resources");
      }

      JsonNode root = mapper.readTree(is);
      JsonNode byePositionsNode = root.path(String.valueOf(drawSize))
                                      .path(String.valueOf(nbSeeds))
                                      .path(String.valueOf(nbByes));

      if (byePositionsNode.isMissingNode()) {
        throw new IllegalStateException(
            String.format("No BYE positions found for drawSize=%d, nbSeeds=%d, nbByes=%d",
                          drawSize, nbSeeds, nbByes));
      }

      List<Integer> positions = new ArrayList<>();
      for (JsonNode position : byePositionsNode) {
        positions.add(position.asInt());
      }

      return positions;

    } catch (Exception e) {
      throw new RuntimeException("Failed to load BYE positions from JSON", e);
    }
  }
}
