package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public abstract class AbstractRoundGenerator implements RoundGenerator {

  private final List<PlayerPair> pairs;
  private final int              nbSeeds;

}
