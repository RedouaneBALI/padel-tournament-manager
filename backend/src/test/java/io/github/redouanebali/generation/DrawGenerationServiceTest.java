package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.service.DrawGenerationService;
import io.github.redouanebali.util.TestFixtures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // -------------------- capPairsToMax --------------------

  @Test
  @Disabled
  void capPairsToMax_truncates_whenAboveLimit() {
    Tournament t = new Tournament();
    // t.setNbMaxPairs(32);
    t.getPlayerPairs().addAll(TestFixtures.createPairs(36));

    List<PlayerPair> result = DrawGenerationService.capPairsToMax(t);
    assertEquals(32, result.size());
    assertEquals(1, result.get(0).getSeed());
    assertEquals(32, result.get(31).getSeed());
  }

  @Test
  @Disabled
  void capPairsToMax_noTruncate_whenUnderOrNoLimit() {
    Tournament t1 = new Tournament();
    //  t1.setNbMaxPairs(40);
    t1.getPlayerPairs().addAll(TestFixtures.createPairs(36));
    List<PlayerPair> r1 = DrawGenerationService.capPairsToMax(t1);
    assertEquals(36, r1.size());

    Tournament t2 = new Tournament();
    // t2.setNbMaxPairs(0);
    t2.getPlayerPairs().addAll(TestFixtures.createPairs(5));
    List<PlayerPair> r2 = DrawGenerationService.capPairsToMax(t2);
    assertEquals(5, r2.size());
  }

  @Test
  @Disabled
  void capPairsToMax_emptyList_returnsEmpty() {
    Tournament t = new Tournament();
    //  t.setNbMaxPairs(8);
    // no pairs added → empty list
    List<PlayerPair> result = DrawGenerationService.capPairsToMax(t);
    assertEquals(0, result.size());
  }

  @Test
  @Disabled
  void capPairsToMax_negativeLimit_noTruncate() {
    Tournament t = new Tournament();
    //   t.setNbMaxPairs(-1);
    t.getPlayerPairs().addAll(TestFixtures.createPairs(5));
    List<PlayerPair> result = DrawGenerationService.capPairsToMax(t);
    assertEquals(5, result.size());
  }

  // -------------------- generateDraw (KO) --------------------

  @Test
  void generateDraw_manual_knockout_populatesSemis_andSaves() {
    Tournament tournament = baseTournamentKO(4, 0);
    // tournament.setNbMaxPairs(4); // structure: SEMIS -> FINAL
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(4));
    // init structure with empty games
    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(structure);

    Tournament updated = drawGenerationService.generateDraw(tournament, true); // manual

    assertSame(tournament, updated);

    Round semis = findRound(updated, Stage.SEMIS);
    assertNotNull(semis);
    assertEquals(2, semis.getGames().size());

    // Manual draw must keep original order (1v2, 3v4 with sequential fill A then B)
    List<PlayerPair> pairs = new ArrayList<>(tournament.getPlayerPairs());
    Game             g1    = semis.getGames().get(0);
    Game             g2    = semis.getGames().get(1);
    assertSame(pairs.get(0), g1.getTeamA());
    assertSame(pairs.get(1), g1.getTeamB());
    assertSame(pairs.get(2), g2.getTeamA());
    assertSame(pairs.get(3), g2.getTeamB());
  }

  @Test
  void generateDraw_algorithmic_knockout_populatesSemis_andSaves() {
    Tournament tournament = baseTournamentKO(4, 0);
    //   tournament.setNbMaxPairs(4);
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(tournament);
    tournament.getRounds().clear();
    tournament.getRounds().addAll(structure);

    Tournament updated = drawGenerationService.generateDraw(tournament, false); // algorithmic

    assertSame(tournament, updated);

    Round semis = findRound(updated, Stage.SEMIS);
    assertNotNull(semis);
    assertEquals(2, semis.getGames().size());

    // We only check that both teams are assigned (placement may shuffle)
    for (Game g : semis.getGames()) {
      assertNotNull(g.getTeamA());
      assertNotNull(g.getTeamB());
    }
  }

  @Test
  void generateDraw_throws_whenStageMissing() {
    Tournament tournament = baseTournamentKO(4, 0);
    //tournament.setNbMaxPairs(4);
    tournament.getPlayerPairs().clear();
    tournament.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    // Do NOT create structure → service should fail to find matching round
    tournament.getRounds().clear();

    assertThrows(IllegalArgumentException.class, () -> drawGenerationService.generateDraw(tournament, true));
  }

  @Test
  void generateDraw_denied_whenNotOwnerOrSuperAdmin() {
    // current user is bali.redouane@gmail.com (set in setUp), set owner to someone else
    Tournament t = baseTournamentKO(4, 0);
    t.setOwnerId("not.me@example.com");
    t.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(structure);

    assertThrows(AccessDeniedException.class, () -> drawGenerationService.generateDraw(t, true));
  }

  @Test
  void generateDraw_allowed_forSuperAdmin_evenIfNotOwner() {
    // make current user a super-admin
    when(securityProps.getSuperAdmins()).thenReturn(Set.of("bali.redouane@gmail.com"));

    Tournament t = baseTournamentKO(4, 0);
    t.setOwnerId("someone@else"); // not owner, but super-admin
    t.getPlayerPairs().clear();
    t.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(structure);

    // should not throw
    Tournament updated = drawGenerationService.generateDraw(t, true);
    assertSame(t, updated);
  }

  @Test
  void generateDraw_savesTournament_once() {
    Tournament t = baseTournamentKO(4, 0);
    //   t.setNbMaxPairs(4);
    t.getPlayerPairs().clear();
    t.getPlayerPairs().addAll(TestFixtures.createPairs(4));

    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(structure);

    drawGenerationService.generateDraw(t, true);
    verify(tournamentRepository, times(1)).save(t);
  }

  @Test
  void generateDraw_respectsNbMaxPairs_limit_manual() {
    Tournament t = baseTournamentKO(4, 0);
    //   t.setNbMaxPairs(4); // limit to 4
    t.getPlayerPairs().clear();
    t.getPlayerPairs().addAll(TestFixtures.createPairs(6)); // 6 registered, should use first 4

    KnockoutRoundGenerator gen       = new KnockoutRoundGenerator(0);
    List<Round>            structure = gen.initRoundsAndGames(t);
    t.getRounds().clear();
    t.getRounds().addAll(structure);

    Tournament updated = drawGenerationService.generateDraw(t, true); // manual uses order
    Round      semis   = findRound(updated, Stage.SEMIS);
    assertNotNull(semis);
    assertEquals(2, semis.getGames().size());

    List<PlayerPair> pairs = new ArrayList<>(t.getPlayerPairs());
    Game             g1    = semis.getGames().get(0);
    Game             g2    = semis.getGames().get(1);

    // only first 4 pairs should be used in order
    assertSame(pairs.get(0), g1.getTeamA());
    assertSame(pairs.get(1), g1.getTeamB());
    assertSame(pairs.get(2), g2.getTeamA());
    assertSame(pairs.get(3), g2.getTeamB());
  }

  // -------------------- helpers --------------------

  private Tournament baseTournamentKO(int nbPairs, int nbSeeds) {
    Tournament t = new Tournament();
    t.setId(1L);
    t.setFormat(TournamentFormat.KNOCKOUT);
    t.setOwnerId("bali.redouane@gmail.com");
    t.setConfig(TournamentFormatConfig.builder().mainDrawSize(nbPairs).nbSeeds(nbSeeds).build());
    return t;
  }

  private Round findRound(Tournament t, Stage stage) {
    return t.getRounds().stream()
            .filter(r -> r.getStage() == stage)
            .findFirst()
            .orElse(null);
  }
}
