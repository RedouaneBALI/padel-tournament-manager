package io.github.redouanebali.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
