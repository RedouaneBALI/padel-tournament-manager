package io.github.redouanebali.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Gender {

  MEN("hommes"),
  WOMEN("femmes"),
  MIX("mixte");

  private String label;

}
