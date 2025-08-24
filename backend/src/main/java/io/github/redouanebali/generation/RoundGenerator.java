package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface RoundGenerator {

  List<Round> generateAlgorithmicRounds(List<PlayerPair> pairs);

  List<Round> generateManualRounds(List<PlayerPair> pairs);

  List<Round> generateRounds(Tournament tournament, List<PlayerPair> pairs, boolean manual);

  // List<Round> createRoundsStructure(Tournament tournament);
}
