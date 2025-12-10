package io.github.redouanebali.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.response.VoteSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Vote;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.VoteRepository;
import io.github.redouanebali.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoteServiceTest {

  @Mock
  private VoteRepository voteRepository;

  @Mock
  private GameRepository gameRepository;

  @InjectMocks
  private VoteService voteService;

  private MockedStatic<SecurityUtil> secMock;

  static Stream<Arguments> currentUserVoteProvider() {
    return Stream.of(
        Arguments.of(TeamSide.TEAM_A, TeamSide.TEAM_A),
        Arguments.of(TeamSide.TEAM_B, TeamSide.TEAM_B),
        Arguments.of(null, null)
    );
  }

  @BeforeEach
  void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
  }

  @AfterEach
  void tearDown() {
    secMock.close();
  }

  private HttpServletRequest mockRequest(String ip, String userAgent) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn(ip);
    when(request.getHeader("User-Agent")).thenReturn(userAgent);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    return request;
  }

  @ParameterizedTest(name = "vote for {0} creates vote successfully")
  @CsvSource({"TEAM_A", "TEAM_B"})
  void vote_createsVote_whenUserHasNotVoted(TeamSide teamSide) {
    Long               gameId  = 1L;
    HttpServletRequest request = mockRequest("192.168.1.1", "TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    Game mockGame = mock(Game.class);
    when(mockGame.isStarted()).thenReturn(false);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(mockGame));

    when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.empty());
    when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> {
      Vote v = inv.getArgument(0);
      v.setId(1L);
      return v;
    });

    Vote result = voteService.vote(gameId, teamSide, request);

    assertThat(result).isNotNull();
    assertThat(result.getGameId()).isEqualTo(gameId);
    assertThat(result.getTeamSide()).isEqualTo(teamSide);
    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void getVoteSummary_returnsCorrectCounts() {
    Long               gameId  = 1L;
    HttpServletRequest request = mockRequest("192.168.1.1", "TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    long teamAVotes = 5;
    long teamBVotes = 3;

    when(voteRepository.countTeamAVotes(gameId)).thenReturn(teamAVotes);
    when(voteRepository.countTeamBVotes(gameId)).thenReturn(teamBVotes);
    when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.empty());

    VoteSummaryDTO summary = voteService.getVoteSummary(gameId, request);

    assertThat(summary.getTeamAVotes()).isEqualTo(teamAVotes);
    assertThat(summary.getTeamBVotes()).isEqualTo(teamBVotes);
    assertThat(summary.getCurrentUserVote()).isNull();
  }

  @ParameterizedTest(name = "getVoteSummary returns currentUserVote={1} when user voted for {0}")
  @MethodSource("currentUserVoteProvider")
  void getVoteSummary_returnsCurrentUserVote(TeamSide votedFor, TeamSide expectedUserVote) {
    Long               gameId  = 1L;
    HttpServletRequest request = mockRequest("192.168.1.1", "TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    when(voteRepository.countTeamAVotes(gameId)).thenReturn(5L);
    when(voteRepository.countTeamBVotes(gameId)).thenReturn(3L);

    if (votedFor != null) {
      Vote userVote = new Vote(gameId, "anon:hash", votedFor, false);
      when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.of(userVote));
    } else {
      when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.empty());
    }

    VoteSummaryDTO summary = voteService.getVoteSummary(gameId, request);

    assertThat(summary.getCurrentUserVote()).isEqualTo(expectedUserVote);
  }

  @Test
  void resolveVoterId_usesXForwardedFor_whenPresent() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");
    when(request.getHeader("User-Agent")).thenReturn("TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    String voterId = voteService.resolveVoterId(request);

    assertThat(voterId).startsWith("anon:");
  }

  @Test
  void resolveVoterId_differentFingerprints_produceDifferentIds() {
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    HttpServletRequest request1 = mockRequest("192.168.1.1", "Browser1");
    HttpServletRequest request2 = mockRequest("192.168.1.2", "Browser1");
    HttpServletRequest request3 = mockRequest("192.168.1.1", "Browser2");

    String id1 = voteService.resolveVoterId(request1);
    String id2 = voteService.resolveVoterId(request2);
    String id3 = voteService.resolveVoterId(request3);

    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).isNotEqualTo(id3);
    assertThat(id2).isNotEqualTo(id3);
  }

  @Test
  void vote_allowsChangingVote_whenGameNotStarted() {
    Long               gameId  = 1L;
    HttpServletRequest request = mockRequest("192.168.1.1", "TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    Game mockGame = mock(Game.class);
    when(mockGame.isStarted()).thenReturn(false);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(mockGame));

    Vote existingVote = new Vote(gameId, "anon:abc123", TeamSide.TEAM_A, false);
    when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.of(existingVote));
    when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> inv.getArgument(0));

    Vote result = voteService.vote(gameId, TeamSide.TEAM_B, request);

    assertThat(result).isNotNull();
    assertThat(result.getTeamSide()).isEqualTo(TeamSide.TEAM_B);
    assertThat(result.getGameId()).isEqualTo(gameId);
    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  void vote_throwsException_whenVotingForSameTeam() {
    Long               gameId  = 1L;
    HttpServletRequest request = mockRequest("192.168.1.1", "TestBrowser");
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    Game mockGame = mock(Game.class);
    when(mockGame.isStarted()).thenReturn(false);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(mockGame));

    Vote existingVote = new Vote(gameId, "anon:abc123", TeamSide.TEAM_A, false);
    when(voteRepository.findByGameIdAndVoterId(any(), any())).thenReturn(Optional.of(existingVote));

    assertThatThrownBy(() -> voteService.vote(gameId, TeamSide.TEAM_A, request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("You have already voted for this team");
  }
}
