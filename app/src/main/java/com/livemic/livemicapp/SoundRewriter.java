package com.livemic.livemicapp;

import com.livemic.livemicapp.model.Conversation;
import com.livemic.livemicapp.pipes.AudioSink;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;
import com.livemic.livemicapp.pipes.SpeakerSink;
import com.livemic.livemicapp.pipes.SpeechToTextSink;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  private static final int UI_SECONDS = 4;
  private static final int UI_DOWNSAMPLE = 4;

  // Create all initial pipes and hook them together.
  public void start(
      MainActivity activity,
      Conversation conversation,
      Runnable newDataCallback) {
    AudioSink audioOut = new SpeakerSink();

    RecentSamplesBuffer uiOut = new RecentSamplesBuffer(
        UI_SECONDS * Constants.SAMPLE_RATE_LOCAL_HZ / UI_DOWNSAMPLE, UI_DOWNSAMPLE, newDataCallback);
    activity.attachLocalBuffer(uiOut);

    conversation.getAudioSource().addSink(uiOut);
    conversation.getAudioSource().addSink(audioOut);

//    if (conversation.amModerator()) {
//      AudioSink speechToText = new SpeechToTextSink(activity, conversation);
//      conversation.getAudioSource().addSink(speechToText);
//    }
  }
}

