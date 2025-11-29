package io.github.redouanebali.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Stage;
import org.junit.jupiter.api.Test;

class RoundLightDTOTest {

  @Test
  void testAllArgsConstructorAndGetters() {
    MatchFormatDTO matchFormat = new MatchFormatDTO();
    matchFormat.setNumberOfSetsToWin(2);
    matchFormat.setGamesPerSet(6);
    matchFormat.setAdvantage(true);
    matchFormat.setSuperTieBreakInFinalSet(false);
    matchFormat.setTieBreakAt(6);

    RoundLightDTO dto = new RoundLightDTO(1L, Stage.R32, matchFormat);
    assertEquals(1L, dto.getId());
    assertEquals(Stage.R32, dto.getStage());
    assertEquals(matchFormat, dto.getMatchFormat());
  }

  @Test
  void testNoArgsConstructorAndSetters() {
    RoundLightDTO  dto         = new RoundLightDTO();
    MatchFormatDTO matchFormat = new MatchFormatDTO();
    dto.setId(2L);
    dto.setStage(Stage.R16);
    dto.setMatchFormat(matchFormat);
    assertEquals(2L, dto.getId());
    assertEquals(Stage.R16, dto.getStage());
    assertEquals(matchFormat, dto.getMatchFormat());
  }
}

