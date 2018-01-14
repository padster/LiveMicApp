package com.livemic.livemicapp.pipes;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.livemic.livemicapp.Constants;

// Output for audio to be written to the default phone audio out.
public class SpeakerSink implements AudioSink {
  private final AudioTrack audioOut;

  public SpeakerSink() {
    audioOut = new AudioTrack.Builder()
        .setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build())
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Constants.SAMPLE_RATE_LOCAL_HZ)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build())
        .setBufferSizeInBytes(2 * Constants.REWRITE_CAPACITY)
        .build();
    audioOut.play();
  }

  @Override
  public void newSamples(byte[] samples) {
    if (audioOut == null) {
      Log.d(Constants.TAG, "SPEAKER SINK DEAD, need to remove it from its source.");
    }

    // TODO - pass in length / initial offset too? Needed if switching to non-blocking.
    audioOut.write(samples, 0, samples.length);
    double tot = 0;
    for (byte sample : samples) {
      tot += sample;
    }

//    Log.v(Constants.TAG, tot + "   -- Sent " + samples.length + " bytes.");
  }

  /** Stop sending to sink - after these need to create a new one. */
  public void stop() {
    audioOut.stop();
  }
}
