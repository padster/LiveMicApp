package com.livemic.livemicapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Prayansh on 2018-01-13.
 */

public class MessageObject implements Parcelable, Serializable {
    private byte[] audioData;
    public static final String mDel = "^&^";

    private MessageObject() {
        audioData = null;
    }

    public MessageObject(byte[] audioData) {
        this.audioData = audioData;
    }

    public MessageObject(Parcel in) {
        readFromParcel(in);
    }


    @Override public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        in.readByteArray(audioData);
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        if (audioData != null) {
            dest.writeByteArray(audioData);
        }
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

    @Override public String toString() {
        return "MessageObject{" +
                "audioData=" + Arrays.toString(audioData) +
                '}';
    }
}
