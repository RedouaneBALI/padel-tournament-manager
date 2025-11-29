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

  @Override
  public String toString() {
    return getDisplay();
  }
}
