package io.github.redouanebali.model.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TournamentConfigTest {

  @ParameterizedTest
  @CsvSource({
      "KNOCKOUT, 32, 0, 4, 4, 0, 0, 0, 32",
      "KNOCKOUT, 16, 0, 0, 0, 0, 0, 0, 16",
      "GROUPS_KO, 0, 0, 4, 4, 0, 0, 0, 16",
      "GROUPS_KO, 0, 0, 8, 3, 0, 0, 0, 24",
      "GROUPS_KO, 0, 0, 2, 8, 0, 0, 0, 16",
      "QUALIF_KO, 32, 0, 0, 0, 0, 16, 4, 48",
      "QUALIF_KO, 16, 0, 0, 0, 0, 8, 2, 24"
  })
  void getNbMaxPairs_shouldCalculateCorrectly(
      TournamentFormat format,
      int mainDrawSize,
      int nbSeeds,
      int nbPoolsParam,
      int nbPairsPerPool,
      int nbQualifiedByPool,
      int preQualDrawSize,
      int nbQualifiers,
      int expected) {

    TournamentConfig config = TournamentConfig.builder()
                                              .format(format)
                                              .mainDrawSize(mainDrawSize)
                                              .nbSeeds(nbSeeds)
                                              .nbPools(nbPoolsParam)
                                              .nbPairsPerPool(nbPairsPerPool)
                                              .nbQualifiedByPool(nbQualifiedByPool)
                                              .preQualDrawSize(preQualDrawSize)
                                              .nbQualifiers(nbQualifiers)
                                              .build();

    assertEquals(expected, config.getNbMaxPairs());
  }

  @ParameterizedTest
  @CsvSource({
      "KNOCKOUT",
      "GROUPS_KO",
      "QUALIF_KO"
  })
  void builder_shouldUseDefaultValues(TournamentFormat format) {
    TournamentConfig config = TournamentConfig.builder()
                                              .format(format)
                                              .build();

    assertEquals(format, config.getFormat());
    assertEquals(DrawMode.MANUAL, config.getDrawMode());
    assertEquals(false, config.getStaggeredEntry());
  }
}

