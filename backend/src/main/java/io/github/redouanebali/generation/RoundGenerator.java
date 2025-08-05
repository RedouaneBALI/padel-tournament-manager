package io.github.redouanebali.generation;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.Set;

public interface RoundGenerator {

  Round generate();

  Set<Round> createRounds(Tournament tournament);
}
