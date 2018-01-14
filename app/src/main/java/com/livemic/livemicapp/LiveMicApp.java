package com.livemic.livemicapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import static com.livemic.livemicapp.Constants.MSG_STARTCLIENT;
import static com.livemic.livemicapp.Constants.MSG_STARTSERVER;

/**
 * Created by Prayansh on 2018-01-13.
 */

public class LiveMicApp extends Application {

    private static final String TAG = "PTP_APP";

    WifiP2pManager mP2pMan = null;
    WifiP2pManager.Channel mP2pChannel = null;
    boolean mP2pConnected = false;
    String mMyAddr = null;
    String mDeviceName = null;   // the p2p name that is configurated from UI.
    BroadcastReceiver wiFiDirectBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Recieved intent:" + intent.getAction());
            String action = intent.getAction();
            Intent serviceIntent = new Intent(context, ConnectionService.class);  // start ConnectionService
            serviceIntent.setAction(action);   // put in action and extras
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);  // start the connection service
        }
    };
    WifiP2pDevice mThisDevice = null;
    WifiP2pInfo mP2pInfo = null;  // set when connection info available, reset when WIFI_P2P_CONNECTION_CHANGED_ACTION

    boolean mIsServer = false;

    WiFiDirectActivity mHomeActivity = null;
    List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();  // update on every peers available
    JSONArray mMessageArray = new JSONArray();    // limit to the latest 50 messages

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter wifip2pFilter = new IntentFilter();
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(wiFiDirectBroadcastReceiver, wifip2pFilter);
    }


    /**
     * whether p2p is enabled in this device.
     * my bcast listener always gets enable/disable intent and persist to shared pref
     */
    public boolean isP2pEnabled() {
        String state = AppPreferences.getStringFromPref(this, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED);
        if (state != null && "1".equals(state.trim())) {
            return true;
        }
        return false;
    }

    /**
     * upon p2p connection available, group owner start server socket channel
     * start socket server and select monitor the socket
     */
    public void startSocketServer() {
        Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
        msg.what = MSG_STARTSERVER;
        ConnectionService.getInstance().getHandler().sendMessage(msg);
    }

    /**
     * upon p2p connection available, non group owner start socket channel connect to group owner.
     */
    public void startSocketClient(String hostname) {
        Log.d(TAG, "startSocketClient : client connect to group owner : " + hostname);
        Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
        msg.what = MSG_STARTCLIENT;
        msg.obj = hostname;
        ConnectionService.getInstance().getHandler().sendMessage(msg);
    }

    /**
     * check whether there exists a connected peer.
     */
    public WifiP2pDevice getConnectedPeer() {
        WifiP2pDevice peer = null;
        for (WifiP2pDevice d : mPeers) {
            PTPLog.d(TAG, "getConnectedPeer : device : " + d.deviceName + " status: " + ConnectionService.getDeviceStatus(d.status));
            if (d.status == WifiP2pDevice.CONNECTED) {
                peer = d;
            }
        }
        return peer;
    }

    public void clearMessages() {
        mMessageArray = new JSONArray();
    }

    /**
     * get the intent to lauch any activity
     */
    public Intent getLaunchActivityIntent(Class<?> cls, String initmsg) {
        Intent i = new Intent(this, cls);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("FIRST_MSG", initmsg);
        return i;
    }

    public void setMyAddr(String addr) {
        mMyAddr = addr;
    }

    public static class PTPLog {
        public static void i(String tag, String msg) {
            Log.i(tag, msg);
        }

        public static void d(String tag, String msg) {
            Log.d(tag, msg);
        }

        public static void e(String tag, String msg) {
            Log.e(tag, msg);
        }
    }

}
