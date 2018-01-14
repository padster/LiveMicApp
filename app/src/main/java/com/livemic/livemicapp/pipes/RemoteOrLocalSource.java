package com.livemic.livemicapp.pipes;

import android.support.v4.util.Preconditions;

import com.livemic.livemicapp.pipes.wifi.WiFiDirectSource;

/** Source that is backed by at most one of: a local source (mic) or remote source (wifi). */
public class RemoteOrLocalSource extends AudioSource implements AudioSink {
  private WiFiDirectSource remoteSource;
  private MicSource localSource;

  /** @return True iff the local source is connected and used. */
  public boolean isLocal() {
    return localSource != null;
  }

  /** Switch to forwarding from a new local source. */
  public void switchToLocalSource(MicSource newLocalSource) {
    if (remoteSource != null) {
      remoteSource.removeSink(this);
      remoteSource = null;
    }
    if (localSource != null) {
      if (localSource != newLocalSource) {
        localSource.stop();
        localSource.removeSink(this);
        localSource = null;
      }
    }
    localSource = newLocalSource;
    localSource.addSink(this);
  }

  /** Switch to forwarding from a new remote source. */
  public void switchToRemoteSource(WiFiDirectSource newRemoteSource) {
    if (localSource != null) {
      localSource.stop();
      localSource.removeSink(this);
      localSource = null;
    }
    if (remoteSource != null) {
      if (remoteSource != newRemoteSource) {
        remoteSource.removeSink(this);
      }
    }
    remoteSource = newRemoteSource;
    if (remoteSource != null) {
      // NOTE: null is fine here, it means no source.
      remoteSource.addSink(this);
    }
  }

  @Override
  public void newSamples(byte[] samples) {
    // Forward from upstream source to downstream sinks
    handleNewSamples(samples);
  }
}
