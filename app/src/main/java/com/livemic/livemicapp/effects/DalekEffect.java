package com.livemic.livemicapp.effects;

import com.livemic.livemicapp.Constants;

// Ring Modulator with 20hz
public class DalekEffect extends BaseEffect {
  private static final double HZ = 40.0;
  private static final double SECONDS_PER_CYCLE = 1.0 / Constants.SAMPLE_RATE_LOCAL_HZ;

  private static final double TIME_DELTA = HZ * SECONDS_PER_CYCLE;
  double at = 0.0;

  @Override
  protected float[] newSamples(float[] oldSamples) {
    int n = oldSamples.length;
    float[] result = new float[n];
    for (int i = 0; i < n; i++) {
      float scale = gen20hzSample();
      result[i] = oldSamples[i] * scale;
    }
    return result;
  }

  private float gen20hzSample() {
    double result = Math.sin(at * 2.0 * Math.PI);
    at = (at + TIME_DELTA) % 1.0;
    return (float)result;
  }
}
