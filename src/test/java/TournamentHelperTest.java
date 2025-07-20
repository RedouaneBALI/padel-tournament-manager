import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.RoundInfo;
import io.github.redouanebali.model.TournamentHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TournamentHelperTest {

  static Stream<Arguments> provideTournamentCases() {
    return Stream.of(
        Arguments.of(0, 0, 0, 0, 0, 8, 8),
        Arguments.of(0, 0, 0, 0, 8, 4, 12),
        Arguments.of(0, 0, 0, 0, 16, 0, 16),
        Arguments.of(0, 0, 0, 16, 8, 0, 24),
        Arguments.of(0, 0, 0, 32, 0, 0, 32),
        Arguments.of(0, 0, 8, 28, 0, 0, 36),
        Arguments.of(0, 0, 16, 24, 0, 0, 40),
        Arguments.of(0, 0, 20, 22, 0, 0, 42),
        Arguments.of(0, 0, 32, 16, 0, 0, 48),
        Arguments.of(0, 0, 40, 12, 0, 0, 52),
        Arguments.of(0, 0, 64, 0, 0, 0, 64),
        Arguments.of(0, 12, 58, 0, 0, 0, 70),
        Arguments.of(0, 128, 0, 0, 0, 0, 128),
        Arguments.of(256, 0, 0, 0, 0, 0, 256)
    );
  }

  @ParameterizedTest(name = "Teams: {6} → Q1(R256): {0}, Q2(R128): {1}, R64: {2}, R32: {3}, R16: {4}, QUARTERS: {5}")
  @MethodSource("provideTournamentCases")
  public void printTournamentResult(
      int expectedQ1, int expectedQ2, int expectedR64, int expectedR32, int expectedR16, int expectedQuarters, int nbPairs) {

    List<PlayerPair> allPairs = new ArrayList<>();
    IntStream.rangeClosed(1, nbPairs).forEach(seed -> {
      Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
      Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
      allPairs.add(new PlayerPair(player1, player2, seed));
    });

    List<Round> result = TournamentHelper.createNewTournament(allPairs);

    int q1Teams       = getTeamsForRound(result, RoundInfo.Q1);         // R256
    int q2Teams       = getTeamsForRound(result, RoundInfo.Q2);         // R128
    int r64Teams      = getTeamsForRound(result, RoundInfo.R64);
    int r32Teams      = getTeamsForRound(result, RoundInfo.R32);
    int r16Teams      = getTeamsForRound(result, RoundInfo.R16);
    int quartersTeams = getTeamsForRound(result, RoundInfo.QUARTERS);

    assertEquals(expectedQ1, q1Teams, "Q1(R256) should contain " + expectedQ1 + " teams");
    assertEquals(expectedQ2, q2Teams, "Q2(R128) should contain " + expectedQ2 + " teams");
    assertEquals(expectedR64, r64Teams, "R64 should contain " + expectedR64 + " teams");
    assertEquals(expectedR32, r32Teams, "R32 should contain " + expectedR32 + " teams");
    assertEquals(expectedR16, r16Teams, "R16 should contain " + expectedR16 + " teams");
    assertEquals(expectedQuarters, quartersTeams, "QUARTERS should contain " + expectedQuarters + " teams");

    // All other rounds should have 0 teams assigned
    result.stream()
          .filter(r -> !List.of(RoundInfo.Q1, RoundInfo.Q2, RoundInfo.R64, RoundInfo.R32, RoundInfo.R16, RoundInfo.QUARTERS).contains(r.getInfo()))
          .forEach(r -> assertEquals(0, r.getPlayerPairs().size(), r.getInfo().name() + " should contain 0 teams"));

    // Optional: print result for visual confirmation
    for (Round round : result) {
      System.out.println(round.getInfo().getLabel() + " -> " + round.getPlayerPairs().size() + " teams");
    }
  }

  private int getTeamsForRound(List<Round> rounds, RoundInfo info) {
    return rounds.stream()
                 .filter(r -> r.getInfo() == info)
                 .mapToInt(r -> r.getPlayerPairs().size())
                 .sum();
  }
}