package io.github.redouanebali.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.redouanebali.PadelTournamentManagerApplication;
import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
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
public class TournamentIntegrationTest {

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
                 .claim("email", "integration@test.com")
                 .build();

    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(), "integration@test.com");
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void testFullTournamentLifecycle() {
    // Création d'un tournoi
    Tournament t = new Tournament();
    t.setOwnerId("integration@test.com");
    t.setName("Integration Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    assertNotNull(saved.getId());

    // Ajout de paires
    List<CreatePlayerPairRequest> pairs = List.of(
        new CreatePlayerPairRequest("Alice", "Bob", 1),
        new CreatePlayerPairRequest("Charlie", "Dave", 2),
        new CreatePlayerPairRequest("Eve", "Frank", 3),
        new CreatePlayerPairRequest("Grace", "Heidi", 4)
    );
    Tournament updated = playerPairService.addPairs(saved.getId(), pairs);
    assertEquals(4, updated.getPlayerPairs().size());

    // Génération du tableau
    Tournament withDraw = tournamentService.generateDrawAuto(saved.getId());
    assertNotNull(withDraw.getRounds());
    assertTrue(withDraw.getRounds().stream().anyMatch(r -> r.getStage() == Stage.SEMIS));
    // Vérification des matchs générés
    Round semis = withDraw.getRounds().stream().filter(r -> r.getStage() == Stage.SEMIS).findFirst().orElse(null);
    assertNotNull(semis);
    assertEquals(2, semis.getGames().size());
  }

  @Test
  void testTournamentWithoutPlayers() {
    // Création d'un tournoi sans joueurs
    Tournament t = new Tournament();
    t.setOwnerId("integration@test.com");
    t.setName("No Players Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    assertNotNull(saved.getId());

    // Tentative de génération du tableau
    Tournament withDraw = tournamentService.generateDrawAuto(saved.getId());
    assertNotNull(withDraw.getRounds());
    boolean allGamesEmpty = withDraw.getRounds().stream()
                                    .flatMap(r -> r.getGames().stream())
                                    .allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
    assertTrue(allGamesEmpty, "Tous les matchs doivent être vides si aucun joueur n'est inscrit");
  }

  @Test
  void testTournamentWithInsufficientPlayers() {
    // Création d'un tournoi avec un nombre insuffisant de joueurs
    Tournament t = new Tournament();
    t.setOwnerId("integration@test.com");
    t.setName("Insufficient Players Cup");
    t.setConfig(TournamentConfig.builder().mainDrawSize(4).nbSeeds(0).format(TournamentFormat.KNOCKOUT).build());
    Tournament saved = tournamentRepository.save(t);
    assertNotNull(saved.getId());

    // Ajout de quelques paires seulement
    List<CreatePlayerPairRequest> pairs = List.of(
        new CreatePlayerPairRequest("Alice", "Bob", 1),
        new CreatePlayerPairRequest("Charlie", "Dave", 2)
    );
    Tournament updated   = playerPairService.addPairs(saved.getId(), pairs);
    long       realPairs = updated.getPlayerPairs().stream().filter(p -> !p.isBye()).count();
    assertEquals(2, realPairs);

    // Tentative de génération du tableau
    Tournament withDraw = tournamentService.generateDrawAuto(saved.getId());
    assertNotNull(withDraw.getRounds());
    // Vérifier que les matchs comportent bien les paires réelles et des BYE
    boolean hasByeGames = withDraw.getRounds().stream()
                                  .flatMap(r -> r.getGames().stream())
                                  .anyMatch(g -> (g.getTeamA() != null && g.getTeamA().isBye()) || (g.getTeamB() != null && g.getTeamB().isBye()));
    assertTrue(hasByeGames, "Le tableau doit contenir des matchs avec des BYE si le nombre de joueurs est insuffisant");
  }

  @Test
  void testDeleteTournament() {
    Tournament t = new Tournament();
    t.setOwnerId("integration@test.com");
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
    t.setOwnerId("integration@test.com");
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
    t.setOwnerId("integration@test.com");
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
