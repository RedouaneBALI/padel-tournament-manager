package io.github.redouanebali.model.format;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class DrawMath {

  public static boolean isPowerOfTwo(int n) {
    return n > 0 && (n & (n - 1)) == 0;
  }


  public static int nextPowerOfTwo(int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("Input must be positive");
    }
    if (isPowerOfTwo(n)) {
      return n;
    }
    int power = 1;
    while (power < n) {
      power <<= 1;
    }
    return power;
  }
}