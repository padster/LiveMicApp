package com.livemic.livemicapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Payload for all possible messages that can be sent. */
public class MessageObject implements Parcelable, Serializable {
    private byte[] audioData;
    private List<String> participantNames;
    private String talkingParticipant;
    private List<String> pastMessages;

    private MessageObject() {
      audioData = null;
      participantNames = null;
      talkingParticipant = null;
      pastMessages = null;
    }

    public MessageObject(
        byte[] audioData,
        List<String> participantNames,
        String talkingParticipant,
        List<String> pastMessages) {
      this.audioData = audioData;
      this.participantNames = participantNames;
      this.talkingParticipant = talkingParticipant;
      this.pastMessages = pastMessages;
    }

    public MessageObject(Parcel in) {
      readFromParcel(in);
    }

    public byte[] getAudioData() {
      return this.audioData;
    }

    public List<Participant> getParticipants() {
      if (participantNames == null) {
        return null;
      }
      List<Participant> result = new ArrayList<>();
      for (String name : participantNames) {
        result.add(new Participant(name));
      }
      return result;
    }

    public String getTalkingParticipant() {
      return talkingParticipant;
    }

    public List<String> getPastMessages() {
      return pastMessages;
    }

    @Override public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        in.readByteArray(audioData);
        in.readStringList(participantNames);
        talkingParticipant = in.readString();
        in.readStringList(pastMessages);
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(audioData);
        dest.writeStringList(participantNames);
        dest.writeString(talkingParticipant);
        dest.writeStringList(pastMessages);
    }

    public static final Creator<MessageObject> CREATOR = new Creator<MessageObject>() {
        @Override
        public MessageObject createFromParcel(Parcel in) {
            return new MessageObject(in);
        }

        @Override
        public MessageObject[] newArray(int size) {
            return new MessageObject[size];
        }
    };

  @Override
  public String toString() {
    return "MessageObject{" +
        "audioData=" + Arrays.toString(audioData) +
        ", participantNames=" + participantNames +
        ", talkingParticipant='" + talkingParticipant + '\'' +
        ", pastMessages=" + pastMessages +
        '}';
  }
}
