package com.livemic.livemicapp.pipes.wifi;

import com.livemic.livemicapp.pipes.AudioSource;

/** Receives Audio over WiFi and stream that locally to the others connected. */
public class WiFiDirectSource extends AudioSource {
  // TODO - implement... need to call handleNewSamples on each packet that comes in.

  public void stop() {
    // TODO
  }

  public void updateWithRemoteSamples(byte[] samples) {
    handleNewSamples(samples);
  }
}
