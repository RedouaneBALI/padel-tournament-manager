package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.AuthorizationService;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.util.TestFixturesApp;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DrawGenerationServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;
  @Mock
  private SecurityProps        securityProps;
  @Mock
  private AuthorizationService authorizationService;
  @Mock
  private EntityManager        entityManager;

  @InjectMocks
  private DrawGenerationService drawGenerationService;

  @BeforeEach
  void setUp() {
    Jwt jwt = Jwt.withTokenValue("fake")
                 .header("alg", "none")
                 .claim("email", "bali.redouane@gmail.com")
                 .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList(), "bali.redouane@gmail.com");
    SecurityContextHolder.getContext().setAuthentication(auth);

    lenient().when(securityProps.getSuperAdmins()).thenReturn(Collections.emptySet());
    lenient().when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Configure authorizationService to throw AccessDeniedException by default (can be overridden in specific tests)
    lenient().doAnswer(inv -> {
      Tournament t      = inv.getArgument(0);
      String     userId = inv.getArgument(1);
      // Allow if user is owner or in super-admins
      if (!securityProps.getSuperAdmins().contains(userId) && !t.isEditableBy(userId)) {
        throw new AccessDeniedException("You are not allowed to initialize draw for this tournament");
      }
      return null;
    }).when(authorizationService).requireTournamentEditPermission(any(), any());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // -------------------- capPairsToMax --------------------

  @Test
  void capPairsToMax_truncates_whenAboveLimit() {
    Tournament tournament = baseTournamentKO(32, 0);
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixturesApp.createPlayerPairs(36));
    List<PlayerPair> result = DrawGenerationService.capPairsToMax(tournament);
    assertEquals(32, result.size());
    Assertions.assertEquals(1, result.getFirst().getSeed());
    Assertions.assertEquals(32, result.get(31).getSeed());
  }

  // -------------------- generateDraw (KO) --------------------

  @Test
  void generateDraw_manual_knockout_populatesSemis_andSaves() {
    Tournament tournament = baseTournamentKO(4, 0);

    // Use helper method to create manual RoundRequest objects
    List<PlayerPair>   pairs        = new ArrayList<>(tournament.getPlayerPairs());
    List<RoundRequest> manualRounds = TestFixturesApp.createManualRoundRequestsFromPairs(Stage.SEMIS, pairs);

    Tournament updated = drawGenerationService.generateDrawManual(tournament, manualRounds);
    assertSame(tournament, updated);
    Round semis = updated.getRoundByStage(Stage.SEMIS);
    assertNotNull(semis);
    Assertions.assertEquals(2, semis.getGames().size());
    // Manual draw must keep original order (1v2, 3v4 with sequential fill A then B)
    Game g1 = semis.getGames().getFirst();
    Game g2 = semis.getGames().get(1);
    assertSame(pairs.getFirst(), g1.getTeamA());
    assertSame(pairs.get(1), g1.getTeamB());
    assertSame(pairs.get(2), g2.getTeamA());
    assertSame(pairs.get(3), g2.getTeamB());
  }


  @Test
  void generateDraw_denied_whenNotOwnerOrSuperAdmin() {
    Tournament tournament = baseTournamentKO(4, 0);
    tournament.setOwnerId("not.me@example.com");
    List<PlayerPair>   pairs        = new ArrayList<>(tournament.getPlayerPairs());
    List<RoundRequest> manualRounds = TestFixturesApp.createManualRoundRequestsFromPairs(Stage.SEMIS, pairs);
    assertThrows(AccessDeniedException.class,
                 () -> drawGenerationService.generateDrawManual(tournament, manualRounds));
  }

  @Test
  void generateDraw_allowed_forSuperAdmin_evenIfNotOwner() {
    when(securityProps.getSuperAdmins()).thenReturn(Set.of("bali.redouane@gmail.com"));
    Tournament tournament = baseTournamentKO(4, 0);
    tournament.setOwnerId("someone@else");
    List<PlayerPair>   pairs        = new ArrayList<>(tournament.getPlayerPairs());
    List<RoundRequest> manualRounds = TestFixturesApp.createManualRoundRequestsFromPairs(Stage.SEMIS, pairs);
    Tournament         updated      = drawGenerationService.generateDrawManual(tournament, manualRounds);
    assertSame(tournament, updated);
  }

  @Test
  void generateDraw_savesTournament_once() {
    Tournament         tournament   = baseTournamentKO(4, 0);
    List<PlayerPair>   pairs        = new ArrayList<>(tournament.getPlayerPairs());
    List<RoundRequest> manualRounds = TestFixturesApp.createManualRoundRequestsFromPairs(Stage.SEMIS, pairs);
    drawGenerationService.generateDrawManual(tournament, manualRounds);
    verify(tournamentRepository, times(1)).save(tournament);
  }

  @Test
  void generateDrawManual_throwsException_whenInitialRoundsIsEmpty() {
    Tournament tournament = baseTournamentKO(4, 0);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> generateDrawManualWithEmptyRounds(tournament));
    assertEquals("Manual draw generation requires initial rounds to be provided.",
                 exception.getMessage());
  }

  @Test
  void generateDraw_manual_knockout_with_bye_should_propagate_bye_winners_to_semis() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(8).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());

    // Create 6 normal pairs
    List<PlayerPair> normals = TestFixturesApp.createPlayerPairs(6);
    // Create 2 BYE
    PlayerPair bye1 = PlayerPair.bye();
    PlayerPair bye2 = PlayerPair.bye();
    // Arrange pairs: bye, p1, p2, p3, p4, p5, p6, bye
    List<PlayerPair> pairs = new ArrayList<>();
    pairs.add(bye1);
    pairs.addAll(normals);
    pairs.add(bye2);
    tournament.getPlayerPairs().addAll(pairs);

    // Create manual rounds for quarters
    List<RoundRequest> manualRounds = TestFixturesApp.createManualRoundRequestsFromPairs(Stage.QUARTERS, pairs);

    // Generate draw
    Tournament updated = drawGenerationService.generateDrawManual(tournament, manualRounds);

    // Check semis
    Round semis = updated.getRoundByStage(Stage.SEMIS);
    assertNotNull(semis);
    assertEquals(2, semis.getGames().size());

    // The teams that faced BYE are p1 and p6
    PlayerPair p1 = normals.get(0);
    PlayerPair p6 = normals.get(5);

    // They should be in semis, but due to bug, they are not
    List<PlayerPair> semisTeams = semis.getGames().stream()
                                       .flatMap(g -> Stream.of(g.getTeamA(), g.getTeamB()))
                                       .filter(Objects::nonNull)
                                       .toList();
    assertTrue(semisTeams.contains(p1), "p1 should be in semis after facing BYE");
    assertTrue(semisTeams.contains(p6), "p6 should be in semis after facing BYE");
  }

  // -------------------- helpers --------------------

  private void generateDrawManualWithEmptyRounds(Tournament tournament) {
    drawGenerationService.generateDrawManual(tournament, List.of());
  }

  private Tournament baseTournamentKO(int nbPairs, int nbSeeds) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(nbPairs).nbSeeds(nbSeeds).format(TournamentFormat.KNOCKOUT).build());
    tournament.getPlayerPairs().addAll(TestFixturesApp.createPlayerPairs(4));
    return tournament;
  }
}
