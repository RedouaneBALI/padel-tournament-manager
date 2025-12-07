package io.github.redouanebali.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redouanebali.dto.request.CreateTournamentRequest;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.User;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.GameService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import io.github.redouanebali.websocket.GameScoreWebSocketController;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = AdminTournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminTournamentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TournamentService tournamentService;

  @MockitoBean
  private TournamentMapper tournamentMapper;

  @MockitoBean
  private SecurityProps securityProps;

  @MockitoBean
  private io.github.redouanebali.security.AuthorizationService authorizationService;

  // newly added mocks required by controller constructor
  @MockitoBean
  private PlayerPairService playerPairService;

  @MockitoBean
  private GameService gameService;

  @MockitoBean
  private MatchFormatService matchFormatService;

  @MockitoBean
  private GameScoreWebSocketController gameScoreWebSocketController;

  @MockitoBean
  private io.github.redouanebali.service.UserService userService;

  private MockedStatic<SecurityUtil> secMock;

  @BeforeEach
  public void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
    when(userService.getUserNameByEmail("user1")).thenReturn("User One");
  }

  @AfterEach
  public void tearDown() {
    if (secMock != null) {
      secMock.close();
    }
  }

  @Test
  public void createTournament_returnsCreated() throws Exception {
    CreateTournamentRequest req = new CreateTournamentRequest();
    // minimal valid request
    req.setName("T1");

    Tournament t = new Tournament();
    t.setName("T1");
    t.setOwnerId("user1");
    t.setId(10L);

    TournamentDTO dto = new TournamentDTO();
    dto.setOwnerId("user1");

    when(tournamentMapper.toEntity(req)).thenReturn(t);
    when(tournamentService.createTournament(t)).thenReturn(t);
    when(tournamentMapper.toDTO(t)).thenReturn(dto);

    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
    secMock.when(SecurityUtil::getCurrentUser).thenReturn(new User("user1", "User Name", "en"));

    mockMvc.perform(MockMvcRequestBuilders.post("/admin/tournaments")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isCreated());
  }

  @Test
  public void listMyTournaments_mine() throws Exception {
    when(securityProps.getSuperAdmins()).thenReturn(java.util.Set.of());
    when(tournamentService.listByOwnerOrEditor("user1")).thenReturn(List.of());
    when(tournamentMapper.toDTO(java.util.Collections.emptyList())).thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/admin/tournaments").accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
  }

}