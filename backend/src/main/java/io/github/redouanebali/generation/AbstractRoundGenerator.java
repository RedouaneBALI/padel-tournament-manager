package io.github.redouanebali.generation;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public abstract class AbstractRoundGenerator implements RoundGenerator {

  private final int nbSeeds;


}
