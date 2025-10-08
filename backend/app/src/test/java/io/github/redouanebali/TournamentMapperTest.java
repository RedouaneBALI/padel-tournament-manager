package io.github.redouanebali;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.PoolRankingDetails;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class TournamentMapperTest {

  private final TournamentMapper mapper       = Mappers.getMapper(TournamentMapper.class);
  private final ObjectMapper     objectMapper = new ObjectMapper();

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
    // Added a pair and a round to test the collection
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
  void testGameToDTO_mapping() throws Exception {
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
    game.setCourt("Court1");
    LocalTime now = LocalTime.of(13, 0);
    game.setScheduledTime(now);
    GameDTO dto = mapper.toDTO(game);
    assertNotNull(dto);
    assertEquals(100L, dto.getId());
    assertEquals("A", dto.getTeamA().getPlayer1Name());
    assertEquals("C", dto.getTeamB().getPlayer1Name());
    assertEquals("Court1", dto.getCourt());
    assertEquals(now, dto.getScheduledTime());
    assertEquals(TeamSide.TEAM_A, dto.getWinnerSide());
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    String json = objectMapper.writeValueAsString(dto);
    assertTrue(json.contains("\"scheduledTime\":\"13:00\""),
               "Le champ scheduledTime doit être au format HH:mm dans le JSON : " + json);
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

  // ========================================
  // Tests for mapping the type field
  // ========================================

  @Test
  void testPlayerPairToDTO_withNormalType() {
    // Given: A normal player pair
    PlayerPair pair = new PlayerPair();
    pair.setId(1L);
    pair.setPlayer1(new Player("Alice"));
    pair.setPlayer2(new Player("Bob"));
    pair.setSeed(3);
    pair.setType(PairType.NORMAL);

    // When: Mapping to DTO
    PlayerPairDTO dto = mapper.toDTO(pair);

    // Then: Type should be mapped
    assertNotNull(dto);
    assertEquals(PairType.NORMAL, dto.getType());
    assertEquals("Alice", dto.getPlayer1Name());
    assertEquals("Bob", dto.getPlayer2Name());
    assertEquals(3, dto.getSeed());
    assertFalse(dto.isBye());
    assertFalse(dto.isQualifierSlot());
  }

  @Test
  void testPlayerPairToDTO_withByeType() {
    // Given: A BYE pair
    PlayerPair pair = PlayerPair.bye();
    pair.setId(2L);

    // When: Mapping to DTO
    PlayerPairDTO dto = mapper.toDTO(pair);

    // Then: Type should be BYE
    assertNotNull(dto);
    assertEquals(PairType.BYE, dto.getType());
    assertEquals("BYE", dto.getPlayer1Name());
    assertEquals("BYE", dto.getPlayer2Name());
    assertTrue(dto.isBye());
    assertNull(dto.getSeed()); // BYE teams don't have a visible seed
    assertNull(dto.getDisplaySeed());
  }

  @Test
  void testPlayerPairToDTO_withQualifierType() {
    // Given: A QUALIFIER pair
    PlayerPair pair = PlayerPair.qualifier();
    pair.setId(3L);

    // When: Mapping to DTO
    PlayerPairDTO dto = mapper.toDTO(pair);

    // Then: Type should be QUALIFIER with NO player names
    assertNotNull(dto);
    assertEquals(PairType.QUALIFIER, dto.getType());
    assertNull(dto.getPlayer1Name()); // Important: no names for qualifiers
    assertNull(dto.getPlayer2Name()); // Important: no names for qualifiers
    assertTrue(dto.isQualifierSlot());
    assertNull(dto.getSeed()); // Qualifiers don't have a visible seed
    assertEquals("Q", dto.getDisplaySeed());
  }

  @Test
  void testPlayerPairDTO_jsonSerialization_normalType() throws JsonProcessingException {
    // Given: A DTO with NORMAL type
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(1L);
    dto.setPlayer1Name("Alice");
    dto.setPlayer2Name("Bob");
    dto.setSeed(3);
    dto.setType(PairType.NORMAL);
    dto.setDisplaySeed("3");

    // When: Serialization to JSON
    String json = objectMapper.writeValueAsString(dto);

    // Then: "type" field should NOT appear (NORMAL is filtered)
    assertFalse(json.contains("\"type\""),
                "Le champ 'type' ne devrait pas apparaître pour PairType.NORMAL");
    assertTrue(json.contains("\"player1Name\":\"Alice\""));
    assertTrue(json.contains("\"seed\":3"));
  }

  @Test
  void testPlayerPairDTO_jsonSerialization_byeType() throws JsonProcessingException {
    // Given: A DTO with BYE type
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(2L);
    dto.setPlayer1Name("BYE");
    dto.setPlayer2Name("BYE");
    dto.setBye(true);
    dto.setType(PairType.BYE);

    // When: Serialization to JSON
    String json = objectMapper.writeValueAsString(dto);

    // Then: "type" field MUST appear with value "BYE"
    assertTrue(json.contains("\"type\":\"BYE\""),
               "Le champ 'type' devrait apparaître avec la valeur 'BYE'");
    assertTrue(json.contains("\"bye\":true"));
  }

  @Test
  void testPlayerPairDTO_jsonSerialization_qualifierType() throws JsonProcessingException {
    // Given: A DTO with QUALIFIER type
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(3L);
    dto.setPlayer1Name("Q");
    dto.setPlayer2Name("Q");
    dto.setQualifierSlot(true);
    dto.setType(PairType.QUALIFIER);
    dto.setDisplaySeed("Q");

    // When: Serialization to JSON
    String json = objectMapper.writeValueAsString(dto);

    // Then: "type" field MUST appear with value "QUALIFIER"
    assertTrue(json.contains("\"type\":\"QUALIFIER\""),
               "Le champ 'type' devrait apparaître avec la valeur 'QUALIFIER'");
    assertTrue(json.contains("\"qualifierSlot\":true"));
    assertTrue(json.contains("\"displaySeed\":\"Q\""));
  }

  @Test
  void testGameDTO_withQualifierTeams() {
    // Given: A game with a qualifier team
    PlayerPair normalTeam = new PlayerPair();
    normalTeam.setId(1L);
    normalTeam.setPlayer1(new Player("Alice"));
    normalTeam.setPlayer2(new Player("Bob"));
    normalTeam.setSeed(1);
    normalTeam.setType(PairType.NORMAL);

    PlayerPair qualifierTeam = PlayerPair.qualifier();
    qualifierTeam.setId(2L);

    Game game = new Game();
    game.setId(100L);
    game.setTeamA(normalTeam);
    game.setTeamB(qualifierTeam);

    // When: Mapping to DTO
    GameDTO dto = mapper.toDTO(game);

    // Then: Both teams should be mapped with their type
    assertNotNull(dto);
    assertEquals(100L, dto.getId());

    PlayerPairDTO teamADto = dto.getTeamA();
    assertNotNull(teamADto);
    assertEquals(PairType.NORMAL, teamADto.getType());
    assertEquals("Alice", teamADto.getPlayer1Name());

    PlayerPairDTO teamBDto = dto.getTeamB();
    assertNotNull(teamBDto);
    assertEquals(PairType.QUALIFIER, teamBDto.getType());
    assertNull(teamBDto.getPlayer1Name()); // Important: no names for qualifiers
    assertNull(teamBDto.getPlayer2Name()); // Important: no names for qualifiers
    assertTrue(teamBDto.isQualifierSlot());
    assertEquals("Q", teamBDto.getDisplaySeed());
  }

  @Test
  void testPlayerPairDTO_jsonSerialization_nullType() throws JsonProcessingException {
    // Given: A DTO with null type
    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(1L);
    dto.setPlayer1Name("Alice");
    dto.setPlayer2Name("Bob");
    dto.setType(null);

    // When: Serialization to JSON
    String json = objectMapper.writeValueAsString(dto);

    // Then: "type" field should NOT appear (null is filtered)
    assertFalse(json.contains("\"type\""),
                "Le champ 'type' ne devrait pas apparaître quand il est null");
  }

  @Test
  void testPlayerPairDTO_displaySeed_forDifferentTypes() {
    // Test displaySeed for NORMAL
    PlayerPair normalPair = new PlayerPair();
    normalPair.setSeed(5);
    normalPair.setType(PairType.NORMAL);
    normalPair.setPlayer1(new Player("A"));
    normalPair.setPlayer2(new Player("B"));
    PlayerPairDTO normalDto = mapper.toDTO(normalPair);
    assertEquals("5", normalDto.getDisplaySeed());

    // Test displaySeed for BYE
    PlayerPair    byePair = PlayerPair.bye();
    PlayerPairDTO byeDto  = mapper.toDTO(byePair);
    assertNull(byeDto.getDisplaySeed());

    // Test displaySeed for QUALIFIER
    PlayerPair    qualifierPair = PlayerPair.qualifier();
    PlayerPairDTO qualifierDto  = mapper.toDTO(qualifierPair);
    assertEquals("Q", qualifierDto.getDisplaySeed());
  }
}
