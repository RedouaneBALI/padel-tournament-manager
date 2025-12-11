package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.dto.request.UpdateTournamentRequest;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
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
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TournamentServiceTest {

  private TournamentService                                    tournamentService;
  private SecurityProps                                        securityProps;
  private TournamentRepository                                 tournamentRepository;
  private DrawGenerationService                                drawGenerationService;
  private io.github.redouanebali.security.AuthorizationService authorizationService;

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
    authorizationService  = mock(io.github.redouanebali.security.AuthorizationService.class);
    // Configure authorizationService to check ownership (simulate real behavior)
    lenient().when(authorizationService.canEditTournament(any(), any())).thenAnswer(inv -> {
      Tournament t      = inv.getArgument(0);
      String     userId = inv.getArgument(1);
      return t.isEditableBy(userId);
    });
    lenient().when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    tournamentService = new TournamentService(
        tournamentRepository,
        drawGenerationService,
        authorizationService
    );
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
  void testGetTournamentsByOwner_delegatesToRepository() {
    tournamentService.getTournamentsByOwner("owner");
    verify(tournamentRepository, times(1)).findAllByOwnerId("owner");
  }

  @Test
  void testListAll_delegatesToRepository() {
    tournamentService.listAll();
    verify(tournamentRepository, times(1)).findAll();
  }

  @Test
  void testGetTournamentsByOwner() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    when(tournamentRepository.findAllByOwnerId("bali.redouane@gmail.com")).thenReturn(List.of(tournament));

    List<Tournament> result = tournamentService.getTournamentsByOwner("bali.redouane@gmail.com");

    assertEquals(1, result.size());
    assertEquals(tournament, result.get(0));
  }

  @Test
  void testGenerateDrawManual_withTwoTeams_shouldWork() {
    // Create tournament with 2 player pairs
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    Player player1 = new Player("Player1");
    Player player2 = new Player("Player2");
    Player player3 = new Player("Player3");
    Player player4 = new Player("Player4");

    PlayerPair pair1 = new PlayerPair(player1, player2, 1);
    PlayerPair pair2 = new PlayerPair(player3, player4, 2);

    tournament.getPlayerPairs().add(pair1);
    tournament.getPlayerPairs().add(pair2);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    // Mock drawGenerationService to return the tournament with a final round added
    Tournament updatedTournament = new Tournament();
    updatedTournament.setId(1L);
    updatedTournament.setOwnerId("bali.redouane@gmail.com");
    updatedTournament.getPlayerPairs().addAll(tournament.getPlayerPairs());
    Round finalRound = new Round(Stage.FINAL);
    updatedTournament.getRounds().add(finalRound);

    when(drawGenerationService.generateDrawManual(any(Tournament.class), any())).thenReturn(updatedTournament);

    // Create RoundRequest for manual draw: one round FINAL with one game
    RoundRequest roundRequest = new RoundRequest();
    roundRequest.setStage("FINAL");
    // Assuming GameRequest has teamA and teamB as PlayerPairRequest
    // For simplicity, since it's manual, we can pass empty or mock

    List<RoundRequest> initialRounds = List.of(roundRequest);

    // Call the method
    Tournament result = tournamentService.generateDrawManual(1L, initialRounds);

    // Assertions
    assertEquals(1L, result.getId());
    assertEquals(2, result.getPlayerPairs().size());
    assertEquals(1, result.getRounds().size());
    assertEquals(Stage.FINAL, result.getRounds().get(0).getStage());

    // Verify drawGenerationService was called
    verify(drawGenerationService, times(1)).generateDrawManual(tournament, initialRounds);
  }
}