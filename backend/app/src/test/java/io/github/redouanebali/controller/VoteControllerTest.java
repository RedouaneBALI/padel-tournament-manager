package io.github.redouanebali.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.dto.response.VoteSummaryDTO;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Vote;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.service.VoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = VoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class VoteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private VoteService voteService;

  @MockitoBean
  private SecurityProps securityProps;

  @ParameterizedTest(name = "POST /games/{0}/votes with teamSide={1} returns 200")
  @CsvSource({"1,TEAM_A", "2,TEAM_B", "99,TEAM_A"})
  void vote_returns200_whenSuccessful(Long gameId, TeamSide teamSide) throws Exception {
    Vote           vote    = new Vote(gameId, "user:test@test.com", teamSide, true);
    VoteSummaryDTO summary = new VoteSummaryDTO(1, 0, teamSide);

    when(voteService.vote(eq(gameId), eq(teamSide), any())).thenReturn(vote);
    when(voteService.getVoteSummary(eq(gameId), any())).thenReturn(summary);

    mockMvc.perform(MockMvcRequestBuilders.post("/games/{gameId}/votes", gameId)
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content("{\"teamSide\":\"" + teamSide + "\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.teamAVotes").value(1))
           .andExpect(jsonPath("$.teamBVotes").value(0))
           .andExpect(jsonPath("$.currentUserVote").value(teamSide.toString()));
  }

  @Test
  void vote_returns409_whenAlreadyVoted() throws Exception {
    Long gameId = 1L;
    when(voteService.vote(eq(gameId), any(), any()))
        .thenThrow(new IllegalStateException("You have already voted for this game"));

    mockMvc.perform(MockMvcRequestBuilders.post("/games/{gameId}/votes", gameId)
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content("{\"teamSide\":\"TEAM_A\"}"))
           .andExpect(status().isConflict());
  }

  @Test
  void vote_returns409_whenGameHasStarted() throws Exception {
    Long gameId = 1L;
    when(voteService.vote(eq(gameId), any(), any()))
        .thenThrow(new IllegalStateException("Cannot vote after game has started"));

    mockMvc.perform(MockMvcRequestBuilders.post("/games/{gameId}/votes", gameId)
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content("{\"teamSide\":\"TEAM_A\"}"))
           .andExpect(status().isConflict());
  }

  @Test
  void vote_returns409_whenVotingForSameTeam() throws Exception {
    Long gameId = 1L;
    when(voteService.vote(eq(gameId), eq(TeamSide.TEAM_A), any()))
        .thenThrow(new IllegalStateException("You have already voted for this team"));

    mockMvc.perform(MockMvcRequestBuilders.post("/games/{gameId}/votes", gameId)
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content("{\"teamSide\":\"TEAM_A\"}"))
           .andExpect(status().isConflict());
  }

  @Test
  void vote_returns400_whenTeamSideNull() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/games/1/votes")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content("{}"))
           .andExpect(status().isBadRequest());
  }

  @ParameterizedTest(name = "GET /games/{0}/votes returns counts teamA={1}, teamB={2}")
  @CsvSource({"1,5,3", "2,0,0", "99,10,10"})
  void getVotes_returns200_withCounts(Long gameId, long teamAVotes, long teamBVotes) throws Exception {
    VoteSummaryDTO summary = new VoteSummaryDTO(teamAVotes, teamBVotes, null);
    when(voteService.getVoteSummary(eq(gameId), any())).thenReturn(summary);

    mockMvc.perform(MockMvcRequestBuilders.get("/games/{gameId}/votes", gameId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.teamAVotes").value(teamAVotes))
           .andExpect(jsonPath("$.teamBVotes").value(teamBVotes))
           .andExpect(jsonPath("$.currentUserVote").isEmpty());
  }

  @Test
  void getVotes_returnsCurrentUserVote_whenUserHasVoted() throws Exception {
    Long           gameId  = 1L;
    VoteSummaryDTO summary = new VoteSummaryDTO(5, 3, TeamSide.TEAM_B);
    when(voteService.getVoteSummary(eq(gameId), any())).thenReturn(summary);

    mockMvc.perform(MockMvcRequestBuilders.get("/games/{gameId}/votes", gameId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.teamAVotes").value(5))
           .andExpect(jsonPath("$.teamBVotes").value(3))
           .andExpect(jsonPath("$.currentUserVote").value("TEAM_B"));
  }
}
