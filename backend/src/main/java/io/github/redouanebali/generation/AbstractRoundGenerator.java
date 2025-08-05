package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public abstract class AbstractRoundGenerator implements RoundGenerator {

  private final List<PlayerPair> pairs = new ArrayList<>();
  private final int              nbSeeds;

  public void addPairs(List<PlayerPair> pairs) {
    this.pairs.addAll(pairs);
  }

}
