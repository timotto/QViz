package com.ubergrund.qviz.renderer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.google.tungsten.ledcommon.ILedService;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/23/13
 * Time: 9:08 AM
 */
public abstract class AbstractRenderer {

    protected static final String TAG = "QViz/AbstractRenderer";
    protected static final String mName = "QViz";

    protected Context context;
    protected final Binder mBinder = new Binder();
    protected int leds = 32;
    protected ILedService mLedService;
    protected boolean isConnected;

//    private int magnitudePoints[] = new int[leds];


    abstract public void onFft(byte[] data, int sampleRate);

    protected void onConnected(){}

    public AbstractRenderer(Context context) {
        this.context = context;
    }

    public void start() {
        connectLedService();
    }

    public void stop() {
        disconnectLedService();
    }

    /**
     * Connects to the LED service if not already connected.
     */
    private void connectLedService()
    {
        Log.d(TAG, "connect");

        if(!this.isConnected ) {
            context.bindService(new Intent("com.google.tungsten.LedService"), ledClientServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Disconnects from the LED service if connected and unbinds the service.
     */
    private void disconnectLedService()
    {
        Log.d(TAG, "disconnect");

        if(this.isConnected) {
            // Disable the client connection if have service
            if(mLedService != null) {
                disableLedService();
            }

            // Unbind service
            context.unbindService(ledClientServiceConnection);

            isConnected = false;
        }
    }

    /**
     * Enables the client with the given priority.
     *
     * @param priority The client application's priority. Higher numbers represent higher priority.
     */
    private void enableLedService(int priority)
    {
        // Verify service is connected
        assertService();

        try {
            mLedService.enable(mBinder, mName, priority);
        } catch (RemoteException e) {
            Log.e(TAG, "enable failed");
        }
    }

    /**
     * Stops the LED client.
     */
    private void disableLedService()
    {
        // Verify service is connected
        assertService();

        try {
            mLedService.disable(mBinder);
        } catch (RemoteException e) {
            Log.e(TAG, "disableLedService failed");
        }
    }

    /**
     * Throws an IllegalStateException exception if the service is not connected.
     */
    private void assertService() {

        if(mLedService == null) {
            throw new IllegalStateException("LedClient not connected");
        }
    }

    protected final ServiceConnection ledClientServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            isConnected = true;
            mLedService = ILedService.Stub.asInterface(iBinder);
            try {
                leds = mLedService.getLedCount();
                onConnected();
            } catch (RemoteException e) {
                Log.e(TAG, "Error getting LED count", e);
                leds = 0;
            }
            enableLedService(7);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            isConnected = false;
            mLedService = null;
        }
    };


}
