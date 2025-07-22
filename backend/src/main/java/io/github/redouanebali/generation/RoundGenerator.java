package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import java.util.List;

public interface RoundGenerator {

  Round generate(List<PlayerPair> pairs, int nbSeeds);
  
}
