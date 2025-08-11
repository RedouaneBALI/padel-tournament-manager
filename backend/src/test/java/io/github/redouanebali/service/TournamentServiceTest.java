package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class TournamentServiceTest {

  private TournamentService     tournamentService;
  private SecurityProps         securityProps;
  private TournamentRepository  tournamentRepository;
  private DrawGenerationService drawGenerationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    Jwt jwt = Jwt.withTokenValue("fake")
                 .header("alg", "none")
                 .claim("email", "bali.redouane@gmail.com")
                 .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList(), "bali.redouane@gmail.com");
    SecurityContextHolder.getContext().setAuthentication(auth);

    tournamentRepository = mock(TournamentRepository.class);
    securityProps        = mock(SecurityProps.class);
    org.mockito.Mockito.lenient().when(securityProps.getSuperAdmins()).thenReturn(Collections.emptySet());
    drawGenerationService = mock(DrawGenerationService.class);

    tournamentService = new TournamentService(
        tournamentRepository,
        securityProps,
        drawGenerationService
    );
  }

  @Test
  void testGenerateDraw_shouldDelegateToDrawGenerationService() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(drawGenerationService.generateDraw(tournament, false)).thenReturn(tournament);

    Tournament result = tournamentService.generateDraw(1L, false);

    verify(drawGenerationService, times(1)).generateDraw(tournament, false);
    assertEquals(tournament, result);
  }

  @Test
  void testGetTournamentById_shouldReturnTournament() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    Tournament result = tournamentService.getTournamentById(1L);

    assertEquals(tournament, result);
  }

  @Test
  void testGetGamesByTournamentAndStage_shouldReturnGames() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);

    Round round = new Round(Stage.R16);

    tournament.getRounds().clear();
    tournament.getRounds().add(round);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertEquals(0, tournamentService.getGamesByTournamentAndStage(1L, Stage.R16).size());
  }
}