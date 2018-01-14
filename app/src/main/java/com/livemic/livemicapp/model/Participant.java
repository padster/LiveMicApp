package com.livemic.livemicapp.model;

import android.databinding.BaseObservable;

/**
 * Person who's taking part in the conversation.
 */
public class Participant extends BaseObservable {
  // NOTE: IN PROGRESS!
  // Needs to be cleaned up a lot...

  // Unique within the conversation:
  public final String name;

  public Participant(String name) {
    this.name = name;
  }
}
