package io.github.redouanebali.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.security.AuthorizationService;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
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

@WebMvcTest(controllers = PublicTournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PublicTournamentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TournamentService tournamentService;

  @MockitoBean
  private PlayerPairService playerPairService;

  @MockitoBean
  private MatchFormatService matchFormatService;

  @MockitoBean
  private TournamentMapper tournamentMapper;

  @MockitoBean
  private AuthorizationService authorizationService;

  private MockedStatic<SecurityUtil> secMock;

  @BeforeEach
  public void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
  }

  @AfterEach
  public void tearDown() {
    if (secMock != null) {
      secMock.close();
    }
  }

  @Test
  public void getTournament_returnsOk_whenFound() throws Exception {
    Tournament t = new Tournament();
    t.setId(5L);

    TournamentDTO dto = new TournamentDTO();
    dto.setId(5L);

    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
    when(tournamentService.getTournamentById(5L)).thenReturn(t);
    when(tournamentMapper.toDTO(t)).thenReturn(dto);
    when(authorizationService.canEditTournament(t, "user1")).thenReturn(false);

    mockMvc.perform(MockMvcRequestBuilders.get("/tournaments/{id}", 5L).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
  }

  @Test
  public void getTournament_isEditableFalse_whenNotOwner() throws Exception {
    Tournament t = new Tournament();
    t.setId(10L);
    t.setOwnerId("otherUser");

    TournamentDTO dto = new TournamentDTO();
    dto.setId(10L);

    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
    when(tournamentService.getTournamentById(10L)).thenReturn(t);
    when(tournamentMapper.toDTO(t)).thenReturn(dto);
    when(authorizationService.canEditTournament(t, "user1")).thenReturn(false);

    String response = mockMvc.perform(MockMvcRequestBuilders.get("/tournaments/{id}", 10L).accept(MediaType.APPLICATION_JSON))
                             .andExpect(status().isOk())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(response).contains("\"isEditable\":false");
  }

  @Test
  public void getTournament_isEditableTrue_whenOwner() throws Exception {
    Tournament t = new Tournament();
    t.setId(11L);
    t.setOwnerId("user1");

    TournamentDTO dto = new TournamentDTO();
    dto.setId(11L);

    secMock.when(SecurityUtil::currentUserId).thenReturn("user1");
    when(tournamentService.getTournamentById(11L)).thenReturn(t);
    when(tournamentMapper.toDTO(t)).thenReturn(dto);
    when(authorizationService.canEditTournament(t, "user1")).thenReturn(true);

    String response = mockMvc.perform(MockMvcRequestBuilders.get("/tournaments/{id}", 11L).accept(MediaType.APPLICATION_JSON))
                             .andExpect(status().isOk())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(response).contains("\"isEditable\":true");
  }

  @Test
  public void getGame_returnsNotFound_whenMissing() throws Exception {
    Tournament t = new Tournament();
    t.setId(6L);
    when(tournamentService.getTournamentById(6L)).thenReturn(t);

    mockMvc.perform(MockMvcRequestBuilders.get("/tournaments/{tournamentId}/games/{gameId}", 6L, 999L).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isNotFound());
  }

}
