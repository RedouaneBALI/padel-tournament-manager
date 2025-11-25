package io.github.redouanebali.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.redouanebali.PadelTournamentManagerApplication;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = PadelTournamentManagerApplication.class)
@ActiveProfiles("h2")
@Transactional
class TournamentIntegrationTest {

  @Autowired
  private TournamentRepository tournamentRepository;
  @Autowired
  private TournamentService    tournamentService;
  @Autowired
  private PlayerPairService    playerPairService;

  @BeforeEach
  void setUp() {
    // Configure security context for tests using a fake JWT
    Jwt jwt = Jwt.withTokenValue("fake-token")
                 .header("alg", "none")
                 .claim("email", "io.github.redouanebali.api.integration@test.com")
                 .build();

    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(), "io.github.redouanebali.api.integration@test.com");
    SecurityContextHolder.getContext().setAuthentication(auth);
  }


  @Test
  void testDeleteTournament() {
    Tournament t = new Tournament();
    t.setOwnerId("io.github.redouanebali.api.integration@test.com");
    t.setName("Delete Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    Long       id    = saved.getId();
    assertNotNull(id);
    tournamentRepository.deleteById(id);
    assertTrue(tournamentRepository.findById(id).isEmpty(), "Tournament should be deleted");
  }

  @Test
  void testUpdateTournament() {
    Tournament t = new Tournament();
    t.setOwnerId("io.github.redouanebali.api.integration@test.com");
    t.setName("Update Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    Long       id    = saved.getId();
    assertNotNull(id);
    saved.setName("Updated Cup");
    Tournament updated = tournamentRepository.save(saved);
    assertEquals("Updated Cup", updated.getName());
  }

  @Test
  void testPersistenceTournament() {
    Tournament t = new Tournament();
    t.setOwnerId("io.github.redouanebali.api.integration@test.com");
    t.setName("Persistence Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    Long       id    = saved.getId();
    assertNotNull(id);
    Tournament found = tournamentRepository.findById(id).orElse(null);
    assertNotNull(found);
    assertEquals("Persistence Cup", found.getName());
  }

  @Test
  void testAddPairsToNonExistentTournament() {
    Long fakeId = 999999L;
    List<CreatePlayerPairRequest> pairs = List.of(
        new CreatePlayerPairRequest("Alice", "Bob", 1)
    );
    try {
      playerPairService.addPairs(fakeId, pairs);
      fail("Adding pairs to a non-existent tournament should fail");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  void testActiveTournamentsIncludeTournamentWithRealGame() {
    // Create a tournament active today
    Tournament t = new Tournament();
    t.setOwnerId("io.github.redouanebali.api.integration@test.com");
    t.setName("Active Cup");
    t.setStartDate(LocalDate.now());
    t.setEndDate(LocalDate.now().plusDays(1));
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());

    // Create a round with a real game (teamA assigned)
    Round      round = new Round(Stage.R32);
    PlayerPair pairA = new PlayerPair("A1", "A2", 0);
    PlayerPair pairB = new PlayerPair("B1", "B2", 0);
    round.addGame(pairA, pairB);

    t.getRounds().add(round);
    Tournament saved = tournamentRepository.save(t);

    // --- Negative cases: tournaments that SHOULD NOT be returned by getActiveTournaments() ---
    // 1) Future tournament (not active yet)
    Tournament futureT = new Tournament();
    futureT.setOwnerId("io.github.redouanebali.api.integration@test.com");
    futureT.setName("Future Cup");
    futureT.setStartDate(LocalDate.now().plusDays(2));
    futureT.setEndDate(LocalDate.now().plusDays(3));
    Round futRound = new Round(Stage.R32);
    futRound.addGame(new PlayerPair("F1", "F2", 0), new PlayerPair("F3", "F4", 0));
    futureT.getRounds().add(futRound);
    tournamentRepository.save(futureT);

    // 2) Past tournament (already finished)
    Tournament pastT = new Tournament();
    pastT.setOwnerId("io.github.redouanebali.api.integration@test.com");
    pastT.setName("Past Cup");
    pastT.setStartDate(LocalDate.now().minusDays(10));
    pastT.setEndDate(LocalDate.now().minusDays(1));
    Round pastRound = new Round(Stage.R32);
    pastRound.addGame(new PlayerPair("P1", "P2", 0), new PlayerPair("P3", "P4", 0));
    pastT.getRounds().add(pastRound);
    tournamentRepository.save(pastT);

    // 3) Active by date but rounds contain only games with NULL teams (should not be counted)
    Tournament emptyTeams = new Tournament();
    emptyTeams.setOwnerId("io.github.redouanebali.api.integration@test.com");
    emptyTeams.setName("Empty Teams Cup");
    emptyTeams.setStartDate(LocalDate.now());
    emptyTeams.setEndDate(LocalDate.now().plusDays(1));
    Round rEmpty = new Round(Stage.R32);
    // add a Game with no teams assigned
    rEmpty.addGame(new io.github.redouanebali.model.Game());
    emptyTeams.getRounds().add(rEmpty);
    tournamentRepository.save(emptyTeams);

    List<Tournament> active = tournamentService.getActiveTournaments();
    // Only the initially created tournament should be present
    boolean found = active.stream().anyMatch(tt -> tt.getId().equals(saved.getId()));
    assertTrue(found, "The created tournament with a real game should appear in active tournaments");

    // Ensure none of the negative-case tournaments are present
    boolean containsFuture     = active.stream().anyMatch(tt -> "Future Cup".equals(tt.getName()));
    boolean containsPast       = active.stream().anyMatch(tt -> "Past Cup".equals(tt.getName()));
    boolean containsEmptyTeams = active.stream().anyMatch(tt -> "Empty Teams Cup".equals(tt.getName()));

    assertFalse(containsFuture, "Future tournaments must not be returned");
    assertFalse(containsPast, "Past tournaments must not be returned");
    assertFalse(containsEmptyTeams, "Tournaments with only empty-team games must not be returned");
  }

}
