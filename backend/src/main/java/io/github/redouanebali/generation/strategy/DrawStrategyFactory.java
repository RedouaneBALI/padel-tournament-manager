package io.github.redouanebali.generation.strategy;

import io.github.redouanebali.model.format.DrawMode;

public class DrawStrategyFactory {

  public static DrawStrategy createStrategy(DrawMode mode) {
    return switch (mode) {
      case SEEDED -> new AutomaticDrawStrategy();
      case MANUAL -> new ManualDrawStrategy();
    };
  }
}
