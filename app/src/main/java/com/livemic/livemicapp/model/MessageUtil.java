package com.livemic.livemicapp.model;

import java.util.ArrayList;
import java.util.List;

/** Helpers for dealing with MessageObject. */
public class MessageUtil {
  private MessageUtil() {} // Don't create, use static methods.

  //
  // Constructors to fill in the fields for each call.
  //
  public static MessageObject fromSamples(byte[] samples) {
    return new MessageObject(samples, null, null, null);
  }
  public static MessageObject fromParticipants(List<Participant> participants) {
    List<String> names = new ArrayList<>();
    for (Participant p : participants) {
      names.add(p.name);
    }
    return new MessageObject(null, names, null, null);
  }
  public static MessageObject fromNewTalker(String talkerName) {
    return new MessageObject(null, null, talkerName, null);
  }
  public static MessageObject fromPastMessages(List<String> pastMessages) {
    return new MessageObject(null, null, null, pastMessages);
  }

  //
  // Logic for checking what call has been made
  //
  public static boolean isSamples(MessageObject msg) {
    return msg.getAudioData() != null;
  }
  public static boolean isParticipants(MessageObject msg) {
    return msg.getParticipants() != null;
  }
  public static boolean isNewTalker(MessageObject msg) {
    return msg.getTalkingParticipant() != null;
  }
  public static boolean isPastMessages(MessageObject msg) {
    return msg.getPastMessages() != null;
  }
}
