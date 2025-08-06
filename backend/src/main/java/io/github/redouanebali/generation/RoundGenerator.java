package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface RoundGenerator {

  Round generate(List<PlayerPair> pairs);

  List<Round> createRounds(Tournament tournament);
}
