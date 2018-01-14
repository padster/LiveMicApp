/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.livemic.livemicapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends AppCompatActivity implements DeviceListFragment.DeviceActionListener {

  String[] permissions = new String[]{
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.ACCESS_WIFI_STATE,
      Manifest.permission.CHANGE_WIFI_STATE,
      Manifest.permission.CHANGE_NETWORK_STATE,
      Manifest.permission.INTERNET,
      Manifest.permission.ACCESS_NETWORK_STATE,
      Manifest.permission.READ_PHONE_STATE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

    public static final String TAG = "PTP_Activity";

    LiveMicApp mApp = null;

    boolean mHasFocus = false;
    private boolean retryChannel = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        maybeRequestPermissions();

        setContentView(R.layout.launch_connect);   // statically draw two <fragment class=>


        mApp = (LiveMicApp) getApplication();

        mApp.mHomeActivity = this;

        // If service not started yet, start it.
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        startService(serviceIntent);  // start the connection service

        Log.d(TAG, "onCreate : home activity launched, start service anyway.");
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        mHasFocus = true;
        if (mApp.mThisDevice != null) {
//            Log.d(TAG, "onResume : redraw this device details");
//            updateThisDevice(mApp.mThisDevice);

            // if p2p connetion info available, and my status is connected, enabled start chatting !
            if (mApp.mP2pInfo != null && mApp.mThisDevice.status == WifiP2pDevice.CONNECTED) {
                Log.d(TAG, "onResume : redraw detail fragment");
                onConnectionInfoAvailable(mApp.mP2pInfo);
            } else {
                // XXX stop client, if any.
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {  // the activity is no long visible
        super.onStop();
        mHasFocus = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mApp.mHomeActivity = null;
        Log.d(TAG, "onDestroy: reset app home activity.");
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
                if (fragmentList != null) {
                    fragmentList.clearPeers();
                }
                if (fragmentDetails != null) {
                    fragmentDetails.resetViews();
                }
            }
        });
    }

    /**
     * process WIFI_P2P_THIS_DEVICE_CHANGED_ACTION intent, refresh this device.
     *
    public void updateThisDevice(final WifiP2pDevice device) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                fragment.updateThisDevice(device);
            }
        });
    }
     */

    /**
     * update the device list fragment.
     */
    public void onPeersAvailable(final WifiP2pDeviceList peerList) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                fragmentList.onPeersAvailable(mApp.mPeers);  // use application cached list.
                DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);

                for (WifiP2pDevice d : peerList.getDeviceList()) {
                    if (d.status == WifiP2pDevice.FAILED) {
                        Log.d(TAG, "onPeersAvailable: Peer status is failed " + d.deviceName);
                        fragmentDetails.resetViews();
                    }
                }
            }
        });
    }

    /**
     * handle p2p connection available, update UI.
     */
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
                fragmentDetails.onConnectionInfoAvailable(info);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /**
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:   // using app icon for navigation up or home:
                Log.d(TAG, " navigating up or home clicked.");
                // startActivity(new Intent(home.class, Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

            case R.id.atn_direct_enable:
                if (!mApp.isP2pEnabled()) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, " WiFi direct support : already enabled. ");
                }
                return true;
            case R.id.atn_direct_discover:
                if (!mApp.isP2pEnabled()) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning, Toast.LENGTH_LONG).show();
                    return true;
                }

                // show progressbar when discoverying.
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();

                Log.d(TAG, "onOptionsItemSelected : start discovering ");
                mApp.mP2pMan.discoverPeers(mApp.mP2pChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onOptionsItemSelected : discovery succeed... ");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(TAG, "onOptionsItemSelected : discovery failed !!! " + reasonCode);
//                        fragment.clearPeers();
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed, try again... ", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;

            case R.id.disconnect:
                Log.d(TAG, "onOptionsItemSelected : disconnect all connections and stop server ");
                ConnectionService.getInstance().mConnMan.closeClient();
                ConnectionService.getInstance().mConnMan.closeServer();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * user taps on peer from discovered list of peers, show this peer's detail.
     */
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    /**
     * user clicked connect button after discover peers.
     */
    public void connect(WifiP2pConfig config) {
        Log.d(TAG, "connect : connect to server : " + config.deviceAddress);
        // perform p2p connect upon users click the connect button. after connection, manager request connection info.
        mApp.mP2pMan.connect(mApp.mP2pChannel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.i(TAG, "Successfully connected");
                Toast.makeText(WiFiDirectActivity.this, "Connect success..", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "Failed to connect");
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * user clicked disconnect button, disconnect from group owner.
     */
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        Log.d(TAG, "disconnect : removeGroup ");
        mApp.mP2pMan.removeGroup(mApp.mP2pChannel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason : 1=error, 2=busy; " + reasonCode);
                Toast.makeText(WiFiDirectActivity.this, "disconnect failed.." + reasonCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnect succeed. ");
                fragment.getView().setVisibility(View.GONE);
            }
        });
    }

    /**
     * The channel to the framework(WiFi direct) has been disconnected.
     * This is diff than the p2p connection to group owner.
     */
    public void onChannelDisconnected() {
        Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.", Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("WiFi Direct down, please re-enable WiFi Direct")
                .setCancelable(true)
                .setPositiveButton("Re-enable WiFi Direct", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });

        AlertDialog info = builder.create();
        info.show();
    }

    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (mApp.mP2pMan != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE || fragment.getDevice().status == WifiP2pDevice.INVITED) {
                mApp.mP2pMan.cancelConnect(mApp.mP2pChannel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "cancelConnect : success canceled...");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "cancelConnect: request failed. Please try again.. ", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "cancelConnect : cancel connect request failed..." + reasonCode);
                    }
                });
            }
        }
    }

    /**
     * launch chat activity
     */
    public void startPresenterActivity(final String initMsg) {
        if (!mApp.mP2pConnected) {
            Log.d(TAG, "startPresenterActivity : p2p connection is missing, do nothng...");
            return;
        }

        Log.d(TAG, "startPresenterActivity : start chat activity fragment..." + initMsg);
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Intent i = mApp.getLaunchActivityIntent(MainActivity.class, initMsg);
                startActivity(i);
            }
        });
    }


    ///
    // Permissions stuff
    ///
    private void maybeRequestPermissions() {
      //check API version, do nothing if API version < 23!
      int currentapiVersion = android.os.Build.VERSION.SDK_INT;
      if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){
        List<String> needed = new ArrayList<>();
        for (String permission : permissions) {
          if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(permission);
          }
        }
        if (!needed.isEmpty()) {
          String[] asArray = needed.toArray(new String[0]);
          ActivityCompat.requestPermissions(this, asArray, 1);
        }

      /*
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
          // Show an expanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.
        } else {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
      }
      */
      }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
      switch (requestCode) {
        case 1: {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Activity", "Granted!");
          } else {
            Log.d("Activity", "Denied!");
            finish();
          }
          return;
        }
      }
    }

    /** Use this app as a normal mic, no network. Sound comes in mic, out headphone jack. */
    public void useLocalMic(View view) {
      Log.d(TAG, "useLocalMic ");
      Intent i = mApp.getLaunchActivityIntent(MainActivity.class, "");
      i.putExtra(Constants.LOCAL_ONLY_TAG, true);
      startActivity(i);
    }
}
