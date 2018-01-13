package com.livemic.livemicapp;

import com.livemic.livemicapp.pipes.AudioSink;
import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.SpeakerSink;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  public void start() {
    AudioSource audioIn = new MicSource();
    AudioSink audioOut = new SpeakerSink();
    audioIn.addSink(audioOut);
  }
}

