package com.livemic.livemicapp.model;

import android.databinding.BaseObservable;

import com.livemic.livemicapp.pipes.AudioSource;

import java.util.List;

/**
 * A group discussion with lots of members.
 */
public class Conversation extends BaseObservable {
  // NOTE: IN PROGRESS!
  // Needs to be cleaned up a lot...
  private final List<Participant> participants;
  private final List<String> recentMessages;
  private boolean amModerator;
  private String currentTalker;
  private long talkingStartMs;
  private AudioSource talkingSource;

  public Conversation(
      boolean amModerator,
      List<Participant> participants,
      String currentTalker,
      long talkingStartMs,
      AudioSource talkingSource,
      List<String> recentMessages) {
    this.amModerator = amModerator;
    this.participants = participants;
    this.currentTalker = currentTalker;
    this.talkingStartMs = talkingStartMs;
    this.talkingSource = talkingSource;
    this.recentMessages = recentMessages;
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
    notifyChange();
  }

  // HACK

  // TODO - proper stuff...
  public void selectParticipant(Participant participant) {
    updateTalker(participant);
  }
}
