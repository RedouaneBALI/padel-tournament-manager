package io.github.redouanebali.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class PlayerPairServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;

  @Mock
  private io.github.redouanebali.security.AuthorizationService authorizationService;

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
    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, pairs);

    assertEquals(2, updated.getPlayerPairs().size());
  }

  @Test
  void testAddPairs_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, this::addPairsToTournament);
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

    List<PlayerPair> result = playerPairService.getPairsByTournamentId(1L, false, false);
    assertEquals(1, result.size());
  }

  @Test
  void testGetPairsByTournamentId_shouldThrowIfTournamentNotFound() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.getPairsByTournamentId(1L, false, false));
  }

  @Test
  void testUpdatePlayerPair_shouldUpdateNamesAndSeed() {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair = new PlayerPair("Old1", "Old2", 1);
    pair.setId(10L);
    tournament.getPlayerPairs().add(pair);

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
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

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));

    assertThrows(IllegalStateException.class,
                 () -> playerPairService.updatePlayerPair(1L, 11L, "Someone", null, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @CsvSource({
      "KNOCKOUT, 64, 40, 24",  // mainDrawSize=8, pairs=6, byes=2
      "KNOCKOUT, 32, 30, 2", // mainDrawSize=16, pairs=10, byes=6
      "KNOCKOUT, 32, 32, 0"   // mainDrawSize=4, pairs=4, byes=0
  })
  void testAddPairs_shouldAddByesForKnockoutToReachMainDrawSize(io.github.redouanebali.model.format.TournamentFormat format,
                                                                int mainDrawSize, int initialPairs, int expectedByes) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setConfig(TournamentConfig.builder().format(format).mainDrawSize(mainDrawSize).build());

    List<CreatePlayerPairRequest> requests = java.util.stream.IntStream.range(0, initialPairs)
                                                                       .mapToObj(i -> new CreatePlayerPairRequest("P" + i + "A",
                                                                                                                  "P" + i + "B",
                                                                                                                  i + 1))
                                                                       .toList();

    when(tournamentMapper.toPlayerPairList(requests)).thenReturn(
        requests.stream().map(r -> new PlayerPair(r.getPlayer1Name(), r.getPlayer2Name(), r.getSeed())).toList()
    );
    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, requests);

    long byeCount = updated.getPlayerPairs().stream().filter(PlayerPair::isBye).count();
    assertEquals(expectedByes, byeCount);
    assertEquals(initialPairs + expectedByes, updated.getPlayerPairs().size());
  }

  @ParameterizedTest
  @CsvSource({
      "QUALIF_KO, 8, 32, 4, 36, 0",  // preQual=8, main=32, nbQualifiers=4, pairs=36, byes=0 (total required=8+(32-4)=36)
      "QUALIF_KO, 4, 16, 2, 18, 0",  // preQual=4, main=16, nbQualifiers=2, pairs=18, byes=0 (total=4+(16-2)=18)
      "QUALIF_KO, 16, 64, 8, 72, 0", // preQual=16, main=64, nbQualifiers=8, pairs=72, byes=0 (total=16+(64-8)=72)
      "QUALIF_KO, 4, 16, 2, 16, 2",  // preQual=4, main=16, nbQualifiers=2, pairs=16, byes=2 (total=18)
      "QUALIF_KO, 8, 32, 4, 34, 2",  // preQual=8, main=32, nbQualifiers=4, pairs=34, byes=2 (total=36)
      "QUALIF_KO, 16, 64, 8, 70, 2"  // preQual=16, main=64, nbQualifiers=8, pairs=70, byes=2 (total=72)
  })
  void testAddPairs_shouldAddByesForQualifKoToReachTotalDrawSize(TournamentFormat format,
                                                                 int preQualDrawSize,
                                                                 int mainDrawSize,
                                                                 int nbQualifiers,
                                                                 int initialPairs,
                                                                 int expectedByes) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setConfig(TournamentConfig.builder()
                                         .format(format)
                                         .preQualDrawSize(preQualDrawSize)
                                         .mainDrawSize(mainDrawSize)
                                         .nbQualifiers(nbQualifiers)
                                         .build());

    List<CreatePlayerPairRequest> requests = java.util.stream.IntStream.range(0, initialPairs)
                                                                       .mapToObj(i -> new CreatePlayerPairRequest("P" + i + "A",
                                                                                                                  "P" + i + "B",
                                                                                                                  i + 1))
                                                                       .toList();

    when(tournamentMapper.toPlayerPairList(requests)).thenReturn(
        requests.stream().map(r -> new PlayerPair(r.getPlayer1Name(), r.getPlayer2Name(), r.getSeed())).toList()
    );
    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, requests);

    long byeCount       = updated.getPlayerPairs().stream().filter(PlayerPair::isBye).count();
    long qualifierCount = updated.getPlayerPairs().stream().filter(PlayerPair::isQualifier).count();
    assertEquals(expectedByes, byeCount);
    assertEquals(nbQualifiers, qualifierCount);
    assertEquals(initialPairs + expectedByes + nbQualifiers, updated.getPlayerPairs().size());
  }

  @ParameterizedTest
  @CsvSource({
      "GROUPS_KO, 4, 3, 10, 2",  // nbPools=4, nbPairsPerPool=3, pairs=10, byes=2 (total required=12)
      "GROUPS_KO, 2, 4, 8, 0",   // nbPools=2, nbPairsPerPool=4, pairs=8, byes=0
      "GROUPS_KO, 3, 2, 5, 1"    // nbPools=3, nbPairsPerPool=2, pairs=5, byes=1 (total=6)
  })
  void testAddPairs_shouldAddByesForGroupsKoToReachPoolSize(io.github.redouanebali.model.format.TournamentFormat format,
                                                            int nbPools, int nbPairsPerPool, int initialPairs, int expectedByes) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");
    tournament.setConfig(TournamentConfig.builder().format(format).nbPools(nbPools).nbPairsPerPool(nbPairsPerPool).build());

    List<CreatePlayerPairRequest> requests = java.util.stream.IntStream.range(0, initialPairs)
                                                                       .mapToObj(i -> new CreatePlayerPairRequest("P" + i + "A",
                                                                                                                  "P" + i + "B",
                                                                                                                  i + 1))
                                                                       .toList();

    when(tournamentMapper.toPlayerPairList(requests)).thenReturn(
        requests.stream().map(r -> new PlayerPair(r.getPlayer1Name(), r.getPlayer2Name(), r.getSeed())).toList()
    );
    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tournament updated = playerPairService.addPairs(1L, requests);

    long byeCount = updated.getPlayerPairs().stream().filter(PlayerPair::isBye).count();
    assertEquals(expectedByes, byeCount);
    assertEquals(initialPairs + expectedByes, updated.getPlayerPairs().size());
  }

  @Test
  void testReorderPlayerPairs_shouldPreserveExactOrderIncludingByeAndQualifier() {
    // Given: A tournament with 5 pairs including normal pairs, BYE, and QUALIFIER
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair1 = new PlayerPair("Alice", "Bob", 1);
    pair1.setId(10L);

    PlayerPair pair2 = new PlayerPair("Charlie", "Dave", 2);
    pair2.setId(20L);

    PlayerPair byePair = PlayerPair.bye();
    byePair.setId(30L);

    PlayerPair pair3 = new PlayerPair("Eve", "Frank", 3);
    pair3.setId(40L);

    PlayerPair qualifierPair = PlayerPair.qualifier(1);
    qualifierPair.setId(50L);

    // Original order: pair1, pair2, BYE, pair3, QUALIFIER
    tournament.getPlayerPairs().add(pair1);
    tournament.getPlayerPairs().add(pair2);
    tournament.getPlayerPairs().add(byePair);
    tournament.getPlayerPairs().add(pair3);
    tournament.getPlayerPairs().add(qualifierPair);

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // When: Reorder to: pair3, BYE, pair1, QUALIFIER, pair2
    List<Long> newOrder = List.of(40L, 30L, 10L, 50L, 20L);
    playerPairService.reorderPlayerPairs(1L, newOrder);

    // Then: Verify the exact new order is preserved
    List<PlayerPair> reordered = tournament.getPlayerPairs();
    assertEquals(5, reordered.size());

    assertEquals(40L, reordered.get(0).getId()); // pair3 (Eve/Frank)
    assertEquals("Eve", reordered.get(0).getPlayer1().getName());

    assertEquals(30L, reordered.get(1).getId()); // BYE
    assertTrue(reordered.get(1).isBye());

    assertEquals(10L, reordered.get(2).getId()); // pair1 (Alice/Bob)
    assertEquals("Alice", reordered.get(2).getPlayer1().getName());

    assertEquals(50L, reordered.get(3).getId()); // QUALIFIER
    assertTrue(reordered.get(3).isQualifier());

    assertEquals(20L, reordered.get(4).getId()); // pair2 (Charlie/Dave)
    assertEquals("Charlie", reordered.get(4).getPlayer1().getName());
  }

  @Test
  void testReorderPlayerPairs_shouldThrowIfMissingPairIds() {
    // Given: A tournament with 3 pairs
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair1 = new PlayerPair("Alice", "Bob", 1);
    pair1.setId(10L);

    PlayerPair pair2 = new PlayerPair("Charlie", "Dave", 2);
    pair2.setId(20L);

    PlayerPair pair3 = new PlayerPair("Eve", "Frank", 3);
    pair3.setId(30L);

    tournament.getPlayerPairs().add(pair1);
    tournament.getPlayerPairs().add(pair2);
    tournament.getPlayerPairs().add(pair3);

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));

    // When: Try to reorder with only 2 IDs (missing one)
    List<Long> incompleteOrder = List.of(10L, 20L);

    // Then: Should throw IllegalArgumentException
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.reorderPlayerPairs(1L, incompleteOrder));
  }

  @Test
  void testReorderPlayerPairs_shouldThrowIfExtraPairIds() {
    // Given: A tournament with 2 pairs
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair1 = new PlayerPair("Alice", "Bob", 1);
    pair1.setId(10L);

    PlayerPair pair2 = new PlayerPair("Charlie", "Dave", 2);
    pair2.setId(20L);

    tournament.getPlayerPairs().add(pair1);
    tournament.getPlayerPairs().add(pair2);

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));

    // When: Try to reorder with 3 IDs (extra one)
    List<Long> orderWithExtra = List.of(10L, 20L, 99L);

    // Then: Should throw IllegalArgumentException
    assertThrows(IllegalArgumentException.class,
                 () -> playerPairService.reorderPlayerPairs(1L, orderWithExtra));
  }

  @Test
  void testReorderPlayerPairs_shouldWorkWithOnlyNormalPairs() {
    // Given: A tournament with only normal pairs (no BYE, no QUALIFIER)
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setOwnerId("bali.redouane@gmail.com");

    PlayerPair pair1 = new PlayerPair("Alice", "Bob", 1);
    pair1.setId(10L);

    PlayerPair pair2 = new PlayerPair("Charlie", "Dave", 2);
    pair2.setId(20L);

    PlayerPair pair3 = new PlayerPair("Eve", "Frank", 3);
    pair3.setId(30L);

    // Original order: pair1, pair2, pair3
    tournament.getPlayerPairs().add(pair1);
    tournament.getPlayerPairs().add(pair2);
    tournament.getPlayerPairs().add(pair3);

    when(tournamentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(tournament));
    when(tournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // When: Reorder to: pair3, pair1, pair2
    List<Long> newOrder = List.of(30L, 10L, 20L);
    playerPairService.reorderPlayerPairs(1L, newOrder);

    // Then: Verify the new order
    List<PlayerPair> reordered = tournament.getPlayerPairs();
    assertEquals(3, reordered.size());
    assertEquals(30L, reordered.get(0).getId());
    assertEquals(10L, reordered.get(1).getId());
    assertEquals(20L, reordered.get(2).getId());
  }
}
