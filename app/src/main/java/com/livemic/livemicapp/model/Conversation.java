package com.livemic.livemicapp.model;

import android.databinding.BaseObservable;
import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.Util;
import com.livemic.livemicapp.pipes.AudioSink;
import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.RemoteOrLocalSource;
import com.livemic.livemicapp.pipes.wifi.WiFiDirectSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A group discussion with lots of members.
 */
public class Conversation extends BaseObservable {
  // NOTE: IN PROGRESS!
  // Needs to be cleaned up a lot...
  private final RemoteOrLocalSource talkingSource = new RemoteOrLocalSource();

  private final List<Participant> participants;

  private String currentTalker;
  private long talkingStartMs;

  private String currentMessage;
  private final List<String> recentMessages;

  private boolean amModerator;

  public Conversation(
      boolean amModerator,
      String currentTalker,
      MicSource localSource) {
    this.amModerator = amModerator;
    this.participants = new ArrayList<>();
    this.currentTalker = currentTalker;
    this.talkingStartMs = System.currentTimeMillis();
    this.recentMessages = new ArrayList<>();
    if (localSource != null) {
      talkingSource.switchToLocalSource(localSource);
    }
  }

  //
  // Accessors
  //

  /** @return Audio source for the conversation, can be from wherever. */
  public AudioSource getAudioSource() {
    return this.talkingSource;
  }

  /** @return Participant in a particular position. */
  public Participant getParticipant(int position) {
    return participants.get(position);
  }

  /** @return Whether the sound data is coming from the device. */
  public boolean isLocalAudio() {
    return talkingSource.isLocal();
  }

  /** @return Number of participants. */
  public int participantCount() {
    return participants.size();
  }

  /** @return Whether a particular person is talking right now. */
  public boolean isTalker(Participant participant) {
    if (participant == null || participant.name == null) {
      return "".equals(currentTalker);
    } else {
      return participant.name.equals(currentTalker);
    }
  }

  /** @return Formatted string for current talker. */
  public String formattedTalkerString() {
    if ("".equals(currentTalker)) {
      return "No speaker";
    } else {
      return String.format("Speaking: %s (%s)", currentTalker,
          Util.formatTalkTime(System.currentTimeMillis() - talkingStartMs));
    }
  }

  /** @return Log of current and previous messages. */
  public String formattedMessageLog() {
    StringBuilder result = new StringBuilder();
    if (currentMessage != null && currentMessage.length() > 0) {
      result.append(currentMessage);
    } else {
      result.append("--");
    }
    result.append("\n");
    for (String prevMessage : recentMessages) {
      result.append("\n" + prevMessage);
    }
    return result.toString();
  }

  //
  // Mutators
  //

  /** New person has joined! */
  public void addParticipant(Participant participant) {
    participants.add(participant);
    notifyChange();
  }

  /** New person is talking! */
  public void updateTalker(Participant participant) {
    currentTalker = participant == null ? "" : participant.name;
    talkingStartMs = System.currentTimeMillis();
    notifyChange();
  }

  /**
   * Update the message log while talking.
   * @param currentMessage Local message being said - only use for current talker. Null to not update.
   * @param previousMessages Short history of previous messages, in reverse order. Null to not update.
   */
  public void updateMessages(String currentMessage, Collection<String> previousMessages) {
    boolean changed = false;
    if (previousMessages != null) {
      this.recentMessages.clear();
      this.recentMessages.addAll(previousMessages);
      changed = true;
    }
    if (currentMessage != null) {
      this.currentMessage = currentMessage.trim();
      changed = true;
    }
    if (changed) {
      notifyChange();
    }
  }

  // HACK

  // TODO - proper stuff...
  public void selectParticipant(Participant participant) {
    updateTalker(participant);
  }

  public void hackSwitchAudio() {
    Log.i(Constants.TAG, "Switching from " + (talkingSource.isLocal() ? " local" : " remote"));
    if (talkingSource.isLocal()) {
      talkingSource.switchToRemoteSource(new WiFiDirectSource());
    } else {
      talkingSource.switchToLocalSource(new MicSource());
    }
    notifyChange();
  }
}
