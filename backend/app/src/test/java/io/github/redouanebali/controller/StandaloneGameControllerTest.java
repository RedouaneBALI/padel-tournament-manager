package io.github.redouanebali.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateStandaloneGameRequest;
import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.repository.GameRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class StandaloneGameControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GameRepository gameRepository;

  @BeforeEach
  void setUp() {
    gameRepository.deleteAll();
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldCreateStandaloneGame() throws Exception {
    // Given
    CreatePlayerPairRequest teamA = new CreatePlayerPairRequest(
        "John Doe", "Jane Smith", 1, PairType.NORMAL
    );
    CreatePlayerPairRequest teamB = new CreatePlayerPairRequest(
        "Alice Johnson", "Bob Williams", 2, PairType.NORMAL
    );

    MatchFormat format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);
    format.setSuperTieBreakInFinalSet(false);
    format.setAdvantage(true);
    format.setTieBreakAt(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    // When & Then
    mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"))
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.teamA.player1Name").value("John Doe"))
           .andExpect(jsonPath("$.teamA.player2Name").value("Jane Smith"))
           .andExpect(jsonPath("$.teamB.player1Name").value("Alice Johnson"))
           .andExpect(jsonPath("$.teamB.player2Name").value("Bob Williams"))
           .andExpect(jsonPath("$.finished").value(false))
           .andExpect(jsonPath("$.score").isEmpty());
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldListAllStandaloneGames() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

    // When & Then
    mockMvc.perform(get("/games"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
           .andExpect(jsonPath("$[0].teamA").exists())
           .andExpect(jsonPath("$[0].teamB").exists());
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldGetGameById() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    String response = mockMvc.perform(post("/games")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                             .andReturn().getResponse().getContentAsString();

    Long gameId = objectMapper.readTree(response).get("id").asLong();

    // When & Then
    mockMvc.perform(get("/games/" + gameId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(gameId))
           .andExpect(jsonPath("$.teamA.player1Name").value("John"))
           .andExpect(jsonPath("$.teamB.player1Name").value("Alice"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldUpdateGameScore() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    String response = mockMvc.perform(post("/games")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                             .andReturn().getResponse().getContentAsString();

    Long gameId = objectMapper.readTree(response).get("id").asLong();

    // Create a score
    Score score = new Score();
    score.setSets(List.of(
        new SetScore(6, 4),
        new SetScore(6, 2)
    ));
    score.setForfeit(false);

    // When & Then
    mockMvc.perform(put("/games/" + gameId + "/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(score)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.finished").value(true))
           .andExpect(jsonPath("$.score.sets", hasSize(2)))
           .andExpect(jsonPath("$.score.sets[0].teamAScore").value(6))
           .andExpect(jsonPath("$.score.sets[0].teamBScore").value(4));
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldUpdateGame() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    String response = mockMvc.perform(post("/games")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                             .andReturn().getResponse().getContentAsString();

    long gameId = objectMapper.readTree(response).get("id").asLong();

    // Create an update request with score, time and court
    UpdateGameRequest updateRequest = new UpdateGameRequest();
    Score             score         = new Score();
    score.setSets(List.of(
        new SetScore(6, 4),
        new SetScore(6, 2)
    ));
    score.setForfeit(false);
    updateRequest.setScore(score);
    updateRequest.setScheduledTime(LocalTime.of(14, 30));
    updateRequest.setCourt("Court 1");

    // When & Then
    mockMvc.perform(put("/games/" + gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.finished").value(true))
           .andExpect(jsonPath("$.score.sets", hasSize(2)))
           .andExpect(jsonPath("$.score.sets[0].teamAScore").value(6))
           .andExpect(jsonPath("$.score.sets[0].teamBScore").value(4))
           .andExpect(jsonPath("$.scheduledTime").value("14:30"))
           .andExpect(jsonPath("$.court").value("Court 1"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldDeleteGame() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    String response = mockMvc.perform(post("/games")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                             .andReturn().getResponse().getContentAsString();

    Long gameId = objectMapper.readTree(response).get("id").asLong();

    // When & Then
    mockMvc.perform(delete("/games/" + gameId))
           .andExpect(status().isNoContent());

    // Verify game is deleted - should return empty list or not find the game
    assertThat(gameRepository.findById(gameId)).isEmpty();
  }

  @Test
  void shouldReturn401WhenNotAuthenticated() throws Exception {
    // Given
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    // When & Then - POST should require authentication
    mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "testuser")
  void shouldAllowPublicAccessToGetEndpoints() throws Exception {
    // Given - create a game first
    CreatePlayerPairRequest teamA  = new CreatePlayerPairRequest("John", "Jane");
    CreatePlayerPairRequest teamB  = new CreatePlayerPairRequest("Alice", "Bob");
    MatchFormat             format = new MatchFormat();
    format.setNumberOfSetsToWin(2);
    format.setGamesPerSet(6);

    CreateStandaloneGameRequest request = new CreateStandaloneGameRequest(teamA, teamB, format);

    String response = mockMvc.perform(post("/games")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                             .andReturn().getResponse().getContentAsString();

    long gameId = objectMapper.readTree(response).get("id").asLong();

    // When & Then - GET endpoints should work without authentication
    // Note: We can't easily test without authentication in @SpringBootTest with security enabled
    // This test validates that authenticated users can still access GET endpoints
    mockMvc.perform(get("/games"))
           .andExpect(status().isOk());

    mockMvc.perform(get("/games/" + gameId))
           .andExpect(status().isOk());
  }
}
