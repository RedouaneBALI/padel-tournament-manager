package io.github.redouanebali.generation.draw;

import io.github.redouanebali.model.format.DrawMode;

public class DrawStrategyFactory {

  private DrawStrategyFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static DrawStrategy createStrategy(DrawMode mode) {
    return switch (mode) {
      case SEEDED -> new AutomaticDrawStrategy();
      case MANUAL -> new ManualDrawStrategy();
    };
  }
}
