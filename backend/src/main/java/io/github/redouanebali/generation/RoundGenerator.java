package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface RoundGenerator {

  Round generateAlgorithmicRound(List<PlayerPair> pairs);

  Round generateManualRound(List<PlayerPair> pairs);

  Round generateRound(Tournament tournament, List<PlayerPair> pairs, boolean manual);

  List<Round> createRoundsStructure(Tournament tournament);
}
