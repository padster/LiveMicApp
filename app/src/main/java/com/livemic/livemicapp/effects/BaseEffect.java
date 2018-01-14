package com.livemic.livemicapp.effects;

import com.livemic.livemicapp.Util;

// Logic shared by effects to convert it into floats and back.
public abstract class BaseEffect {
  public byte[] newSampleBytes(byte[] oldSamples) {
    float[] samples = new float[oldSamples.length / 2];
    for (int i = 0; i < samples.length; i++) {
      samples[i] = Util.pcmBytesToFloat(oldSamples[2 * i], oldSamples[2 * i + 1]);
    }
    float[] changed = newSamples(samples);
    byte[] result = new byte[oldSamples.length];
    for (int i = 0; i < changed.length; i++) {
      byte[] pair = Util.floatToPcmBytes(changed[i]);
      result[2 * i    ] = pair[0];
      result[2 * i + 1] = pair[1];
    }
    return result;
  }

  protected abstract float[] newSamples(float[] oldSamples);
}
