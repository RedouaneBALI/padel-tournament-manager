package io.github.redouanebali.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.redouanebali.PadelTournamentManagerApplication;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
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
    // Configuration du contexte de sécurité pour les tests avec JWT
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
    assertTrue(tournamentRepository.findById(id).isEmpty(), "Le tournoi doit être supprimé");
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
      fail("L'ajout de paires à un tournoi inexistant doit échouer");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}
