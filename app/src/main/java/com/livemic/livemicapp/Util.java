package com.livemic.livemicapp;

import android.util.Log;

// Collection of convenient static functions.
public class Util {
  private static final double SCALE = (1.0 / (1 << 15));

  // PCM comes in bytes in a byte array - convert to [-1 -> 1 float]
  public static float pcmBytesToFloat(byte b1, byte b2) {
    int asInt = b1 + (b2 << 8);
    return (float)(asInt * SCALE);
  }

  // Convert milliseconds to something that looks like 11m40s
  public static String formatTalkTime(long durationMs) {
    int durationSec = (int)(durationMs / 1000);
    Log.i(Constants.TAG, "Duration sec: " + durationSec);
    if (durationSec < 60) {
      return durationSec + "s";
    }
    int ss = durationSec % 60;
    int mm = durationSec / 60;
    return String.format("%dm%02ds", mm, ss);
  }
}
