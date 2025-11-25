package io.github.redouanebali.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicTournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PublicTournamentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TournamentService tournamentService;

  @MockBean
  private PlayerPairService playerPairService;

  @MockBean
  private MatchFormatService matchFormatService;

  @MockBean
  private TournamentMapper tournamentMapper;

  @Test
  public void getTournament_returnsOk_whenFound() throws Exception {
    Tournament t = new Tournament();
    t.setId(5L);
    when(tournamentService.getTournamentById(5L)).thenReturn(t);
    when(tournamentMapper.toDTO(t)).thenReturn(new TournamentDTO());

    mockMvc.perform(get("/tournaments/{id}", 5L).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());
  }

  @Test
  public void getGame_returnsNotFound_whenMissing() throws Exception {
    Tournament t = new Tournament();
    t.setId(6L);
    when(tournamentService.getTournamentById(6L)).thenReturn(t);

    mockMvc.perform(get("/tournaments/{tournamentId}/games/{gameId}", 6L, 999L).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isNotFound());
  }

}
