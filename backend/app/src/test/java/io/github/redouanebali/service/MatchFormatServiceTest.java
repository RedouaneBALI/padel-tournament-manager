package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.AuthorizationService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MatchFormatServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @Mock
  private AuthorizationService authorizationService;

  @InjectMocks
  private MatchFormatService matchFormatService;

  private Tournament  tournament;
  private Round       round;
  private MatchFormat existingFormat;


  @BeforeEach
  void setUp() {
    Jwt jwt = Jwt.withTokenValue("fake")
                 .header("alg", "none")
                 .claim("email", "bali.redouane@gmail.com")
                 .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList(), "bali.redouane@gmail.com");
    SecurityContextHolder.getContext().setAuthentication(auth);

    existingFormat = new MatchFormat();
    existingFormat.setNumberOfSetsToWin(2);
    existingFormat.setGamesPerSet(6);
    existingFormat.setAdvantage(true);
    existingFormat.setSuperTieBreakInFinalSet(false);

    round = new Round();
    round.setStage(Stage.R32);
    round.setMatchFormat(existingFormat);

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.getRounds().clear();
    tournament.getRounds().add(round);

    org.mockito.Mockito.lenient().when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
  }

  @Test
  void testGetMatchFormatForRound() {
    MatchFormat result = matchFormatService.getMatchFormatForRound(1L, Stage.R32);

    assertEquals(existingFormat, result);
    verify(tournamentRepository).findById(1L);
  }

  @Test
  void testUpdateMatchFormatForRound() {
    MatchFormat newFormat = new MatchFormat();
    newFormat.setNumberOfSetsToWin(3);
    newFormat.setGamesPerSet(7);
    newFormat.setAdvantage(false);
    newFormat.setSuperTieBreakInFinalSet(true);

    MatchFormat updated = matchFormatService.updateMatchFormatForRound(1L, Stage.R32, newFormat);

    assertEquals(3, existingFormat.getNumberOfSetsToWin());
    assertEquals(7, existingFormat.getGamesPerSet());
    assertFalse(existingFormat.isAdvantage());
    assertTrue(existingFormat.isSuperTieBreakInFinalSet());
    assertEquals(newFormat, updated);

    verify(tournamentRepository).save(tournament);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
