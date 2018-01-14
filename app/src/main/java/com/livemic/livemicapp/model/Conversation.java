package com.livemic.livemicapp.model;

import android.databinding.BaseObservable;

import com.livemic.livemicapp.Util;
import com.livemic.livemicapp.pipes.AudioSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A group discussion with lots of members.
 */
public class Conversation extends BaseObservable {
  // NOTE: IN PROGRESS!
  // Needs to be cleaned up a lot...
  private final List<Participant> participants;

  private String currentTalker;
  private long talkingStartMs;

  private String currentMessage;
  private final List<String> recentMessages;

  private boolean amModerator;
  private AudioSource talkingSource;

  public Conversation(
      boolean amModerator,
      String currentTalker,
      AudioSource talkingSource) {
    this.amModerator = amModerator;
    this.participants = new ArrayList<Participant>();
    this.currentTalker = currentTalker;
    this.talkingStartMs = System.currentTimeMillis();
    this.talkingSource = talkingSource;
    this.recentMessages = new ArrayList<String>();
  }

  //
  // Accessors
  //

  /** @return Participant in a particular position. */
  public Participant getParticipant(int position) {
    return participants.get(position);
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
}
