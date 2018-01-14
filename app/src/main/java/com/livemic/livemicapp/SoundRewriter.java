package com.livemic.livemicapp;

import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  private static final int UI_SECONDS = 4;
  private static final int UI_DOWNSAMPLE = 4;

  public void start(MainActivity activity, TextChatLog messageLog, Runnable newDataCallback) {
    AudioSource audioIn = new MicSource();
//    AudioSink audioOut = new SpeakerSink();
    RecentSamplesBuffer audioOut = new RecentSamplesBuffer(
        UI_SECONDS * Constants.SAMPLE_RATE_LOCAL_HZ / UI_DOWNSAMPLE, UI_DOWNSAMPLE, newDataCallback);
    activity.attachLocalBuffer(audioOut);
    audioIn.addSink(audioOut);
//    SpeechToTextSink stt = new SpeechToTextSink(activity, messageLog);
//    audioIn.addSink(stt);
  }
}

