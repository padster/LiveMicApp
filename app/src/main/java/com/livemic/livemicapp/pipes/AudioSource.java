package com.livemic.livemicapp.pipes;

import android.util.Log;

import com.livemic.livemicapp.Constants;

import java.util.ArrayList;
import java.util.List;

// Producer of realtime Audio buffers
public class AudioSource {
  private List<AudioSink> sinks = new ArrayList<>();

  /** Each source should call this when they get new data.  */
  protected void handleNewSamples(byte[] samples) {
    for (AudioSink sink : sinks) {
      Log.i(Constants.TAG, "TALK to " + sink.getClass().getName());
      sink.newSamples(samples);
    }
  }

  public void addSink(AudioSink sink) {
    this.sinks.add(sink);
  }
  public void removeSink(AudioSink sink) {
    this.sinks.remove(sink);
  }
}
