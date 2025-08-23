package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.SetScoreDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class TournamentMapperTest {

  private final TournamentMapper mapper = Mappers.getMapper(TournamentMapper.class);

  @Test
  void testTournamentMapperAvailable() {
    assertNotNull(mapper, "TournamentMapper should be available from Mappers.getMapper");
  }

  @Test
  void testGameDTO() {
    GameDTO dto = new GameDTO();
    dto.setId(1L);
    dto.setFinished(true);
    assertEquals(1L, dto.getId());
    assertTrue(dto.isFinished());
  }

  @Test
  void testMatchFormatDTO() {
    MatchFormatDTO dto = new MatchFormatDTO();
    dto.setNumberOfSetsToWin(2);
    dto.setGamesPerSet(6);
    assertEquals(2, dto.getNumberOfSetsToWin());
    assertEquals(6, dto.getGamesPerSet());
  }

  @Test
  void testPlayerPairDTO() {
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(5L);
    dto.setPlayer1Name("Alice");
    dto.setPlayer2Name("Bob");
    dto.setSeed(1);
    dto.setBye(false);
    assertEquals("Alice", dto.getPlayer1Name());
    assertEquals("Bob", dto.getPlayer2Name());
    assertEquals(1, dto.getSeed());
    assertFalse(dto.isBye());
  }

  @Test
  void testPoolRankingDTO() {
    PoolRankingDetailsDTO detail = new PoolRankingDetailsDTO();
    detail.setPairId(10L);
    detail.setPairName("Team X");
    detail.setPoints(100);
    detail.setSetAverage(5);

    PoolRankingDTO dto = new PoolRankingDTO();
    dto.setId(1L);
    dto.setDetails(List.of(detail));

    assertEquals(1L, dto.getId());
    assertEquals(1, dto.getDetails().size());
    PoolRankingDetailsDTO d = dto.getDetails().get(0);
    assertEquals(10L, d.getPairId());
    assertEquals("Team X", d.getPairName());
    assertEquals(100, d.getPoints());
    assertEquals(5, d.getSetAverage());
  }

  @Test
  void testPoolRankingDetailsDTO() {
    PoolRankingDetailsDTO dto = new PoolRankingDetailsDTO();
    dto.setPairId(20L);
    dto.setPairName("Team Y");
    dto.setPoints(80);
    dto.setSetAverage(3);
    assertEquals(20L, dto.getPairId());
    assertEquals("Team Y", dto.getPairName());
    assertEquals(80, dto.getPoints());
    assertEquals(3, dto.getSetAverage());
  }

  @Test
  void testRoundDTO() {
    RoundDTO dto = new RoundDTO();
    dto.setId(7L);
    dto.setStage(Stage.R32);
    assertEquals(7L, dto.getId());
    assertEquals(Stage.R32, dto.getStage());
  }

  @Test
  void testScoreDTOAndSetScoreDTO() {
    SetScoreDTO set = new SetScoreDTO();
    set.setTeamAScore(6);
    set.setTeamBScore(4);
    set.setTieBreakTeamA(7);
    set.setTieBreakTeamB(5);
    assertEquals(6, set.getTeamAScore());
    assertEquals(4, set.getTeamBScore());
    assertEquals(7, set.getTieBreakTeamA());
    assertEquals(5, set.getTieBreakTeamB());

    ScoreDTO score = new ScoreDTO();
    score.setSets(List.of(set));
    assertEquals(1, score.getSets().size());
  }


  @Test
  void testTournamentDTO() {
    TournamentDTO dto = new TournamentDTO();
    dto.setId(1L);
    dto.setOwnerId("owner@test.com");
    dto.setName("Padel Cup");
    dto.setCity("Casablanca");
    dto.setStartDate(LocalDate.of(2025, 10, 1));
    dto.setEndDate(LocalDate.of(2025, 10, 5));
    dto.setEditable(true);

    assertEquals(1L, dto.getId());
    assertEquals("owner@test.com", dto.getOwnerId());
    assertEquals("Padel Cup", dto.getName());
    assertEquals("Casablanca", dto.getCity());
    assertTrue(dto.isEditable());
  }

  @Test
  void testScoreDTOWithMultipleSets() {
    SetScoreDTO set1 = new SetScoreDTO();
    set1.setTeamAScore(6);
    set1.setTeamBScore(3);

    SetScoreDTO set2 = new SetScoreDTO();
    set2.setTeamAScore(7);
    set2.setTeamBScore(6);
    set2.setTieBreakTeamA(10);
    set2.setTieBreakTeamB(8);

    ScoreDTO score = new ScoreDTO();
    score.setSets(List.of(set1, set2));

    assertEquals(2, score.getSets().size());
    assertEquals(10, score.getSets().get(1).getTieBreakTeamA());
  }

  @Test
  void testRoundDTOWithGames() {
    GameDTO game = new GameDTO();
    game.setId(100L);
    game.setFinished(false);

    RoundDTO round = new RoundDTO();
    round.setId(55L);
    round.setStage(Stage.SEMIS);
    round.setGames(List.of(game));

    assertEquals(55L, round.getId());
    assertEquals(Stage.SEMIS, round.getStage());
    assertEquals(1, round.getGames().size());
    assertEquals(100L, round.getGames().get(0).getId());
  }

  @Test
  void testCreatePlayerPairRequestToPlayerPair() {
    CreatePlayerPairRequest request = new CreatePlayerPairRequest();
    request.setPlayer1Name("aa");
    request.setPlayer2Name("bb");
    request.setSeed(1);

    List<PlayerPair> pairs = mapper.toPlayerPairList(List.of(request));

    assertEquals(1, pairs.size());
    PlayerPair pair = pairs.get(0);
    assertNotNull(pair);
    assertEquals(1, pair.getSeed());
    assertNotNull(pair.getPlayer1());
    assertNotNull(pair.getPlayer2());
    assertEquals("aa", pair.getPlayer1().getName());
    assertEquals("bb", pair.getPlayer2().getName());
  }

  @Test
  void testMatchFormatDTOFull() {
    MatchFormatDTO dto = new MatchFormatDTO();
    dto.setNumberOfSetsToWin(3);
    dto.setGamesPerSet(5);
    dto.setAdvantage(true);
    dto.setSuperTieBreakInFinalSet(false);

    assertEquals(3, dto.getNumberOfSetsToWin());
    assertEquals(5, dto.getGamesPerSet());
    assertTrue(dto.isAdvantage());
    assertFalse(dto.isSuperTieBreakInFinalSet());
  }

  @Test
  void testPlayerPairDTOBye() {
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(9L);
    dto.setPlayer1Name("Ghost");
    dto.setPlayer2Name(null);
    dto.setSeed(2);
    dto.setBye(true);

    assertEquals(9L, dto.getId());
    assertEquals("Ghost", dto.getPlayer1Name());
    assertNull(dto.getPlayer2Name());
    assertTrue(dto.isBye());
  }

  @Test
  void testTournamentDTOWithNested() {
    GameDTO game = new GameDTO();
    game.setId(200L);
    game.setFinished(true);

    RoundDTO round = new RoundDTO();
    round.setId(20L);
    round.setStage(Stage.FINAL);
    round.setGames(List.of(game));

    PlayerPairDTO pair = new PlayerPairDTO();
    pair.setId(30L);
    pair.setPlayer1Name("Alice");
    pair.setPlayer2Name("Bob");

    TournamentDTO dto = new TournamentDTO();
    dto.setId(2L);
    dto.setName("World Cup");
    dto.setRounds(List.of(round));
    dto.setPlayerPairs(List.of(pair));

    assertEquals(2L, dto.getId());
    assertEquals("World Cup", dto.getName());
    assertEquals(1, dto.getRounds().size());
    assertEquals(1, dto.getPlayerPairs().size());
    assertEquals(Stage.FINAL, dto.getRounds().get(0).getStage());
    assertEquals("Alice", dto.getPlayerPairs().get(0).getPlayer1Name());
  }


  @Test
  void testGameDTOFull() {
    PlayerPairDTO pairA = new PlayerPairDTO();
    pairA.setPlayer1Name("P1");
    pairA.setPlayer2Name("P2");

    PlayerPairDTO pairB = new PlayerPairDTO();
    pairB.setPlayer1Name("P3");
    pairB.setPlayer2Name("P4");

    SetScoreDTO set = new SetScoreDTO();
    set.setTeamAScore(6);
    set.setTeamBScore(3);

    ScoreDTO score = new ScoreDTO();
    score.setSets(List.of(set));

    GameDTO game = new GameDTO();
    game.setId(321L);
    game.setFinished(true);
    game.setTeamA(pairA);
    game.setTeamB(pairB);
    game.setScore(score);

    assertEquals(321L, game.getId());
    assertTrue(game.isFinished());
    assertEquals("P1", game.getTeamA().getPlayer1Name());
    assertEquals("P3", game.getTeamB().getPlayer1Name());
    assertEquals(6, game.getScore().getSets().get(0).getTeamAScore());
  }

  @Test
  void testMapperWithEmptyList() {
    List<PlayerPair> pairs = mapper.toPlayerPairList(List.of());
    assertNotNull(pairs);
    assertTrue(pairs.isEmpty());
  }

}
