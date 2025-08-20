package io.github.redouanebali.model.format;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class DrawMath {
  
  public static boolean isPowerOfTwo(int n) {
    return n > 0 && (n & (n - 1)) == 0;
  }
}