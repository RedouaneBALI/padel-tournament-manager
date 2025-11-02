package io.github.redouanebali.generation.draw;

import io.github.redouanebali.model.format.DrawMode;

/**
 * Factory pour créer les stratégies de tirage. Seul le mode MANUAL est supporté.
 */
public class DrawStrategyFactory {

  private DrawStrategyFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static DrawStrategy createStrategy(DrawMode mode) {
    // Seul le mode MANUAL est supporté
    return new ManualDrawStrategy();
  }
}
