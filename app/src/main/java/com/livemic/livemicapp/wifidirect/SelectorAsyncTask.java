package com.livemic.livemicapp.wifidirect;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.livemic.livemicapp.ConnectionService;
import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.model.MessageObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Exchanger;

import static com.livemic.livemicapp.Constants.MSG_BROKEN_CONN;
import static com.livemic.livemicapp.Constants.MSG_FINISH_CONNECT;
import static com.livemic.livemicapp.Constants.MSG_NEW_CLIENT;
import static com.livemic.livemicapp.Constants.MSG_PULLIN_DATA;
import static com.livemic.livemicapp.Constants.MSG_SELECT_ERROR;

/**
 * AsyncTask is only for UI thread to submit work(impl inside doInbackground) to executor.
 * The threading rule of AsyncTask: create/load/execute by UI thread only _ONCE_. can be callabled.
 */

/**
 * the selector only monitors OP_CONNECT and OP_READ. Do not monitor OP_WRITE as a channel is always writable.
 * Upon event out, either accept a connection, or read the data from the channel.
 * The writing to the channel is done inside the connection service launch_connect thread.
 */

public class SelectorAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "PTP_SEL";

    private ConnectionService mConnService;
    private Selector mSelector;

    public SelectorAsyncTask(ConnectionService connservice, Selector selector) {
        mConnService = connservice;
        mSelector = selector;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        select();
        return null;
    }

    private void select() {
        // Wait for events looper
        while (true) {
            try {
                Log.d(TAG, "select : selector monitoring: ");
                mSelector.select();   // blocked on waiting for event

                Log.d(TAG, "select : selector evented out: ");
                // Get list of selection keys with pending events, and process it.
                Iterator<SelectionKey> keys = mSelector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    // Get the selection key, and remove it from the list to indicate that it's being processed
                    SelectionKey selKey = keys.next();
                    keys.remove();
                    Log.d(TAG, "select : selectionkey: " + selKey.attachment());

                    try {
                        processSelectionKey(mSelector, selKey);  // process the selection key.
                    } catch (IOException e) {
                        selKey.cancel();
                        Log.e(TAG, "select : io exception in processing selector event: " + e.toString());
                    }
                }
            } catch (Exception e) {  // catch all exception in select() and the following ops in mSelector.
                Log.e(TAG, "Exception in selector: " + e.toString());
                notifyConnectionService(MSG_SELECT_ERROR, null, null);
                break;
            }
        }
    }

    /**
     * process the event popped to the selector
     */
    public void processSelectionKey(Selector selector, SelectionKey selKey) throws IOException {
        if (selKey.isValid() && selKey.isAcceptable()) {  // there is a connection to the server socket channel
            ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
            SocketChannel sChannel = ssChannel.accept();  // accept the connect and get a new socket channel.
            sChannel.configureBlocking(false);

            // let the selector monitor read/write the accepted connections.
            SelectionKey socketKey = sChannel.register(selector, SelectionKey.OP_READ);
            socketKey.attach("accepted_client " + sChannel.socket().getInetAddress().getHostAddress());
            Log.d(TAG, "processSelectionKey : accepted a client connection: " + sChannel.socket().getInetAddress().getHostAddress());
            notifyConnectionService(MSG_NEW_CLIENT, sChannel, null);
        } else if (selKey.isValid() && selKey.isConnectable()) {   // client connect to server got the response.
            SocketChannel sChannel = (SocketChannel) selKey.channel();

            boolean success = sChannel.finishConnect();
            if (!success) {
                // An error occurred; unregister the channel.
                selKey.cancel();
                Log.e(TAG, " processSelectionKey : finish connection not success !");
            }
            Log.d(TAG, "processSelectionKey : this client connect to remote success: ");
            notifyConnectionService(MSG_FINISH_CONNECT, sChannel, null);
            //mOutChannels.put(Integer.toString(sChannel.socket().getLocalPort()), sChannel);
        } else if (selKey.isValid() && selKey.isReadable()) {
            // Get channel with bytes to read
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            Log.d(TAG, "processSelectionKey : remote client is readable, read data: " + selKey.attachment());
            // we can retrieve the key we attached earlier, so we now what to do / where the data is coming from
            // MyIdentifierType myIdentifier = (MyIdentifierType)key.attachment();
            // myIdentifier.readTheData();
            doReadable(sChannel);
        } else if (selKey.isValid() && selKey.isWritable()) {
            // Not select on writable...endless loop.
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            Log.d(TAG, "processSelectionKey : remote client is writable, write data: ");
        }
    }

    /**
     * handle the readable event from selector
     */
    public void doReadable(SocketChannel schannel) {
        Serializable data = readData(schannel);
        if (data != null) {
            Bundle b = new Bundle();
//            Log.d(TAG, "doReadable : " + (MessageObject) data);
            b.putSerializable(Constants.KEY_DATA, data);
            notifyConnectionService(MSG_PULLIN_DATA, schannel, b);
        }
    }

    /**
     * read data when OP_READ event
     */
    public Serializable readData(SocketChannel sChannel) {
        ByteBuffer buf = ByteBuffer.allocate(1024 * 10);   // let's cap json string to 10k for now.

        // Each packet should be ~8kb
//        byte[] bytes = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
            buf.clear();  // Clear the buffer and read bytes from socket

            /*
            int numBytesRead = sChannel.read(buf);
            if (numBytesRead == -1) {
                // read -1 means socket channel is broken. remove it from the selector
                Log.e(TAG, "readData : channel closed due to read -1: ");
                sChannel.close();  // close the channel.
                notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
                // sChannel.close();
            } else {
                Log.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
                buf.flip();  // make buffer ready for read by flipping it into read mode.
                Log.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
                bytes = new byte[buf.limit()];  // use bytes.length will cause underflow exception.
                buf.get(bytes);
                // while ( buf.hasRemaining() ) buf.get();
            }
            */
            int read;
            while( (read = sChannel.read(buf)) > 0 ) {
              buf.flip();
              byte[] bytesLocal = new byte[buf.limit()];
              buf.get(bytesLocal);
              baos.write(bytesLocal);
              buf.clear();
            }


        } catch (Exception e) {
            Log.e(TAG, "readData : exception: " + e.toString());
            notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
        }
        byte[] bytes = baos.toByteArray();
        Log.i(TAG, "readData : RECV: " + bytes.length + " bytes");

        boolean cannotDeserialize = false;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        Serializable deserializedObject = null;
        try {
            in = new ObjectInputStream(bis);
            deserializedObject = (Serializable) in.readObject();
        } catch (Exception e) {
            cannotDeserialize = true;
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return cannotDeserialize ? null : deserializedObject;
    }

    /**
     * notify connection manager event
     */
    private void notifyConnectionService(int what, Object obj, Bundle data) {
        Handler hdl = mConnService.getHandler();
        Message msg = hdl.obtainMessage();
        msg.what = what;

        if (obj != null) {
            msg.obj = obj;
        }
        if (data != null) {
            msg.setData(data);
        }
        hdl.sendMessage(msg);
    }

}
