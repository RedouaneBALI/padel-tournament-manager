package io.github.redouanebali.model;

import lombok.Getter;

@Getter
public enum GamePoint {
  ZERO(0), QUINZE(15), TRENTE(30), QUARANTE(40), AVANTAGE(-1);

  private final int value;

  GamePoint(int value) {
    this.value = value;
  }

  public String getDisplay() {
    if (this == AVANTAGE) {
      return "A";
    }
    return String.valueOf(value);
  }

  /**
   * Returns the next game point in the progression. AVANTAGE stays at AVANTAGE (game won).
   */
  public GamePoint next() {
    return switch (this) {
      case ZERO -> QUINZE;
      case QUINZE -> TRENTE;
      case TRENTE -> QUARANTE;
      case QUARANTE, AVANTAGE -> AVANTAGE;
    };
  }

  /**
   * Returns the previous game point (used for undo). ZERO stays at ZERO.
   */
  public GamePoint previous() {
    return switch (this) {
      case AVANTAGE -> QUARANTE;
      case QUARANTE -> TRENTE;
      case TRENTE -> QUINZE;
      case QUINZE, ZERO -> ZERO;
    };
  }

  @Override
  public String toString() {
    return getDisplay();
  }
}
