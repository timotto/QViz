package com.ubergrund.qviz;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.Visualizer;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.google.tungsten.ledcommon.ILedService;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/22/13
 * Time: 6:28 PM
 */
public class QVizService extends Service {

    private static final String TAG = "QViz/QVizService";

    private static final String mName = "QViz";

    private int leds = 32;
    private int maxR = 255, maxG = 32, maxB = 128;

    private final Binder mBinder = new Binder();
    private Visualizer mVisualizer;
    private ILedService mLedService;
    private boolean isConnected;

    private float mDelay = 0;
    private int currentLED = 0;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        mVisualizer = new Visualizer(0);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {

            private long nextPrint = 0;
            private long lastWav = 0;

            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {

                final float values[] = new float[leds];
                final int sectionLength = bytes.length / leds;
                for(int i=0;i<leds;i++) {
                    float avg = 0;
                    for(int j=0;j<sectionLength;j++)
                        avg += bytes[i*sectionLength + j];
//                        avg += Math.abs(bytes[i*sectionLength + j]);

                    avg /= sectionLength;
                    values[i] = avg;

                    int r,g,b;
                    r = (int) (maxR * values[i]);
                    g = (int) (maxG * values[i]);
                    b = (int) (maxB * values[i]);
                    if (isConnected)
                        try {
                            mLedService.setLed(mBinder, i, r, g, b);
                        } catch (RemoteException e) {
                            Log.e(TAG, "LED Error", e);

                        }
                }

                final long now = System.currentTimeMillis();
                if (lastWav != 0) {
                    mDelay = 0.8f * mDelay + 0.2f * (float)(now - lastWav);
                }
                lastWav = now;

                if (now < nextPrint)
                    return;

                nextPrint = now + 2000;

//                final StringBuilder sb = new StringBuilder();
//                sb.append(values[0]);
//                for(int i=1;i<leds;i++) {
//                    sb.append(",");
//                    sb.append(values[i]);
//                }
                final StringBuilder sb = new StringBuilder();
                sb.append(bytes[0]);
                for(int i=1;i<bytes.length;i++) {
                    sb.append(",");
                    sb.append(Byte.toString(bytes[i]));
                }
                Log.d(TAG, "["+mDelay+"] onWaveFormDataCapture("+bytes.length+", "+samplingRate+") = " + sb.toString());

            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
//                updateLEDs(bytes, samplingRate);

            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);

        mVisualizer.setEnabled(true);
        connectLedService();
    }

    private void updateLEDs(byte[] bytes, int samplingRate) {
        Log.d(TAG, "updateLEDs(..., "+samplingRate+")");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        disconnectLedService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Connects to the LED service if not already connected.
     */
    private void connectLedService()
    {
        Log.d(TAG, "connect");

        if(!this.isConnected ) {
            bindService(new Intent("com.google.tungsten.LedService"), ledClientServiceConnection,
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
                this.disableLedService();
            }

            // Unbind service
            unbindService(ledClientServiceConnection);

            this.isConnected = false;
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

    private final ServiceConnection ledClientServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            isConnected = true;
            mLedService = ILedService.Stub.asInterface(iBinder);
            try {
                leds = mLedService.getLedCount();
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
