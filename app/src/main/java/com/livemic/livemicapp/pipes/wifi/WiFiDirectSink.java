package com.livemic.livemicapp.pipes.wifi;

import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.MainActivity;
import com.livemic.livemicapp.pipes.AudioSink;


/**
 * Sink that takes samples coming in and sends them out over WiFi direct.
 */
public class WiFiDirectSink implements AudioSink {
  private final MainActivity activity;

  public WiFiDirectSink(MainActivity activity) {
    this.activity = activity;
  }

  @Override
  public void newSamples(byte[] samples) {
    // Send the samples over wifi
    activity.sendSamples(samples);
  }

  public void stop() {
    // TODO
    Log.i(Constants.TAG, "TODO: Implement WiFi Sink STOP");
  }
}
