package com.livemic.livemicapp.pipes.wifi;

import com.livemic.livemicapp.pipes.AudioSink;


/**
 * Sink that takes samples coming in and sends them out over WiFi direct.
 */
public class WiFiDirectSink implements AudioSink {
  @Override
  public void newSamples(byte[] samples) {
    // TODO - send over wifi.
  }

  public void stop() {
    // TODO
  }
}
