package com.livemic.livemicapp.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.BaseObservable;
import android.os.Message;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.view.View;

import com.livemic.livemicapp.ConnectionService;
import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.Util;
import com.livemic.livemicapp.pipes.AudioSource;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.RemoteOrLocalSource;
import com.livemic.livemicapp.pipes.wifi.WiFiDirectSink;
import com.livemic.livemicapp.pipes.wifi.WiFiDirectSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.livemic.livemicapp.Constants.MSG_PUSHOUT_DATA;

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

  private final WiFiDirectSource wifiSource;
  private WiFiDirectSink wifiSink;
  private boolean easterEgg = false;

  public Conversation(
      Context ctx,
      boolean amModerator,
      Participant me,
      String currentTalker,
      WiFiDirectSource wifiSource) {
    this.ctx = ctx;
    this.amModerator = amModerator;
    this.me = me;
    this.wifiSource = wifiSource;
    this.participants = new ArrayList<>();
    this.currentTalker = currentTalker;
    this.talkingStartMs = System.currentTimeMillis();
    this.recentMessages = new ArrayList<>();
    if (me.name.equals(currentTalker)) {
      talkingSource.switchToLocalSource(createMicSourceAndMaybeAttachToWiFi());
    } else {
      talkingSource.switchToRemoteSource(wifiSource);
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
    if (amModerator) {
      pushChange(MessageUtil.fromParticipants(participants));
    }
    notifyChange();
  }
  /** Person left :( */
  public void removeParticipant(String name) {
    Participant matched = null;
    for (Participant p : participants) {
      if (p.name.equals(name)) {
        matched = p;
        break;
      }
    }
    if (matched != null) {
      participants.remove(matched);
      if (amModerator) {
        pushChange(MessageUtil.fromParticipants(participants));
      }
      notifyChange();
    }
  }

  /** New person is talking! */
  public void updateTalker(Participant newTalker) {
    currentTalker = newTalker == null ? "" : newTalker.name;
    talkingStartMs = System.currentTimeMillis();
    boolean talkerIsMe = me.name.equals(currentTalker);
    boolean talkIsLocal = talkingSource.isLocal();
    if (talkerIsMe != talkIsLocal) {
      switchAudioSource();
    }
    if (amModerator) {
      pushChange(MessageUtil.fromNewTalker(currentTalker));
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
      if (amModerator) {
        pushChange(MessageUtil.fromPastMessages(new ArrayList<>(previousMessages)));
      }
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
      switchAudioSource();
    }
  }

  public void sendSamples(byte[] samples) {
    pushChange(MessageUtil.fromSamples(samples));
  }

  // HACK

  // TODO - proper stuff...
  public void maybeSelectParticipant(Participant participant) {
    if (amModerator) {
      updateTalker(participant);
    }
  }

  public void switchAudioSource() {
    Log.i(Constants.TAG, "Switching from " + (talkingSource.isLocal() ? " local" : " remote"));
    if (talkingSource.isLocal()) {
      talkingSource.switchToRemoteSource(wifiSource);
    } else {
      talkingSource.switchToLocalSource(createMicSourceAndMaybeAttachToWiFi());
    }
    notifyChange();
  }

  /** Creates a source from the mic, and if connected, also forward the result to wifi. */
  private MicSource createMicSourceAndMaybeAttachToWiFi() {
    MicSource micSource = new MicSource(this);
    if (wifiSink != null) {
      micSource.addSink(wifiSink);
    }
    return micSource;
  }

  /** Pushes a conversation metadata change to all clients. */
  private void pushChange(MessageObject message) {
    if (!MessageUtil.isSamples(message) && !amModerator) {
      throw new IllegalStateException("Only moderator can push metadata");
    };
    Log.d(Constants.TAG, "pushOutMessage : " + message.toString());
    Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
    msg.what = MSG_PUSHOUT_DATA;
    msg.obj = message;
    ConnectionService.getInstance().getHandler().sendMessage(msg);
  }

  /** Invoked on a client when the server pushes new metadata. */
  public void updateMetaFromServer(
      List<Participant> participants, String talkingParticipant, List<String> pastMessages) {
    if (amModerator) {
      throw new IllegalStateException("Only non-moderators need to process metadata");
    }

    Log.i(Constants.TAG, "META CHANGE: " + participants + "\n" + talkingParticipant + "\n" + pastMessages);
    if (participants != null) {
      this.participants.clear();
      this.participants.addAll(participants);
    }
    if (talkingParticipant != null) {
      currentTalker = talkingParticipant;
      boolean talkerIsMe = me.name.equals(currentTalker);
      boolean talkIsLocal = talkingSource.isLocal();
      if (talkerIsMe != talkIsLocal) {
        switchAudioSource();
      }
    }
    if (pastMessages != null) {
      currentMessage = "";
      recentMessages.clear();
      recentMessages.addAll(pastMessages);
    }
    notifyChange();
  }

  // HACK - Conversation needs a direct sink for construction,
  // direct sink then also needs a conversation.
  public void setSink(WiFiDirectSink sink) {
    this.wifiSink = sink;
    if (talkingSource.isLocal()) {
      talkingSource.attachToNewWiFiSink(sink);
    }
  }

  // Easter egg! Toggle dalek mode.
  public void toggleEasterEgg() {
    this.easterEgg = !this.easterEgg;
  }

  public boolean useEasterEgg() {
    return this.easterEgg;
  }
}
