package io.github.redouanebali.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.RoundLightDTO;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import org.junit.jupiter.api.Test;

class TournamentMapperLightRoundTest {

  private final TournamentMapper mapper = new TournamentMapperImpl();

  @Test
  void testToDTOWithRoundLight() {
    MatchFormat matchFormat = new MatchFormat();
    matchFormat.setNumberOfSetsToWin(2);
    matchFormat.setGamesPerSet(6);
    matchFormat.setAdvantage(true);
    matchFormat.setSuperTieBreakInFinalSet(false);
    matchFormat.setTieBreakAt(6);

    Round round = new Round();
    round.setId(10L);
    round.setStage(Stage.FINAL);
    round.setMatchFormat(matchFormat);

    var game = new io.github.redouanebali.model.Game();
    game.setId(99L);

    var dto = mapper.toDTOWithRound(game, round);
    assertNotNull(dto);
    assertEquals(99L, dto.getId());
    RoundLightDTO light = dto.getRound();
    assertNotNull(light);
    assertEquals(10L, light.getId());
    assertEquals(Stage.FINAL, light.getStage());
    MatchFormatDTO mf = light.getMatchFormat();
    assertNotNull(mf);
    assertEquals(2, mf.getNumberOfSetsToWin());
    assertEquals(6, mf.getGamesPerSet());
    assertTrue(mf.isAdvantage());
    assertFalse(mf.isSuperTieBreakInFinalSet());
    assertEquals(6, mf.getTieBreakAt());
  }
}

