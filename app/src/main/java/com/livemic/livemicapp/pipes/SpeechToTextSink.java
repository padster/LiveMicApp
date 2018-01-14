package com.livemic.livemicapp.pipes;

import android.app.Activity;
import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.TextChatLog;
import com.livemic.livemicapp.Util;
import com.livemic.livemicapp.model.Conversation;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechAudioFormat;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.util.ArrayDeque;
import java.util.Deque;

// Sink for audio, streams to MS Speech to Text service.
public class SpeechToTextSink implements ISpeechRecognitionServerEvents, AudioSink {
  private static final double QUIET_AMPLITUDE = 1e-3;
  private static final int QUIET_SAMPLES_IN_A_ROW = Constants.SAMPLE_RATE_NETWORK_HZ / 2;

  private final Deque<String> recentMessages = new ArrayDeque<>();

  private final Activity activity;
  private final Conversation conversation;
  private String currentMessage = "";
  private DataRecognitionClient dataClient;
  private int quietSamplesAllowed;

  public SpeechToTextSink(Activity activity, Conversation conversation) {
    this.activity = activity;
    this.conversation = conversation;
  }

  private void updateTextLog() {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        conversation.updateMessages(currentMessage, recentMessages);
      }
    });
  }

  // Microsoft API settings.
  private SpeechRecognitionMode getMode() {
    return SpeechRecognitionMode.LongDictation;
  }
  private String getDefaultLocale() {
    return "en-us";
  }
  public String getPrimaryKey() {
    return "60c5eef051cc444e90e4da981d34c419";
  }

  // Microsoft Events!
  @Override
  public void onPartialResponseReceived(final String message) {
    Log.i(Constants.TAG, "PARTIAL > " + message);
    currentMessage = message;
    updateTextLog();
  }

  @Override
  public void onFinalResponseReceived(RecognitionResult recognitionResult) {
    Log.i(Constants.TAG, "FINAL > " + recognitionResult.Results.length);
    if (recognitionResult.RecognitionStatus == RecognitionStatus.RecognitionSuccess) {
      if (recognitionResult.Results.length > 0) {
        Log.i(Constants.TAG, "ADDED > " + recognitionResult.Results[recognitionResult.Results.length - 1].DisplayText);
        recentMessages.addFirst(recognitionResult.Results[recognitionResult.Results.length - 1].DisplayText);
      }
      while (recentMessages.size() > 10) {
        recentMessages.removeLast();
      }
      currentMessage = "";
      updateTextLog();
    } else {
      Log.i(Constants.TAG, "Failed STT with status " + recognitionResult.RecognitionStatus);
      currentMessage = "";
      updateTextLog();
    }
  }

  @Override
  public void onIntentReceived(String s) {
    // IGNORE (?)
  }

  @Override
  public void onError(int i, String s) {
    currentMessage = "Azure connection died :(";
    recentMessages.addFirst("WHOOPS - need to restart the app for transcription...");
  }

  @Override
  public void onAudioEvent(boolean b) {

  }

  // Audio Sink methods
  @Override
  public void newSamples(byte[] samples) {
    // Need to downsample PCM 16bit 32khz -> 16khz
    Log.v(Constants.TAG, "SENDING SAMPLES");
    int newLength = samples.length / 2;
    byte[] downsampled = new byte[samples.length / 2];
    for (int i = 0; i < newLength / 2; i++) {
      downsampled[2 * i    ] = samples[4 * i    ];
      downsampled[2 * i + 1] = samples[4 * i + 1];
    }

    /*
    if (isQuiet(downsampled)) {
      if (quietSamplesAllowed == 0) {
        // Already quiet, do nothing
      } else {
        quietSamplesAllowed -= (downsampled.length / 2);
        if (quietSamplesAllowed <= 0) {
          Log.i(Constants.TAG, "SST >>> Killing Connection");
          dataClient.endAudio();
          dataClient = null;
          quietSamplesAllowed = 0;
        }
      }
    } else {
    */
    quietSamplesAllowed = QUIET_SAMPLES_IN_A_ROW;
    forceConnection().sendAudio(downsampled, newLength);
    // }
  }

  /** Lazily re-create the connection whenever required. */
  private DataRecognitionClient forceConnection() {
    if (dataClient == null) {
      Log.i(Constants.TAG, "SST >>> Reforming Connection");
      dataClient = SpeechRecognitionServiceFactory.createDataClient(
          activity,
          this.getMode(),
          this.getDefaultLocale(),
          this,
          this.getPrimaryKey());
      dataClient.sendAudioFormat(SpeechAudioFormat.create16BitPCMFormat(Constants.SAMPLE_RATE_NETWORK_HZ));
    }
    return dataClient;
  }

  /** @return Whether the previous samples were quiet enough to be 'silent'. */
  private boolean isQuiet(byte[] pcmSamples) {
    double avAmplitude = 0.0;
    for (int i = 0; i < pcmSamples.length; i += 2) {
      float sz = Util.pcmBytesToFloat(pcmSamples[i], pcmSamples[i + 1]);
      avAmplitude += sz * sz;
    }
    avAmplitude /= (pcmSamples.length / 2);
    return avAmplitude < QUIET_AMPLITUDE;
  }
}
