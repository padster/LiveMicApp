package com.livemic.livemicapp.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.BaseObservable;
import android.icu.text.MessagePattern;
import android.util.Log;
import android.view.View;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.Util;
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
  private final Context ctx; // ew, yuk, this is horrible.


  // NOTE: IN PROGRESS!
  // Needs to be cleaned up a lot...
  private final RemoteOrLocalSource talkingSource = new RemoteOrLocalSource();

  private final List<Participant> participants;

  private String currentTalker;
  private long talkingStartMs;

  private String currentMessage;
  private final List<String> recentMessages;

  private boolean amModerator;
  private final Participant me;

  public Conversation(
      Context ctx,
      boolean amModerator,
      Participant me,
      String currentTalker) {
    this.ctx = ctx;
    this.amModerator = amModerator;
    this.me = me;
    this.participants = new ArrayList<>();
    this.currentTalker = currentTalker;
    this.talkingStartMs = System.currentTimeMillis();
    this.recentMessages = new ArrayList<>();
    if (me.name.equals(currentTalker)) {
      talkingSource.switchToLocalSource(new MicSource());
    }
  }

  //
  // Accessors
  //

  /** @return Audio source for the conversation, can be from wherever. */
  public AudioSource getAudioSource() {
    return this.talkingSource;
  }

  /** @return Whether the local user is the amModerator of the conversation. */
  public boolean amModerator() {
    return amModerator;
  }

  /** @return Whether I can mute this user. */
  public boolean canMute(Participant p) {
    return amModerator() && isTalker(p) && !p.name.equals(me.name);
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
  public void updateTalker(Participant newTalker) {
    currentTalker = newTalker == null ? "" : newTalker.name;
    talkingStartMs = System.currentTimeMillis();
    boolean talkerIsMe = me.name.equals(currentTalker);
    boolean talkIsLocal = talkingSource.isLocal();
    if (talkerIsMe != talkIsLocal) {
      hackSwitchAudio();
    }
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

  /** Mute a user, but only after confirmation. */
  public void handleMute(View view, final Participant participant) {
    new AlertDialog.Builder(ctx)
        .setTitle("Confirm Mute")
        .setMessage("Stop " + participant.name + " talking?")
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            Log.i(Constants.TAG, "Muting " + participant.name);
            actuallyMute(participant);
          }})
        .setNegativeButton(android.R.string.cancel, null).show();

  }
  private void actuallyMute(Participant participant) {
    updateTalker(me);
    if (!talkingSource.isLocal()) {
      hackSwitchAudio();
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
