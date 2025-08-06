package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;
import java.util.Set;

public interface RoundGenerator {

  Round generate(List<PlayerPair> pairs);

  Set<Round> createRounds(Tournament tournament);
}
