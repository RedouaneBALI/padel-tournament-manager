import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.generation.GroupRoundGenerator;
import io.github.redouanebali.model.Group;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class GroupRoundGeneratorTest {

  private GroupRoundGenerator generator;

  @ParameterizedTest
  @CsvSource({
      "6,2,3,6",   // 2 groups of 3 => 3 games per group = 3*2/2 = 3 games per group, total = 6
      "12,3,4,18", // 3 groups of 4 => 6 games per group = 4*3/2 = 6, total = 18
      "16,4,4,24", // 4 groups of 4 => 6 games per group, total = 24
      "20,5,4,30", // 5 groups of 4 => 6 games per group, total = 30
      "18,6,3,18"  // 6 groups of 3 => 3 games per group, total = 18
  })
  public void checkPoolGeneration(int nbPairs, int expectedGroups, int expectedPairsPerGroup, int expectedNbGames) {
    generator = new GroupRoundGenerator(createPairs(nbPairs), 0, expectedGroups, expectedPairsPerGroup);
    Round round = generator.generate();

    assertEquals(expectedGroups, round.getGroups().size());
    for (Group group : round.getGroups()) {
      System.out.println(group);
      assertEquals(expectedPairsPerGroup, group.getPairs().size());
    }

    assertEquals(expectedNbGames, round.getGames().size());
  }

  private List<PlayerPair> createPairs(int count) {
    List<PlayerPair> pairs = new ArrayList<>();
    IntStream.rangeClosed(1, count).forEach(seed -> {
      Player player1 = new Player((long) seed, "Player" + seed + "A", seed, 0, 1990);
      Player player2 = new Player((long) seed + 100, "Player" + seed + "B", seed, 0, 1990);
      pairs.add(new PlayerPair(-1L, player1, player2, seed));
    });
    return pairs;
  }
}