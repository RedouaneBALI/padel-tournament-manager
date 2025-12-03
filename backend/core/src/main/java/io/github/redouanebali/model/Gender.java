package io.github.redouanebali.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Gender {

  MEN("hommes"),
  WOMEN("femmes"),
  MIX("mixte");

  private final String label;

}
