package io.github.redouanebali.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.StandaloneGameService;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = PublicStandaloneGameController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PublicStandaloneGameControllerTest {

  @Autowired
  private MockMvc mockMvc;

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
    g.setId(Long.valueOf(Math.abs(idSuffix.hashCode()) + 1));
    g.setTeamA(a);
    g.setTeamB(b);
    g.setCourt("C1");
    g.setScheduledTime(LocalTime.of(12, 0));
    return g;
  }

  private GameDTO sampleGameDTO() {
    return new GameDTO();
  }

  @BeforeEach
  public void setUp() {
    // default: current user is 'user1'
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
  public void getGame_returns200_whenGameExists() throws Exception {
    Game g1 = sampleGame("a");

    when(standaloneGameService.getGameById(g1.getId())).thenReturn(g1);
    when(tournamentMapper.toDTO(g1)).thenReturn(sampleGameDTO());

    mockMvc.perform(get("/games/{id}", g1.getId()).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  public void getGame_returns404_whenGameNotFound() throws Exception {
    Long missingId = 9999L;
    when(standaloneGameService.getGameById(missingId)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                             "Game not found with ID: " + missingId));

    mockMvc.perform(get("/games/{id}", missingId).accept(MediaType.APPLICATION_JSON))
           .andExpect(result -> {
             int sc = result.getResponse().getStatus();
             if (!(sc >= 400 && sc < 600)) {
               throw new AssertionError("Unexpected status: " + sc);
             }
           });
  }

}
