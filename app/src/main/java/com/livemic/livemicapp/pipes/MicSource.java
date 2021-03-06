package com.livemic.livemicapp.pipes;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.effects.DalekEffect;
import com.livemic.livemicapp.model.Conversation;

import java.util.Timer;
import java.util.TimerTask;

/** Source of audio coming from phone mic. */
public class MicSource extends AudioSource {
  private static final DalekEffect EFFECT = new DalekEffect();

  private final Timer timer = new Timer();
  private AudioRecord audioIn;

  public MicSource(final Conversation c) {
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
        while(audioIn != null) {
          int nRead = audioIn.read(array, 0, Constants.REWRITE_CAPACITY, AudioRecord.READ_BLOCKING);
          if (c.useEasterEgg()) {
            array = EFFECT.newSampleBytes(array);
          }
          handleNewSamples(array);
        }
      }
    }, 0);
  }

  /** Stop listening to the mic - destroyed, create a new MicSource after this. */
  public void stop() {
    timer.cancel();
    audioIn.stop();
    audioIn = null;
  }
}
