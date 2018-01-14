package com.livemic.livemicapp.pipes;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.livemic.livemicapp.Constants;

import java.util.Timer;
import java.util.TimerTask;

/** Source of audio coming from phone mic. */
public class MicSource extends AudioSource {
  public static final int UPDATE_MS = 20;

  private final AudioRecord audioIn;
  private final Timer timer = new Timer();

  public MicSource() {
    audioIn = new AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Constants.SAMPLE_RATE_LOCAL_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build())
        .setBufferSizeInBytes(2 * Constants.REWRITE_CAPACITY)
        .build();
    audioIn.startRecording();
    timer.schedule(new TimerTask() {
      @Override public void run() {
        Log.i(Constants.TAG, "Starting rewrite loop...");
        byte[] array = new byte[Constants.REWRITE_CAPACITY];
        while(true) {
          int nRead = audioIn.read(array, 0, Constants.REWRITE_CAPACITY, AudioRecord.READ_BLOCKING);
          double tot = 0;
          for (int i = 0; i < nRead; i++) {
            tot += array[i];
          }
          handleNewSamples(array);
        }
      }
    }, 0, UPDATE_MS);
  }
}
