package com.livemic.livemicapp.pipes;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.TextChatLog;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognizedPhrase;
import com.microsoft.cognitiveservices.speechrecognition.SpeechAudioFormat;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.util.ArrayDeque;
import java.util.Deque;

// Sink for audio, streams to MS Speech to Text service.
public class SpeechToTextSink implements ISpeechRecognitionServerEvents, AudioSink {
  private final DataRecognitionClient dataClient;

  private final Deque<String> recentMessages = new ArrayDeque<>();
  private final Activity activity;
  private final TextChatLog textLog;
  private String currentMessage = "";

  public SpeechToTextSink(Activity activity, TextChatLog textLog) {
    this.activity = activity;
    this.textLog = textLog;
    Log.i(Constants.TAG, "STT CREATED");
    dataClient = SpeechRecognitionServiceFactory.createDataClient(
        activity,
        this.getMode(),
        this.getDefaultLocale(),
        this,
        this.getPrimaryKey());
    dataClient.sendAudioFormat(SpeechAudioFormat.create16BitPCMFormat(Constants.SAMPLE_RATE_NETWORK_HZ));
  }

  private void updateTextLog() {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        textLog.handleChatText(currentMessage, recentMessages);
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
    for (RecognizedPhrase phrase : recognitionResult.Results) {
      recentMessages.addLast(phrase.DisplayText);
    }
    while (recentMessages.size() > 15) {
      recentMessages.removeFirst();
    }
    currentMessage = "";
    updateTextLog();
  }

  @Override
  public void onIntentReceived(String s) {
    // IGNORE (?)
  }

  @Override
  public void onError(int i, String s) {
    Log.e(Constants.TAG, "ERROR: " + s);
  }

  @Override
  public void onAudioEvent(boolean b) {

  }

  // Audio Sink methods
  @Override
  public void newSamples(byte[] samples) {
    // Need to downsample PCM 16bit 32khz -> 16khz
    Log.i(Constants.TAG, "SENDING SAMPLES");
    int newLength = samples.length / 2;
    byte[] downsampled = new byte[samples.length / 2];
    for (int i = 0; i < newLength / 2; i++) {
      downsampled[2 * i    ] = samples[4 * i    ];
      downsampled[2 * i + 1] = samples[4 * i + 1];
    }
    dataClient.sendAudio(downsampled, newLength);
  }
}
