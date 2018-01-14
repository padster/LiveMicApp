package com.livemic.livemicapp;

import android.app.Activity;
import android.widget.TextView;

import com.livemic.livemicapp.pipes.AudioSink;
import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.SpeakerSink;
import com.livemic.livemicapp.pipes.SpeechToTextSink;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  public void start(Activity activity, TextChatLog messageLog) {
    AudioSource audioIn = new MicSource();
    AudioSink audioOut = new SpeakerSink();
    audioIn.addSink(audioOut);
//    SpeechToTextSink stt = new SpeechToTextSink(activity, messageLog);
//    audioIn.addSink(stt);
  }
}

