package com.livemic.livemicapp.pipes;

import com.livemic.livemicapp.Util;

// Stores recent samples in a circular buffer, as -1 -> 1 floats.
public class RecentSamplesBuffer implements AudioSink {
  private final float[] buffer;
  private final int downsampleRate;
  private final Runnable onUpdateCallback;
  private int at = 0;
  private int downsampleLeft;

  /**
   * @param sampleCount How many samples of buffer to store.
   * @param downsampleRate Only use 1/n of samples, not all are really needed.
   * @param onUpdateCallback Called whenever a new batch of audio has been processed.
   */
  public RecentSamplesBuffer(int sampleCount, int downsampleRate, Runnable onUpdateCallback) {
    buffer = new float[sampleCount];
    this.downsampleRate = downsampleRate;
    this.onUpdateCallback = onUpdateCallback;
    downsampleLeft = downsampleRate;
  }

  // Get most recent snapshot, from oldest to latest.
  public float[] getSamples() {
    float[] result = new float[buffer.length];
    int copyFrom = at;
    for (int i = 0; i < result.length; i++) {
      result[i] = buffer[copyFrom];
      copyFrom++;
      if (copyFrom == buffer.length) {
        copyFrom = 0;
      }
    }
    return result;
  }

  @Override
  public void newSamples(byte[] samples) {
    for (int at = 0; at < samples.length; at += 2) {
      appendSample(Util.pcmBytesToFloat(samples[at], samples[at + 1]));
    }
    onUpdateCallback.run();
  }

  // Add [-1 -> 1] sample, but only a subset to allow us to downsample.
  private void appendSample(float sample) {
    downsampleLeft--;
    if (downsampleLeft == 0) {
      buffer[at] = sample;
      at = at + 1;
      if (at == buffer.length) {
        at = 0;
      }
      downsampleLeft = this.downsampleRate;
    }
  }
}
