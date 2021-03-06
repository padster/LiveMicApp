package com.livemic.livemicapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.livemic.livemicapp.model.MessageObject;
import com.livemic.livemicapp.model.MessageUtil;
import com.livemic.livemicapp.wifidirect.WorkHandler;

import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;

import static com.livemic.livemicapp.Constants.MSG_BROKEN_CONN;
import static com.livemic.livemicapp.Constants.MSG_CONNECT;
import static com.livemic.livemicapp.Constants.MSG_DISCONNECT;
import static com.livemic.livemicapp.Constants.MSG_FINISH_CONNECT;
import static com.livemic.livemicapp.Constants.MSG_NEW_CLIENT;
import static com.livemic.livemicapp.Constants.MSG_NULL;
import static com.livemic.livemicapp.Constants.MSG_PULLIN_DATA;
import static com.livemic.livemicapp.Constants.MSG_PUSHOUT_DATA;
import static com.livemic.livemicapp.Constants.MSG_REGISTER_ACTIVITY;
import static com.livemic.livemicapp.Constants.MSG_SELECT_ERROR;
import static com.livemic.livemicapp.Constants.MSG_STARTCLIENT;
import static com.livemic.livemicapp.Constants.MSG_STARTSERVER;

/**
 * Created by Prayansh on 2018-01-13.
 */

public class ConnectionService extends Service implements WifiP2pManager.ChannelListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {  // callback of requestPeers{

    private static final String TAG = "PTP_Serv";

    private static ConnectionService _sinstance = null;

    private WorkHandler mWorkHandler;
    private MessageHandler mHandler;

    boolean retryChannel = false;

    LiveMicApp mApp;
    MainActivity mActivity;    // shall I use weak reference here ?
    ConnectionManager mConnMan;

    /**
     * @see android.app.Service#onCreate()
     */
    private void _initialize() {
        if (_sinstance != null) {
            Log.d(TAG, "_initialize, already initialized, do nothing.");
            return;
        }


        mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());

        mApp = (LiveMicApp) getApplication();
        mApp.mP2pMan = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
        Log.d(TAG, "_initialize, get p2p service and init channel !!!");

        mConnMan = new ConnectionManager(this);
        _sinstance = this;
    }

    public static ConnectionService getInstance() {
        return _sinstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _initialize();
        Log.d(TAG, "onCreate : done");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _initialize();
        processIntent(intent);
        return START_STICKY;
    }

    /**
     * process all wifi p2p intent caught by bcast recver.
     * P2P connection setup event sequence:
     * 1. after find, peers_changed to available, invited
     * 2. when connection established, this device changed to connected.
     * 3. for server, WIFI_P2P_CONNECTION_CHANGED_ACTION intent: p2p connected,
     * for client, this device changed to connected first, then CONNECTION_CHANGED
     * 4. WIFI_P2P_PEERS_CHANGED_ACTION: peer changed to connected.
     * 5. now both this device and peer are connected !
     * <p>
     * if select p2p server mode with create group, this device will be group owner automatically, with
     * 1. this device changed to connected
     * 2. WIFI_P2P_CONNECTION_CHANGED_ACTION
     */
    private void processIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "processIntent: " + intent.toString());

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {  // this devices's wifi direct enabled state.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
                AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "1");
                Log.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : enabled, re-init p2p channel to framework ");
            } else {
                mApp.mThisDevice = null;    // reset this device status
                mApp.mP2pChannel = null;
                mApp.mPeers.clear();
                Log.d(TAG, "processIntent : WIFI_P2P_STATE_CHANGED_ACTION : disabled, null p2p channel to framework ");
                if (mApp.mHomeActivity != null) {
//                    mApp.mHomeActivity.updateThisDevice(null);
                    mApp.mHomeActivity.resetData();
                }
                AppPreferences.setStringToPref(mApp, AppPreferences.PREF_NAME, AppPreferences.P2P_ENABLED, "0");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // a list of peers are available after discovery, use PeerListListener to collect
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            Log.d(TAG, "processIntent: WIFI_P2P_PEERS_CHANGED_ACTION: call requestPeers() to get list of peers");
            if (mApp.mP2pMan != null) {
                mApp.mP2pMan.requestPeers(mApp.mP2pChannel, (WifiP2pManager.PeerListListener) this);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mApp.mP2pMan == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION : " + networkInfo.getReason() + " : " + networkInfo.toString());
            if (networkInfo.isConnected()) {
                Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p connected ");
                // Connected with the other device, request connection info for group owner IP. Callback inside details fragment.
                mApp.mP2pMan.requestConnectionInfo(mApp.mP2pChannel, this);
            } else {
                Log.d(TAG, "processIntent: WIFI_P2P_CONNECTION_CHANGED_ACTION: p2p disconnected, mP2pConnected = false..closeClient.."); // It's a disconnect
                mApp.mP2pConnected = false;
                mApp.mP2pInfo = null;   // reset connection info after connection done.
                mConnMan.closeClient();

                if (mApp.mHomeActivity != null) {
                    mApp.mHomeActivity.resetData();
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // this device details has changed(name, connected, etc)
            mApp.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mApp.mDeviceName = mApp.mThisDevice.deviceName;
            Log.d(TAG, "processIntent: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " + mApp.mThisDevice.deviceName);
//            if (mApp.mHomeActivity != null) {
//                mApp.mHomeActivity.updateThisDevice(mApp.mThisDevice);
//            }
        }
    }

    /**
     * The channel to the framework Wifi P2p has been disconnected. could try re-initializing
     */
    @Override
    public void onChannelDisconnected() {
        if (!retryChannel) {
            Log.d(TAG, "onChannelDisconnected : retry initialize() ");
            mApp.mP2pChannel = mApp.mP2pMan.initialize(this, mWorkHandler.getLooper(), null);
            if (mApp.mHomeActivity != null) {
                mApp.mHomeActivity.resetData();
            }
            retryChannel = true;
        } else {
            Log.d(TAG, "onChannelDisconnected : stop self, ask user to re-enable.");
            if (mApp.mHomeActivity != null) {
                mApp.mHomeActivity.onChannelDisconnected();
            }
            stopSelf();
        }
    }

    /**
     * the callback of requestPeers upon WIFI_P2P_PEERS_CHANGED_ACTION intent.
     */
    @Override
    public void onPeersAvailable(final WifiP2pDeviceList peerList) {
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          HACKpeersAvailableDelayed(peerList);
        }
      }, 20);
    }

    private void HACKpeersAvailableDelayed(WifiP2pDeviceList peerList) {
        mApp.mPeers.clear();
        mApp.mPeers.addAll(peerList.getDeviceList());
        Log.d(TAG, "onPeersAvailable : update peer list...");

        WifiP2pDevice connectedPeer = mApp.getConnectedPeer();
        if (connectedPeer != null) {
            Log.d(TAG, "onPeersAvailable : exist connected peer : " + connectedPeer.deviceName);
        } else {

        }

        Log.d(TAG, "onPeersAvailable : missing info? " + (mApp.mP2pInfo == null));
        if (mApp.mP2pInfo != null) {
            Log.d(TAG, "onPeersAvailable : group Formed? " + mApp.mP2pInfo.groupFormed);
            Log.d(TAG, "onPeersAvailable : group Owner? " + mApp.mP2pInfo.isGroupOwner);

          if (mApp.mP2pInfo.groupFormed && mApp.mP2pInfo.isGroupOwner) {
                Log.d(TAG, "onPeersAvailable : device is groupOwner: startSocketServer");
                mApp.startSocketServer();
            } else if (mApp.mP2pInfo.groupFormed && connectedPeer != null) {
                // XXX client path goes to connection info available after connection established.
                // Log.d(TAG, "onConnectionInfoAvailable: device is client, connect to group owner: startSocketClient ");
                // mApp.startSocketClient(mApp.mP2pInfo.groupOwnerAddress.getHostAddress());
            }
        }

        if (mApp.mHomeActivity != null) {
            mApp.mHomeActivity.onPeersAvailable(peerList);
        }
    }

    /**
     * the callback of when the _Requested_ connectino info is available.
     * WIFI_P2P_CONNECTION_CHANGED_ACTION intent, requestConnectionInfo()
     */
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: " + info.groupOwnerAddress.getHostAddress());
        if (info.groupFormed && info.isGroupOwner) {
            // XXX server path goes to peer connected.
            //new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
            //Log.d(TAG, "onConnectionInfoAvailable: device is groupOwner: startSocketServer ");
            // mApp.startSocketServer();
        } else if (info.groupFormed) {
            Log.d(TAG, "onConnectionInfoAvailable: device is client, connect to group owner: startSocketClient ");
            mApp.startSocketClient(info.groupOwnerAddress.getHostAddress());
        }
        mApp.mP2pConnected = true;
        mApp.mP2pInfo = info;   // connection info available
    }

    private void enableStartChatActivity() {
        if (mApp.mHomeActivity != null) {
            Log.d(TAG, "enableStartChatActivity :  nio channel ready, enable start chat !");
            mApp.mHomeActivity.onConnectionInfoAvailable(mApp.mP2pInfo);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * message handler looper to handle all the msg sent to location manager.
     */
    final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }

    /**
     * the launch_connect message process loop.
     */
    private void processMessage(final android.os.Message msg) {
        Log.d(TAG, "processMessage: what = " + msg.what);
        switch (msg.what) {
            case MSG_NULL:
                break;
            case MSG_REGISTER_ACTIVITY:
                Log.d(TAG, "processMessage: onActivityRegister to chat fragment...");
                onActivityRegister((MainActivity) msg.obj, msg.arg1);
                break;
            case MSG_STARTSERVER:
                Log.d(TAG, "processMessage: startServerSelector...");
                if (mConnMan.startServerSelector() >= 0) {
                    enableStartChatActivity();
                }
                break;
            case MSG_STARTCLIENT:
                Log.d(TAG, "processMessage: startClientSelector...");
                final String host = (String) msg.obj;
                new Timer().schedule(new TimerTask() {
                  @Override
                  public void run() {
                    if (mConnMan.startClientSelector(host) >= 0) {
                      enableStartChatActivity();
                    }
                  }
                }, 2000);
                break;
            case MSG_NEW_CLIENT:
                Log.d(TAG, "processMessage:  onNewClient...");
                mConnMan.onNewClient((SocketChannel) msg.obj);
                break;
            case MSG_FINISH_CONNECT:
                Log.d(TAG, "processMessage:  onFinishConnect...");
                mConnMan.onFinishConnect((SocketChannel) msg.obj);
                break;
            case MSG_PULLIN_DATA:
                Log.d(TAG, "processMessage:  onPullIndata ...");
                onPullInData((SocketChannel) msg.obj, msg.getData());
                break;
            case MSG_PUSHOUT_DATA:
                Log.d(TAG, "processMessage: onPushOutData...");
                onPushOutData((Serializable) msg.obj);
                break;
            case MSG_SELECT_ERROR:
                Log.d(TAG, "processMessage: onSelectorError...");
                mConnMan.onSelectorError();
                break;
            case MSG_BROKEN_CONN:
                Log.d(TAG, "processMessage: onBrokenConn...");
                mConnMan.onBrokenConn((SocketChannel) msg.obj);
                break;
            default:
                break;
        }
    }

    /**
     * register the activity that uses this service.
     */
    private void onActivityRegister(MainActivity activity, int register) {
        Log.d(TAG, "onActivityRegister : activity register itself to service : " + register);
        if (register == 1) {
            mActivity = activity;
        } else {
            mActivity = null;    // set to null explicitly to avoid mem leak.
        }
    }

    /**
     * service handle data in come from socket channel
     */
    private Serializable onPullInData(SocketChannel schannel, Bundle b) {
        Serializable data = b.getSerializable(Constants.KEY_DATA);
        Log.d(TAG, "onDataIn : recvd msg : " + data);
        mConnMan.onDataIn(schannel, data);  // pub to all client if this device is server.
        MessageObject msg = (MessageObject) data;

        if (mActivity != null) {
          mActivity.handleMessageReceived(msg);
        }
        return data;
    }

    /**
     * handle data push out request.
     * If the sender is the server, pub to all client.
     * If the sender is client, only can send to the server.
     */
    private void onPushOutData(Serializable data) {
        Log.d(TAG, "onPushOutData : " + data);
        mConnMan.pushOutData(data);
    }

    /**
     * sync call to send data using conn man's channel, as conn man now is blocking on select
     */
    public int connectionSendData(String jsonstring) {
        Log.d(TAG, "connectionSendData : " + jsonstring);
        new SendDataAsyncTask(mConnMan, jsonstring).execute();
        return 0;
    }

    /**
     * write data in an async task to avoid NetworkOnMainThreadException.
     */
    public class SendDataAsyncTask extends AsyncTask<Void, Void, Integer> {
        private Serializable data;
        private ConnectionManager connman;

        public SendDataAsyncTask(ConnectionManager conn, Serializable dataObject) {
            connman = conn;
            data = dataObject;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return connman.pushOutData(data);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "SendDataAsyncTask : onPostExecute:  " + data + " len: " + result);
        }
    }
    /**
     * send a notification upon recv data, click the notification will bcast the pending intent, which
     * will launch the chatactivity fragment.
     */
/*    public void showNotification(MessageObject row) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_action_discover, row.mMsg, System.currentTimeMillis());
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        CharSequence title = row.mSender;
        CharSequence text = row.mMsg;

        //Intent notificationIntent = new Intent(this, WiFiDirectActivity.class);
        Intent notificationIntent = mApp.getLaunchActivityIntent(MainActivity.class, row.mMsg);
        // pendingIntent that will start a new activity.
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

    	  notification.setLatestEventInfo(this, title, text, contentIntent);
        notificationManager.notify(1, notification);
        Log.d(TAG, "showNotification: " + row.mMsg);
    }
*/

    /**
     * show the message in activity
     */
/*    private void showInActivity(final MessageObject row) {
        Log.d(TAG, "showInActivity : " + row.mMsg);
        if (mActivity != null) {
            mActivity.showMessage(row);
        } else {
            if (mApp.mHomeActivity != null && mApp.mHomeActivity.mHasFocus == true) {
                Log.d(TAG, "showInActivity :  chat activity down, force start only when home activity has focus !");
                mApp.mHomeActivity.startPresenterActivity(row.mMsg);
            } else {
                Log.d(TAG, "showInActivity :  Home activity down, do nothing, notification will launch it...");
            }
        }
    }
*/
    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                //Log.d(TAG, "getDeviceStatus : AVAILABLE");
                return "Available";
            case WifiP2pDevice.INVITED:
                //Log.d(TAG, "getDeviceStatus : INVITED");
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                //Log.d(TAG, "getDeviceStatus : CONNECTED");
                return "Connected";
            case WifiP2pDevice.FAILED:
                //Log.d(TAG, "getDeviceStatus : FAILED");
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                //Log.d(TAG, "getDeviceStatus : UNAVAILABLE");
                return "Unavailable";
            default:
                return "Unknown = " + deviceStatus;
        }
    }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mHandler.removeMessages(MSG_NULL);
    mHandler.removeMessages(MSG_STARTSERVER);
    mHandler.removeMessages(MSG_STARTCLIENT);
    mHandler.removeMessages(MSG_CONNECT);
    mHandler.removeMessages(MSG_DISCONNECT);
    mHandler.removeMessages(MSG_PUSHOUT_DATA);
    mHandler.removeMessages(MSG_NEW_CLIENT);
    mHandler.removeMessages(MSG_FINISH_CONNECT);
    mHandler.removeMessages(MSG_PULLIN_DATA);
    mHandler.removeMessages(MSG_REGISTER_ACTIVITY);
  }
}