package com.livemic.livemicapp;

/**
 * Dump any constants in here...
 */
public class Constants {

    public static final String TAG = "LiveMic";

    public static final double UI_AUDIO_SCALE = 1.0; // Lower = zoom in sample UI.

    public static int SAMPLE_RATE_LOCAL_HZ = 32000;
    public static int SAMPLE_RATE_NETWORK_HZ = 16000;

    public static String LOCAL_ONLY_TAG = "tagLocalOnly";

    public static String PACKAGE_NAME = Constants.class.getPackage().getName();

    // Message
    public static final int MSG_NULL = 0;
    public static final int MSG_STARTSERVER = 1001;
    public static final int MSG_STARTCLIENT = 1002;
    public static final int MSG_CONNECT = 1003;
    public static final int MSG_DISCONNECT = 1004;   // p2p disconnect
    public static final int MSG_PUSHOUT_DATA = 1005;
    public static final int MSG_NEW_CLIENT = 1006;
    public static final int MSG_FINISH_CONNECT = 1007;
    public static final int MSG_PULLIN_DATA = 1008;
    public static final int MSG_REGISTER_ACTIVITY = 1009;

    public static final int MSG_SELECT_ERROR = 2001;
    public static final int MSG_BROKEN_CONN = 2002;  // network disconnect

    public static final int MSG_SIZE = 50;    // the lastest 50 messages
    public static final String MSG_SENDER = "sender";
    public static final String MSG_TIME = "time";
    public static final String MSG_CONTENT = "msg";

    public static String KEY_DATA = "DATA";
  public static int REWRITE_CAPACITY = 8128;

  // Turn on for dalek-ification.
  public static final boolean EASTER_EGG = false;
}
