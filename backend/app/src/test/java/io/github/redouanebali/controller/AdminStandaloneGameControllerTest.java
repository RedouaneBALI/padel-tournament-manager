package io.github.redouanebali.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateStandaloneGameRequest;
import io.github.redouanebali.dto.request.UpdateGameRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.StandaloneGameService;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(controllers = AdminStandaloneGameController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminStandaloneGameControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private StandaloneGameService standaloneGameService;

  @MockBean
  private TournamentMapper tournamentMapper;

  @MockBean
  private SecurityProps securityProps;

  private MockedStatic<SecurityUtil> secMock;

  private Game sampleGame(String idSuffix) {
    MatchFormat format = new MatchFormat();
    PlayerPair  a      = new PlayerPair();
    PlayerPair  b      = new PlayerPair();
    Game        g      = new Game(format);
    g.setId(Long.valueOf(1 + idSuffix.hashCode()));
    g.setTeamA(a);
    g.setTeamB(b);
    g.setScheduledTime(LocalTime.of(12, 0));
    g.setCreatedBy("user1");
    return g;
  }

  @BeforeEach
  public void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
  }

  @AfterEach
  public void tearDown() {
    if (secMock != null) {
      secMock.close();
    }
  }

  @Test
  public void listAdminGames_mine() throws Exception {
    Game g1 = sampleGame("a");
    when(standaloneGameService.getStandaloneGamesByOwner("user1")).thenReturn(List.of(g1));
    when(tournamentMapper.toDTOGameList(List.of(g1))).thenReturn(List.of(new GameDTO()));

    mockMvc.perform(get("/admin/games?scope=mine").accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
  }

  @Test
  public void listAdminGames_all_superAdmin() throws Exception {
    Game g1 = sampleGame("a");
    Game g2 = sampleGame("b");
    when(securityProps.getSuperAdmins()).thenReturn(java.util.Set.of("user1"));
    when(standaloneGameService.getAllStandaloneGames()).thenReturn(List.of(g1, g2));
    when(tournamentMapper.toDTOGameList(List.of(g1, g2))).thenReturn(List.of(new GameDTO(), new GameDTO()));

    mockMvc.perform(get("/admin/games?scope=all").accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
  }

  @Test
  public void createGame_returnsCreated() throws Exception {
    CreateStandaloneGameRequest req = new CreateStandaloneGameRequest(
        new CreatePlayerPairRequest("p1", "p2"),
        new CreatePlayerPairRequest("p3", "p4"),
        new MatchFormat()
    );

    Game created = sampleGame("created");
    created.setId(123L);

    when(standaloneGameService.createStandaloneGame(org.mockito.Mockito.any(CreateStandaloneGameRequest.class))).thenReturn(created);
    when(tournamentMapper.toDTO(created)).thenReturn(new GameDTO());

    mockMvc.perform(post("/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isCreated());
  }

  @Test
  public void deleteGame_noContent_whenOwner() throws Exception {
    Long id   = 55L;
    Game game = sampleGame("x");
    game.setId(id);
    game.setCreatedBy("user1");

    when(standaloneGameService.getGameById(id)).thenReturn(game);
    doNothing().when(standaloneGameService).deleteGame(id);

    mockMvc.perform(delete("/admin/games/{id}", id))
           .andExpect(status().isNoContent());
  }

  @Test
  public void deleteGame_forbidden_whenNotOwner() throws Exception {
    Long id   = 99L;
    Game game = sampleGame("y");
    game.setId(id);
    game.setCreatedBy("other");

    when(standaloneGameService.getGameById(id)).thenReturn(game);
    when(securityProps.getSuperAdmins()).thenReturn(java.util.Set.of());

    org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class, () -> {
      try {
        mockMvc.perform(delete("/admin/games/{id}", id)).andReturn();
      } catch (Exception e) {
        throw e;
      }
    }, "Expected ServletException caused by AccessDeniedException");
  }

  @Test
  public void updateGame_returnsOk_whenOwner() throws Exception {
    Long id   = 77L;
    Game game = sampleGame("u");
    game.setId(id);
    game.setCreatedBy("user1");

    UpdateGameRequest req = new UpdateGameRequest();
    req.setCourt("Court 1");
    req.setScheduledTime(java.time.LocalTime.of(15, 30));

    Game updated = sampleGame("u-upd");
    updated.setId(id);
    updated.setCourt("Court 1");

    when(standaloneGameService.getGameById(id)).thenReturn(game);
    when(standaloneGameService.updateGame(org.mockito.Mockito.eq(id), org.mockito.Mockito.any(UpdateGameRequest.class))).thenReturn(updated);
    when(tournamentMapper.toDTO(updated)).thenReturn(new GameDTO());

    mockMvc.perform(put("/admin/games/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isOk());
  }

}
