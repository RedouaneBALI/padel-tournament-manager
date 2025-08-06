package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface RoundGenerator {

  Round generateAlgorithmicRound(List<PlayerPair> pairs);

  Round generateManualRound(List<PlayerPair> pairs);

  List<Round> initRoundsAndGames(Tournament tournament);
}
