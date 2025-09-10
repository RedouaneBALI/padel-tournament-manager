package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.request.UpdateTournamentRequest;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
    lenient().when(securityProps.getSuperAdmins()).thenReturn(Collections.emptySet());
    drawGenerationService = mock(DrawGenerationService.class);
    lenient().when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    tournamentService = new TournamentService(
        tournamentRepository,
        securityProps,
        drawGenerationService
    );
  }

  @Test
  void testGenerateDraw_shouldDelegateToDrawManualGenerationService() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(drawGenerationService.generateDrawAuto(tournament)).thenReturn(tournament);

    Tournament result = tournamentService.generateDrawAuto(1L);

    verify(drawGenerationService, times(1)).generateDrawAuto(tournament);
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
  void testGetTournamentById_shouldThrowWhenNotFound() {
    when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> tournamentService.getTournamentById(99L));
  }

  @Test
  @Disabled
    // no initialization before generation
  void testCreateTournament_initializesStructure_whenConfigProvided() {
    Tournament t = new Tournament();
    t.getConfig().setFormat(TournamentFormat.KNOCKOUT);
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).build());

    Tournament saved = tournamentService.createTournament(t);

    // rounds: SEMIS + FINAL
    long semis  = saved.getRounds().stream().filter(r -> r.getStage() == Stage.SEMIS).count();
    long finals = saved.getRounds().stream().filter(r -> r.getStage() == Stage.FINAL).count();
    assertEquals(1, semis);
    assertEquals(1, finals);
  }

  @Test
  void testCreateTournament_skipsInit_whenNoFormatConfig() {
    Tournament t = new Tournament();
    t.setConfig(TournamentConfig.builder().build());
    t.getConfig().setFormat(TournamentFormat.KNOCKOUT);
    t.setConfig(null);

    Tournament saved = tournamentService.createTournament(t);
    assertEquals(0, saved.getRounds().size());
  }

  @Test
  void testCreateTournament_throwsOnInvalidConfig() {
    Tournament t = new Tournament();
    t.setConfig(TournamentConfig.builder().build());
    t.getConfig().setFormat(TournamentFormat.KNOCKOUT);
    // invalid: mainDrawSize not power of two and seeds > size
    t.setConfig(TournamentConfig.builder().mainDrawSize(12).nbSeeds(16).build());

    // Mock the drawGenerationService to return validation errors instead of throwing exception
    List<String> expectedErrors = List.of(
        "mainDrawSize must be a power of 2, got: 12",
        "nbSeeds (16) cannot exceed mainDrawSize (12)"
    );
    doThrow(new IllegalArgumentException("Invalid tournament configuration: " + String.join(", ", expectedErrors)))
        .when(drawGenerationService).validate(any(Tournament.class));

    assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(t));
  }

  @Test
  void testDeleteTournament_deniedWhenNotOwnerOrSuperAdmin() {
    Tournament existing = new Tournament();
    existing.setId(42L);
    existing.setOwnerId("someone@else");
    when(tournamentRepository.findById(42L)).thenReturn(Optional.of(existing));

    assertThrows(org.springframework.security.access.AccessDeniedException.class,
                 () -> tournamentService.deleteTournament(42L));
  }

  @Test
  void testDeleteTournament_allowedForOwner() {
    Tournament existing = new Tournament();
    existing.setId(43L);
    existing.setOwnerId("bali.redouane@gmail.com");
    when(tournamentRepository.findById(43L)).thenReturn(Optional.of(existing));

    tournamentService.deleteTournament(43L);
    verify(tournamentRepository, times(1)).delete(existing);
  }

  @Test
  void testUpdateTournament_updatesFields_whenOwner() {
    Tournament existing = new Tournament();
    existing.setId(7L);
    existing.setOwnerId("bali.redouane@gmail.com");
    when(tournamentRepository.findById(7L)).thenReturn(Optional.of(existing));
    lenient().when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateTournamentRequest input = new UpdateTournamentRequest();
    input.setName("New name");
    input.setConfig(TournamentConfig.builder().build());
    input.getConfig().setFormat(TournamentFormat.KNOCKOUT);

    Tournament updated = tournamentService.updateTournament(7L, input);

    assertEquals("New name", updated.getName());
    //  assertEquals(4, updated.getNbSeeds());
    assertEquals(TournamentFormat.KNOCKOUT, updated.getConfig().getFormat());
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

  @Test
  void testGetGamesByTournamentAndStage_shouldThrowWhenRoundMissing() {
    Tournament tournament = new Tournament();
    tournament.setId(2L);
    when(tournamentRepository.findById(2L)).thenReturn(Optional.of(tournament));

    assertThrows(IllegalArgumentException.class,
                 () -> tournamentService.getGamesByTournamentAndStage(2L, Stage.QUARTERS));
  }

  @Test
  void testGetTournamentForCurrentUser_setsEditableTrueForOwner() {
    Tournament t = new Tournament();
    t.setId(3L);
    t.setOwnerId("bali.redouane@gmail.com");
    when(tournamentRepository.findById(3L)).thenReturn(Optional.of(t));

    Tournament res = tournamentService.getTournamentForCurrentUser(3L);
    assertTrue(res.isEditable());
  }

  @Test
  void testListByOwner_delegatesToRepository() {
    tournamentService.listByOwner("owner");
    verify(tournamentRepository, times(1)).findAllByOwnerId("owner");
  }

  @Test
  void testListAll_delegatesToRepository() {
    tournamentService.listAll();
    verify(tournamentRepository, times(1)).findAll();
  }
}