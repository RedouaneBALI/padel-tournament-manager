package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
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
  private io.github.redouanebali.security.SecurityProps securityProps;

  @InjectMocks
  private PlayerPairService playerPairService;

  @Mock
  private TournamentMapper tournamentMapper;

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
    tournament.setConfig(TournamentConfig.builder().mainDrawSize(2).build());

    CreatePlayerPairRequest       pp1   = new CreatePlayerPairRequest("Alice", "Bob", 1);
    CreatePlayerPairRequest       pp2   = new CreatePlayerPairRequest("Charlie", "Dave", 2);
    List<CreatePlayerPairRequest> pairs = List.of(pp1, pp2);

    when(tournamentMapper.toPlayerPairList(pairs)).thenReturn(
        List.of(new PlayerPair("Alice", "Bob", 1), new PlayerPair("Charlie", "Dave", 2))
    );
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, pairs);

    assertEquals(2, updated.getPlayerPairs().size());
  }

  @Test
  void testAddPairs_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> addPairsToTournament());
  }

  private void addPairsToTournament() {
    playerPairService.addPairs(1L, List.of());
  }

  @Test
  void testGetPairsByTournamentId_shouldReturnPairs() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    PlayerPair pair = new PlayerPair("A1", "A2", 0);
    tournament.getPlayerPairs().add(pair);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    List<PlayerPair> result = playerPairService.getPairsByTournamentId(1L, false);
    assertEquals(1, result.size());
  }

  @Test
  void testGetPairsByTournamentId_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.getPairsByTournamentId(1L, false));
  }

  @Test
  void testUpdatePlayerPair_shouldUpdateNamesAndSeed() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair = new PlayerPair("Old1", "Old2", 1);
    pair.setId(10L);
    tournament.getPlayerPairs().add(pair);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    playerPairService.updatePlayerPair(1L, 10L, "New1", "New2", 5);

    PlayerPair updated = tournament.getPlayerPairs().getFirst();
    assertEquals("New1", updated.getPlayer1().getName());
    assertEquals("New2", updated.getPlayer2().getName());
    assertEquals(5, updated.getSeed());
  }

  @Test
  void testUpdatePlayerPair_shouldThrowOnByePair() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair byePair = PlayerPair.bye();
    byePair.setId(11L);
    tournament.getPlayerPairs().add(byePair);

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertThrows(IllegalStateException.class,
                 () -> playerPairService.updatePlayerPair(1L, 11L, "Someone", null, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
