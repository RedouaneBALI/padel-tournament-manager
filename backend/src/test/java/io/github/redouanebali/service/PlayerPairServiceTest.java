package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.Collections;
import java.util.List;
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
public class PlayerPairServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @Mock
  private io.github.redouanebali.config.SecurityProps securityProps;

  @InjectMocks
  private PlayerPairService playerPairService;

  @BeforeEach
  void setUp() {
    Jwt jwt = Jwt.withTokenValue("fake")
                 .header("alg", "none")
                 .claim("email", "bali.redouane@gmail.com")
                 .build();

    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList(), "bali.redouane@gmail.com");
    SecurityContextHolder.getContext().setAuthentication(auth);

    // super-admins vide par défaut pour les tests
    org.mockito.Mockito.lenient().when(securityProps.getSuperAdmins()).thenReturn(Collections.emptySet());
  }

  @Test
  void testAddPairs_shouldUpdateTournamentWithNewPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair       pp1   = new PlayerPair("Alice", "Bob", 1);
    PlayerPair       pp2   = new PlayerPair("Charlie", "Dave", 2);
    List<PlayerPair> pairs = List.of(pp1, pp2);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, pairs);

    assertEquals(2, updated.getPlayerPairs().size());
  }

  @Test
  void testAddPairs_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.addPairs(1L, List.of()));
  }

  @Test
  void testGetPairsByTournamentId_shouldReturnPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    PlayerPair pair = new PlayerPair("A1", "A2", 0);
    tournament.getPlayerPairs().add(pair);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    List<PlayerPair> result = playerPairService.getPairsByTournamentId(1L);
    assertEquals(1, result.size());
  }

  @Test
  void testGetPairsByTournamentId_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.getPairsByTournamentId(1L));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
