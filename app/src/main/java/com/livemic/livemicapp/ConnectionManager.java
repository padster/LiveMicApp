package com.livemic.livemicapp;

import android.content.Context;
import android.util.Log;

import com.livemic.livemicapp.ConnectionService;
import com.livemic.livemicapp.LiveMicApp;
import com.livemic.livemicapp.wifidirect.SelectorAsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * this class encapsulate the NIO buffer and NIO channel on top of socket. It is all abt NIO style.
 * SSLServerSocketChannel, ServerSocketChannel, SocketChannel, Selector, ByteBuffer, etc.
 * NIO buffer (ByteBuffer) either in writing mode or in reading mode. Need to flip the mode before reading or writing.
 * <p>
 * You know when a socket channel disconnected when you read -1 or write exception. You need app level ACK.
 */
public class ConnectionManager {
    public static final int PORT = 1080;

    private final String TAG = "PTP_ConnMan";

    private Context mContext;
    ConnectionService mService;
    LiveMicApp mApp;

    // Server knows all clients. key is ip addr, value is socket channel.
    // when remote client screen on, a new connection with the same ip addr is established.
    private Map<String, SocketChannel> mClientChannels = new HashMap<String, SocketChannel>();

    // global selector and channels
    private Selector mClientSelector = null;
    private Selector mServerSelector = null;
    private ServerSocketChannel mServerSocketChannel = null;
    private SocketChannel mClientSocketChannel = null;
    String mClientAddr = null;
    String mServerAddr = null;

    /**
     * constructor
     */
    public ConnectionManager(ConnectionService service) {
        mService = service;
        mApp = (LiveMicApp) mService.getApplication();
    }

    public void configIPV4() {
        // by default Selector attempts to work on IPv6 stack.
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    /**
     * create a server socket channel to listen to the port for incoming connections.
     */
    public static ServerSocketChannel createServerSocketChannel(int port) throws IOException {
        Log.i("SOCKET", "~~~~~~~~~ CREATING SERVER on " + port);
        // Create a non-blocking socket channel
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ServerSocket serverSocket = ssChannel.socket();
        serverSocket.bind(new InetSocketAddress(port));  // bind to the port to listen.
        return ssChannel;
    }

    /**
     * Creates a non-blocking socket channel to connect to specified host name and port.
     * connect() is called on the new channel before it is returned.
     */
    public static SocketChannel createSocketChannel(String hostName, int port) throws IOException {
        Log.i("SOCKET", "~~~~~~~~~ CONNECTING CLIENT at " + hostName + " / " + port);
        // Create a non-blocking socket channel
        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(false);

        // Send a connection request to the server; this method is non-blocking
        sChannel.connect(new InetSocketAddress(hostName, port));
        return sChannel;
    }


    /**
     * create a socket channel and connect to the host.
     * after return, the socket channel guarantee to be connected.
     */
    public SocketChannel connectTo(String hostname, int port) throws Exception {
        SocketChannel sChannel = null;

        sChannel = createSocketChannel(hostname, port);  // connect to the remote host, port

        // Before the socket is usable, the connection must be completed. finishConnect().
        while (!sChannel.finishConnect()) {
            // blocking spin lock
        }

        // Socket channel is now ready to use
        return sChannel;
    }

    /**
     * client, after p2p connection available, connect to group owner and select monitoring the sockets.
     * start blocking selector monitoring in an async task, infinite loop
     */
    public int startClientSelector(String host) {
        closeServer();   // close linger server.

        if (mClientSocketChannel != null) {
            Log.d(TAG, "startClientSelector : client already connected to server: " + mClientSocketChannel.socket().getLocalAddress().getHostAddress());
            return -1;
        }

        try {
            // connected to the server upon start client.

            SocketChannel sChannel = connectTo(host, PORT);

            mClientSelector = Selector.open();
            mClientSocketChannel = sChannel;
            mClientAddr = mClientSocketChannel.socket().getLocalAddress().getHostName();
            sChannel.register(mClientSelector, SelectionKey.OP_READ);
            mApp.setMyAddr(mClientAddr);
            mApp.clearMessages();
            Log.d(TAG, "startClientSelector : started: " + mClientSocketChannel.socket()
                    .getLocalAddress().getHostAddress());

            // start selector monitoring, blocking
            new SelectorAsyncTask(mService, mClientSelector).execute();
            return 0;

        } catch (Exception e) {
            Log.e(TAG, "startClientSelector : exception: " + e.toString());
            e.printStackTrace();
            mClientSelector = null;
            mClientSocketChannel = null;
            mApp.setMyAddr(null);

            return -1;
        }
    }

    /**
     * create a selector to manage a server socket channel
     * The registration process yields an object called a selection key which identifies the selector/socket channel pair
     */
    public int startServerSelector() {
        closeClient();   // close linger client, if exists.

        try {
            // create server socket and register to selector to listen OP_ACCEPT event
            ServerSocketChannel sServerChannel = createServerSocketChannel(PORT); // BindException if already bind.
            mServerSocketChannel = sServerChannel;
            mServerAddr = mServerSocketChannel.socket().getInetAddress().getHostAddress();
            if ("0.0.0.0".equals(mServerAddr)) {
                mServerAddr = "Master";
            }
            ((LiveMicApp) mService.getApplication()).setMyAddr(mServerAddr);

            mServerSelector = Selector.open();
            SelectionKey acceptKey = sServerChannel.register(mServerSelector, SelectionKey.OP_ACCEPT);
            acceptKey.attach("accept_channel");
            mApp.mIsServer = true;

            //SocketChannel sChannel = createSocketChannel("hostname.com", 80);
            //sChannel.register(selector, SelectionKey.OP_CONNECT);  // listen to connect event.
            Log.d(TAG, "startServerSelector : started: " + sServerChannel.socket().getLocalSocketAddress().toString());

            new SelectorAsyncTask(mService, mServerSelector).execute();
            return 0;

        } catch (Exception e) {
            Log.e(TAG, "startServerSelector : exception: " + e.toString());
            return -1;
        }
    }

    /**
     * handle selector error, re-start
     */
    public void onSelectorError() {
        Log.e(TAG, " onSelectorError : do nothing for now.");
        // new SelectorAsyncTask(mService, mSelector).execute();
    }

    /**
     * a device can only be either group owner, or group client, not both.
     * when we start as client, close server, if existing due to linger connection.
     */
    public void closeServer() {
        if (mServerSocketChannel != null) {
            try {
                mServerSocketChannel.close();
                mServerSelector.close();
            } catch (Exception e) {

            } finally {
                mApp.mIsServer = false;
                mServerSocketChannel = null;
                mServerSelector = null;
                mServerAddr = null;
                mClientChannels.clear();
            }
        }
    }

    public void closeClient() {
        if (mClientSocketChannel != null) {
            try {
                mClientSocketChannel.close();
                mClientSelector.close();
            } catch (Exception e) {

            } finally {
                mClientSocketChannel = null;
                mClientSelector = null;
                mClientAddr = null;
            }
        }
    }

    /**
     * read out -1, connection broken, remove it from clients collection
     */
    public void onBrokenConn(SocketChannel schannel) {
        try {
            String peeraddr = schannel.socket().getInetAddress().getHostAddress();
            if (mApp.mIsServer) {
                mClientChannels.remove(peeraddr);
                Log.d(TAG, "onBrokenConn : client down: " + peeraddr);
            } else {
                Log.d(TAG, "onBrokenConn : set null client channel after server down: " + peeraddr);
                closeClient();
            }
            schannel.close();
        } catch (Exception e) {
            Log.e(TAG, "onBrokenConn: close channel: " + e.toString());
        }
    }

    /**
     * Server handle new client coming in.
     */
    public void onNewClient(SocketChannel schannel) {
        String ipaddr = schannel.socket().getInetAddress().getHostAddress();
        Log.d(TAG, "onNewClient : server added remote client: " + ipaddr);
        mClientChannels.put(ipaddr, schannel);
    }

    /**
     * Client's connect to server success,
     */
    public void onFinishConnect(SocketChannel schannel) {
        String clientaddr = schannel.socket().getLocalAddress().getHostAddress();
        String serveraddr = schannel.socket().getInetAddress().getHostAddress();
        Log.d(TAG, "onFinishConnect : client connect to server succeed : " + clientaddr + " -> " + serveraddr);
        mClientSocketChannel = schannel;
        mClientAddr = clientaddr;
        ((LiveMicApp) mService.getApplication()).setMyAddr(mClientAddr);
    }

    /**
     * client send data into server, server pub to all clients.
     */
    public void onDataIn(SocketChannel schannel, Serializable data) {
        Log.d(TAG, "connection onDataIn : " + data);
        if (mApp.mIsServer) {  // push all _other_ clients if the device is the server
            pubDataToAllClients(data, schannel);
        }
    }

    /**
     * write byte buf to the socket channel.
     */
    private int writeData(SocketChannel sChannel, Serializable dataObject) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        int nwritten = 0;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(dataObject);
            out.flush();
            byte[] buf = bos.toByteArray();
            ByteBuffer bytebuf = ByteBuffer.wrap(buf);  // wrap the buf into byte buffer
            //bytebuf.flip();  // no flip after creating from wrap.
            Log.d(TAG, "writeData: start:limit = " + bytebuf.position() + " : " + bytebuf.limit());
            nwritten = sChannel.write(bytebuf);
            Log.d(TAG, "writeData: content: " + new String(buf) + "  : len: " + nwritten);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // Connection may have been closed
            Log.e(TAG, "writeData: exception : " + e.toString());
            onBrokenConn(sChannel);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return nwritten;
    }

    /**
     * server publish data to all the connected clients
     */
    private void pubDataToAllClients(Serializable msg, SocketChannel incomingChannel) {
        Log.d(TAG, "pubDataToAllClients : isServer ? " + mApp.mIsServer + " msg: " + msg);
        if (!mApp.mIsServer) {
            return;
        }

        for (SocketChannel s : mClientChannels.values()) {
            if (s != incomingChannel) {
                String peeraddr = s.socket().getInetAddress().getHostAddress();
                Log.d(TAG, "pubDataToAllClients : Server pub data to:  " + peeraddr);
                writeData(s, msg);
            }
        }
    }

    /**
     * the device want to push out data.
     * If the device is client, the only channel is to the server.
     * If the device is server, it just pub the data to all clients for now.
     */
    public int pushOutData(Serializable dataObject) {
        if (!mApp.mIsServer) {   // device is client, can only send to server
            sendDataToServer(dataObject);
        } else {
            // server pub to all clients, msg already appended with sender addr inside send button handler.
            pubDataToAllClients(dataObject, null);
        }
        return 0;
    }

    /**
     * whenever client write to server, carry the format of "client_addr : msg "
     */
    private int sendDataToServer(Serializable dataObject) {
        if (mClientSocketChannel == null) {
            Log.d(TAG, "sendDataToServer: channel not connected ! waiting...");
            return 0;
        }
        Log.d(TAG, "sendDataToServer: " + mClientAddr + " -> " + mClientSocketChannel.socket().getInetAddress().getHostAddress() + " : " + dataObject);
        return writeData(mClientSocketChannel, dataObject);
    }
}
