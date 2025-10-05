package io.github.redouanebali.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class TournamentMapperTest {

  private final TournamentMapper mapper = Mappers.getMapper(TournamentMapper.class);

  @Test
  void testTournamentToDTO_mapping() {
    Tournament t = new Tournament();
    t.setId(1L);
    t.setOwnerId("owner@test.com");
    t.setName("Padel Cup");
    t.setDescription("Test tournament");
    t.setCity("Casa");
    t.setClub("Padel Club");
    t.setGender(Gender.MIX);
    t.setLevel(TournamentLevel.P100);
    t.setStartDate(LocalDate.of(2025, 10, 1));
    t.setEndDate(LocalDate.of(2025, 10, 5));
    TournamentConfig config = TournamentConfig.builder()
                                              .format(TournamentFormat.KNOCKOUT)
                                              .mainDrawSize(8)
                                              .nbSeeds(4)
                                              .build();
    t.setConfig(config);
    // Ajout d'une paire et d'un round pour tester la collection
    PlayerPair pair = new PlayerPair();
    pair.setId(2L);
    Player p1 = new Player("Alice");
    Player p2 = new Player("Bob");
    pair.setPlayer1(p1);
    pair.setPlayer2(p2);
    t.getPlayerPairs().add(pair);
    Round round = new Round(Stage.FINAL);
    t.getRounds().add(round);

    TournamentDTO dto = mapper.toDTO(t);
    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals("owner@test.com", dto.getOwnerId());
    assertEquals("Padel Cup", dto.getName());
    assertEquals("Test tournament", dto.getDescription());
    assertEquals("Casa", dto.getCity());
    assertEquals("Padel Club", dto.getClub());
    assertEquals(Gender.MIX, dto.getGender());
    assertEquals(TournamentLevel.P100, dto.getLevel());
    assertEquals(LocalDate.of(2025, 10, 1), dto.getStartDate());
    assertEquals(LocalDate.of(2025, 10, 5), dto.getEndDate());
    assertNotNull(dto.getConfig());
    assertEquals(TournamentFormat.KNOCKOUT, dto.getConfig().getFormat());
    assertEquals(8, dto.getConfig().getMainDrawSize());
    assertEquals(4, dto.getConfig().getNbSeeds());
    assertNotNull(dto.getPlayerPairs());
    assertEquals(1, dto.getPlayerPairs().size());
    assertEquals("Alice", dto.getPlayerPairs().get(0).getPlayer1Name());
    assertEquals("Bob", dto.getPlayerPairs().get(0).getPlayer2Name());
    assertNotNull(dto.getRounds());
    assertEquals(1, dto.getRounds().size());
    assertEquals(Stage.FINAL, dto.getRounds().get(0).getStage());
  }

  @Test
  void testPlayerPairToDTO_mapping() {
    PlayerPair pair = new PlayerPair();
    pair.setId(10L);
    Player p1 = new Player("Alice");
    Player p2 = new Player("Bob");
    pair.setPlayer1(p1);
    pair.setPlayer2(p2);
    pair.setSeed(2);
    PlayerPairDTO dto = mapper.toDTO(pair);
    assertNotNull(dto);
    assertEquals(10L, dto.getId());
    assertEquals("Alice", dto.getPlayer1Name());
    assertEquals("Bob", dto.getPlayer2Name());
    assertEquals(2, dto.getSeed());
    assertFalse(dto.isBye());
  }

  @Test
  void testPoolRankingDetailsToDTO_mapping() {
    PlayerPair pair = new PlayerPair();
    pair.setId(42L);
    pair.setPlayer1(new Player("A"));
    pair.setPlayer2(new Player("B"));
    PoolRankingDetails details = new PoolRankingDetails();
    details.setId(99L);
    details.setPlayerPair(pair);
    details.setPoints(7);
    details.setSetAverage(3);
    PoolRankingDetailsDTO dto = mapper.toDTO(details);
    assertNotNull(dto);
    assertEquals(42L, dto.getPairId());
    assertEquals("A", dto.getPlayerPair().getPlayer1Name());
    assertEquals("B", dto.getPlayerPair().getPlayer2Name());
    assertEquals(7, dto.getPoints());
    assertEquals(3, dto.getSetAverage());
  }

  @Test
  void testGameToDTO_mapping() {
    PlayerPair teamA = new PlayerPair();
    teamA.setId(1L);
    teamA.setPlayer1(new Player("A"));
    teamA.setPlayer2(new Player("B"));
    PlayerPair teamB = new PlayerPair();
    teamB.setId(2L);
    teamB.setPlayer1(new Player("C"));
    teamB.setPlayer2(new Player("D"));
    Game game = new Game();
    game.setId(100L);
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setWinnerSide(TeamSide.TEAM_A);
    GameDTO dto = mapper.toDTO(game);
    assertNotNull(dto);
    assertEquals(100L, dto.getId());
    assertEquals("A", dto.getTeamA().getPlayer1Name());
    assertEquals("C", dto.getTeamB().getPlayer1Name());
    assertEquals(TeamSide.TEAM_A, dto.getWinnerSide());
  }

  @Test
  void testRoundToDTO_mapping() {
    Round round = new Round(Stage.QUARTERS);
    round.setId(5L);
    Game game = new Game();
    game.setId(10L);
    round.getGames().add(game);
    RoundDTO dto = mapper.toDTO(round);
    assertNotNull(dto);
    assertEquals(5L, dto.getId());
    assertEquals(Stage.QUARTERS, dto.getStage());
    assertNotNull(dto.getGames());
    assertEquals(1, dto.getGames().size());
    assertEquals(10L, dto.getGames().get(0).getId());
  }

  @Test
  void testMatchFormatToDTO_mapping() {
    MatchFormat mf = new MatchFormat();
    mf.setNumberOfSetsToWin(3);
    mf.setGamesPerSet(5);
    mf.setAdvantage(true);
    mf.setSuperTieBreakInFinalSet(false);
    MatchFormatDTO dto = mapper.toDTO(mf);
    assertNotNull(dto);
    assertEquals(3, dto.getNumberOfSetsToWin());
    assertEquals(5, dto.getGamesPerSet());
    assertTrue(dto.isAdvantage());
    assertFalse(dto.isSuperTieBreakInFinalSet());
  }

  @Test
  void testToPlayerPairList_mapping() {
    CreatePlayerPairRequest req = new CreatePlayerPairRequest();
    req.setPlayer1Name("A");
    req.setPlayer2Name("B");
    req.setSeed(1);
    List<PlayerPair> pairs = mapper.toPlayerPairList(List.of(req));
    assertEquals(1, pairs.size());
    Assertions.assertEquals("A", pairs.get(0).getPlayer1().getName());
    Assertions.assertEquals("B", pairs.get(0).getPlayer2().getName());
    Assertions.assertEquals(1, pairs.get(0).getSeed());
  }

  @Test
  void testTournamentDTO_getCurrentRoundStage() {
    TournamentDTO dto   = new TournamentDTO();
    RoundDTO      round = new RoundDTO();
    round.setStage(Stage.FINAL);
    dto.setRounds(List.of(round));
    assertEquals(Stage.FINAL, dto.getCurrentRoundStage());
  }
}
