package com.livemic.livemicapp;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

// Deals with creating audio reader and audio writer, and piping one to the other.
public class SoundRewriter {
  public static final int MIN_BUFFER_SIZE = 8000;
  public static final int REWRITE_CAPACITY = 8128;
  public static final int UPDATE_MS = 20;

  private final AudioRecord audioIn;
  private final AudioTrack audioOut;
  private final Timer timer = new Timer();

  public SoundRewriter() {
    audioIn = new AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(32000)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build())
        .setBufferSizeInBytes(2 * MIN_BUFFER_SIZE)
        .build();
    audioOut = new AudioTrack.Builder()
        .setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build())
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(32000)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build())
        .setBufferSizeInBytes(2 * MIN_BUFFER_SIZE)
        .build();
  }

  public void start() {
    Log.i(Constants.TAG, "Starting rewriter");
    audioIn.startRecording();
    audioOut.play();
    timer.schedule(new TimerTask() {
      @Override public void run() {
        Log.i(Constants.TAG, "Starting rewrite loop...");
        byte[] array = new byte[REWRITE_CAPACITY];
        while(true) {
          int nRead = audioIn.read(array, 0, REWRITE_CAPACITY, AudioRecord.READ_BLOCKING);
          double tot = 0;
          for (int i = 0; i < nRead; i++) {
            tot += array[i];
          }
          audioOut.write(array, 0, nRead);
          Log.i(Constants.TAG, "Sent " + nRead + " bytes.");
        }
      }
    }, 0, UPDATE_MS);
  }


}

