package com.livemic.livemicapp;

import com.livemic.livemicapp.pipes.AudioSink;
import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;
import com.livemic.livemicapp.pipes.SpeakerSink;
import com.livemic.livemicapp.pipes.SpeechToTextSink;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  private static final int UI_SECONDS = 4;
  private static final int UI_DOWNSAMPLE = 4;

  // Create all initial pipes and hook them together.
  public void start(MainActivity activity, TextChatLog messageLog, Runnable newDataCallback) {
    AudioSink speechToText = new SpeechToTextSink(activity, messageLog);
    AudioSink audioOut = new SpeakerSink();

    RecentSamplesBuffer uiOut = new RecentSamplesBuffer(
        UI_SECONDS * Constants.SAMPLE_RATE_LOCAL_HZ / UI_DOWNSAMPLE, UI_DOWNSAMPLE, newDataCallback);
    activity.attachLocalBuffer(uiOut);


    AudioSource audioIn = new MicSource();
    audioIn.addSink(uiOut);
    audioIn.addSink(audioOut);
    audioIn.addSink(speechToText);
  }
}

