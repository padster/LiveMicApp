package com.livemic.livemicapp;

// Collection of convenient static functions.
public class Util {
  private static final double SCALE = (1.0 / (1 << 15));

  // PCM comes in bytes in a byte array - convert to [-1 -> 1 float]
  public static float pcmBytesToFloat(byte b1, byte b2) {
    int asInt = b1 + (b2 << 8);
    return (float)(asInt * SCALE);
  }
}
