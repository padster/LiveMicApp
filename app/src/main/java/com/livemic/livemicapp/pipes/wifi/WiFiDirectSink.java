package com.livemic.livemicapp.pipes.wifi;

import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.MainActivity;
import com.livemic.livemicapp.model.Conversation;
import com.livemic.livemicapp.pipes.AudioSink;


/**
 * Sink that takes samples coming in and sends them out over WiFi direct.
 */
public class WiFiDirectSink implements AudioSink {
  private Conversation conversation;

  public void setConversation(Conversation conversation) {
    // HACK - Conversation needs a direct sink for construction,
    // direct sink then also needs a conversation.
    this.conversation = conversation;
  }

  @Override
  public void newSamples(byte[] samples) {
    // Send the samples over wifi
    conversation.sendSamples(samples);
  }

  public void stop() {
    // TODO
    Log.i(Constants.TAG, "TODO: Implement WiFi Sink STOP");
  }
}
