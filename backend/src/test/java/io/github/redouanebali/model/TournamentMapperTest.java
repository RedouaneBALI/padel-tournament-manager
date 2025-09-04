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
import io.github.redouanebali.dto.response.PoolDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.SetScoreDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.util.TestFixtures;
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
    detail.setPlayerPair(new PlayerPairDTO("J1", "J2"));
    detail.setPoints(100);
    detail.setSetAverage(5);

    PoolRankingDTO dto = new PoolRankingDTO();
    dto.setId(1L);
    dto.setDetails(List.of(detail));

    assertEquals(1L, dto.getId());
    assertEquals(1, dto.getDetails().size());
    PoolRankingDetailsDTO d = dto.getDetails().get(0);
    assertEquals(10L, d.getPairId());
    assertEquals("J1", d.getPlayerPair().getPlayer1Name());
    assertEquals("J2", d.getPlayerPair().getPlayer2Name());
    assertEquals(100, d.getPoints());
    assertEquals(5, d.getSetAverage());
  }

  @Test
  void testPoolRankingDetailsDTO() {
    PoolRankingDetailsDTO dto = new PoolRankingDetailsDTO();
    dto.setPairId(20L);
    dto.setPlayerPair(new PlayerPairDTO("J1", "J2"));
    dto.setPoints(80);
    dto.setSetAverage(3);
    assertEquals(20L, dto.getPairId());
    assertEquals("J1", dto.getPlayerPair().getPlayer1Name());
    assertEquals("J2", dto.getPlayerPair().getPlayer2Name());
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

  @Test
  void testRoundPoolsMapping() {
    // Création des joueurs et des paires
    Player player1 = new Player("John");
    Player player2 = new Player("Alice");
    Player player3 = new Player("Bob");
    Player player4 = new Player("Carol");

    PlayerPair pair1 = new PlayerPair();
    pair1.setId(1L);
    pair1.setPlayer1(player1);
    pair1.setPlayer2(player2);

    PlayerPair pair2 = new PlayerPair();
    pair2.setId(2L);
    pair2.setPlayer1(player3);
    pair2.setPlayer2(player4);

    // Création du pool avec son nom et ses paires
    Pool pool = new Pool("Pool A", List.of(pair1, pair2));

    // Création du round avec son stage
    Round round = new Round(Stage.GROUPS);
    round.addPool(pool);

    // Mapping vers DTO
    RoundDTO roundDTO = mapper.toDTO(round);

    // Vérifications
    assertNotNull(roundDTO.getPools());
    assertEquals(1, roundDTO.getPools().size());

    PoolDTO poolDTO = roundDTO.getPools().get(0);
    assertEquals("Pool A", poolDTO.getName());
    assertNotNull(poolDTO.getPairs());
    assertEquals(2, poolDTO.getPairs().size());

    // Vérification du premier pair
    PlayerPairDTO firstPairDTO = poolDTO.getPairs().get(0);
    assertEquals("John", firstPairDTO.getPlayer1Name());
    assertEquals("Alice", firstPairDTO.getPlayer2Name());

    // Vérification du deuxième pair
    PlayerPairDTO secondPairDTO = poolDTO.getPairs().get(1);
    assertEquals("Bob", secondPairDTO.getPlayer1Name());
    assertEquals("Carol", secondPairDTO.getPlayer2Name());

    // Vérification du ranking
    assertNotNull(poolDTO.getPoolRanking());
    assertNotNull(poolDTO.getPoolRanking().getDetails());
    assertEquals(pool.getPoolRanking().getDetails().size(), poolDTO.getPoolRanking().getDetails().size());
  }

  @Test
  void testGameWinnerSideMapping() {
    Player     player1 = new Player("A");
    Player     player2 = new Player("B");
    PlayerPair pairA   = new PlayerPair();
    pairA.setPlayer1(player1);
    pairA.setPlayer2(player2);

    Player     player3 = new Player("C");
    Player     player4 = new Player("D");
    PlayerPair pairB   = new PlayerPair();
    pairB.setPlayer1(player3);
    pairB.setPlayer2(player4);

    Game game = new Game();
    game.setId(999L);
    game.setFormat(new MatchFormat());
    game.setTeamA(pairA);
    game.setTeamB(pairB);
    game.setScore(TestFixtures.createScoreWithWinner(game, pairA));
    game.setWinnerSide(TeamSide.TEAM_A);

    GameDTO dto = mapper.toDTO(game);

    assertNotNull(dto);
    assertEquals(999L, dto.getId());
    assertEquals(TeamSide.TEAM_A, dto.getWinnerSide());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithEmptyRounds() {
    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of());

    assertNull(dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithNullRounds() {
    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(null);

    assertNull(dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithUnfinishedGames() {
    // Création d'un jeu non terminé
    GameDTO unfinishedGame = new GameDTO();
    unfinishedGame.setId(1L);
    unfinishedGame.setFinished(false);
    unfinishedGame.setTeamA(new PlayerPairDTO("Player1", "Player2"));
    unfinishedGame.setTeamB(new PlayerPairDTO("Player3", "Player4"));

    // Création d'un round avec le jeu non terminé
    RoundDTO semisRound = new RoundDTO();
    semisRound.setId(1L);
    semisRound.setStage(Stage.SEMIS);
    semisRound.setGames(List.of(unfinishedGame));

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(semisRound));

    assertEquals(Stage.SEMIS, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithAllGamesFinished() {
    // Création d'un jeu terminé
    GameDTO finishedGame = new GameDTO();
    finishedGame.setId(1L);
    finishedGame.setFinished(true);
    finishedGame.setTeamA(new PlayerPairDTO("Player1", "Player2"));
    finishedGame.setTeamB(new PlayerPairDTO("Player3", "Player4"));

    // Création d'un round avec le jeu terminé
    RoundDTO quartersRound = new RoundDTO();
    quartersRound.setId(1L);
    quartersRound.setStage(Stage.QUARTERS);
    quartersRound.setGames(List.of(finishedGame));

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(quartersRound));

    // Tous les jeux sont terminés, donc retourne le dernier stage utilisé
    assertEquals(Stage.QUARTERS, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithMultipleRounds() {
    // Création de jeux pour différents rounds
    GameDTO finishedQuarterGame = new GameDTO();
    finishedQuarterGame.setId(1L);
    finishedQuarterGame.setFinished(true);
    finishedQuarterGame.setTeamA(new PlayerPairDTO("Player1", "Player2"));
    finishedQuarterGame.setTeamB(new PlayerPairDTO("Player3", "Player4"));

    GameDTO unfinishedSemiGame = new GameDTO();
    unfinishedSemiGame.setId(2L);
    unfinishedSemiGame.setFinished(false);
    unfinishedSemiGame.setTeamA(new PlayerPairDTO("Winner1", "Winner2"));
    unfinishedSemiGame.setTeamB(new PlayerPairDTO("Winner3", "Winner4"));

    // Création des rounds
    RoundDTO quartersRound = new RoundDTO();
    quartersRound.setId(1L);
    quartersRound.setStage(Stage.QUARTERS);
    quartersRound.setGames(List.of(finishedQuarterGame));

    RoundDTO semisRound = new RoundDTO();
    semisRound.setId(2L);
    semisRound.setStage(Stage.SEMIS);
    semisRound.setGames(List.of(unfinishedSemiGame));

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(quartersRound, semisRound));

    // Le premier round avec des jeux non terminés
    assertEquals(Stage.SEMIS, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithEmptyGames() {
    // Round sans jeux
    RoundDTO emptyRound = new RoundDTO();
    emptyRound.setId(1L);
    emptyRound.setStage(Stage.FINAL);
    emptyRound.setGames(List.of());

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(emptyRound));

    // Retourne le premier stage car aucun round utilisé
    assertEquals(Stage.FINAL, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_WithNullTeams() {
    // Jeu sans équipes assignées (placeholder)
    GameDTO placeholderGame = new GameDTO();
    placeholderGame.setId(1L);
    placeholderGame.setFinished(false);
    placeholderGame.setTeamA(null);
    placeholderGame.setTeamB(null);

    RoundDTO placeholderRound = new RoundDTO();
    placeholderRound.setId(1L);
    placeholderRound.setStage(Stage.R16);
    placeholderRound.setGames(List.of(placeholderGame));

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(placeholderRound));

    // Retourne le premier stage car pas d'équipes assignées
    assertEquals(Stage.R16, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOGetCurrentRoundStage_ComplexScenario() {
    // Scénario complexe avec plusieurs rounds et états différents

    // Q1 avec jeux terminés
    GameDTO finishedQ1Game = new GameDTO();
    finishedQ1Game.setId(1L);
    finishedQ1Game.setFinished(true);
    finishedQ1Game.setTeamA(new PlayerPairDTO("Q1Team1", "Q1Team2"));
    finishedQ1Game.setTeamB(new PlayerPairDTO("Q1Team3", "Q1Team4"));

    RoundDTO q1Round = new RoundDTO();
    q1Round.setId(1L);
    q1Round.setStage(Stage.Q1);
    q1Round.setGames(List.of(finishedQ1Game));

    // Quarters avec un jeu non terminé
    GameDTO unfinishedQuarterGame = new GameDTO();
    unfinishedQuarterGame.setId(2L);
    unfinishedQuarterGame.setFinished(false);
    unfinishedQuarterGame.setTeamA(new PlayerPairDTO("Team1", "Team2"));
    unfinishedQuarterGame.setTeamB(new PlayerPairDTO("Team3", "Team4"));

    RoundDTO quartersRound = new RoundDTO();
    quartersRound.setId(2L);
    quartersRound.setStage(Stage.QUARTERS);
    quartersRound.setGames(List.of(unfinishedQuarterGame));

    // Final avec placeholder
    GameDTO placeholderFinalGame = new GameDTO();
    placeholderFinalGame.setId(3L);
    placeholderFinalGame.setFinished(false);
    placeholderFinalGame.setTeamA(null);
    placeholderFinalGame.setTeamB(null);

    RoundDTO finalRound = new RoundDTO();
    finalRound.setId(3L);
    finalRound.setStage(Stage.FINAL);
    finalRound.setGames(List.of(placeholderFinalGame));

    TournamentDTO dto = new TournamentDTO();
    dto.setRounds(List.of(q1Round, quartersRound, finalRound));

    // Doit retourner QUARTERS car c'est le premier round avec des jeux non terminés
    assertEquals(Stage.QUARTERS, dto.getCurrentRoundStage());
  }

  @Test
  void testTournamentDTOCompleteMapping() {
    // Test complet du mapping TournamentDTO avec tous les champs
    PlayerPairDTO pair1 = new PlayerPairDTO();
    pair1.setId(1L);
    pair1.setPlayer1Name("Alice");
    pair1.setPlayer2Name("Bob");
    pair1.setSeed(1);
    pair1.setBye(false);

    PlayerPairDTO pair2 = new PlayerPairDTO();
    pair2.setId(2L);
    pair2.setPlayer1Name("Charlie");
    pair2.setPlayer2Name("Diana");
    pair2.setSeed(2);
    pair2.setBye(false);

    GameDTO game = new GameDTO();
    game.setId(1L);
    game.setFinished(false);
    game.setTeamA(pair1);
    game.setTeamB(pair2);

    RoundDTO round = new RoundDTO();
    round.setId(1L);
    round.setStage(Stage.FINAL);
    round.setGames(List.of(game));

    TournamentDTO dto = new TournamentDTO();
    dto.setId(100L);
    dto.setOwnerId("owner@test.com");
    dto.setName("Test Championship");
    dto.setDescription("A test tournament");
    dto.setCity("Paris");
    dto.setClub("Test Club");
    dto.setGender(Gender.MIX);
    dto.setLevel(TournamentLevel.P100);
    dto.setFormat(io.github.redouanebali.model.format.TournamentFormat.KNOCKOUT);
    dto.setStartDate(LocalDate.of(2025, 12, 1));
    dto.setEndDate(LocalDate.of(2025, 12, 5));
    dto.setEditable(true);
    dto.setRounds(List.of(round));
    dto.setPlayerPairs(List.of(pair1, pair2));

    // Vérifications des champs
    assertEquals(100L, dto.getId());
    assertEquals("owner@test.com", dto.getOwnerId());
    assertEquals("Test Championship", dto.getName());
    assertEquals("A test tournament", dto.getDescription());
    assertEquals("Paris", dto.getCity());
    assertEquals("Test Club", dto.getClub());
    assertEquals(Gender.MIX, dto.getGender());
    assertEquals(TournamentLevel.P100, dto.getLevel());
    assertEquals(io.github.redouanebali.model.format.TournamentFormat.KNOCKOUT, dto.getFormat());
    assertEquals(LocalDate.of(2025, 12, 1), dto.getStartDate());
    assertEquals(LocalDate.of(2025, 12, 5), dto.getEndDate());
    assertTrue(dto.isEditable());

    // Vérifications des collections
    assertNotNull(dto.getRounds());
    assertEquals(1, dto.getRounds().size());
    assertEquals(Stage.FINAL, dto.getRounds().get(0).getStage());

    assertNotNull(dto.getPlayerPairs());
    assertEquals(2, dto.getPlayerPairs().size());
    assertEquals("Alice", dto.getPlayerPairs().get(0).getPlayer1Name());
    assertEquals("Charlie", dto.getPlayerPairs().get(1).getPlayer1Name());

    // Test de la méthode getCurrentRoundStage
    assertEquals(Stage.FINAL, dto.getCurrentRoundStage());
  }
}
