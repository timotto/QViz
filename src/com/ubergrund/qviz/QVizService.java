package com.ubergrund.qviz;

import android.app.Service;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.os.IBinder;
import android.util.Log;
import com.ubergrund.qviz.renderer.AbstractRenderer;
import com.ubergrund.qviz.renderer.SimpleRenderer;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/22/13
 * Time: 6:28 PM
 */
public class QVizService extends Service {

    private static final String TAG = "QViz/QVizService";
    private static final String mName = "QViz";

    private boolean startCheapCastService = true;

    private Visualizer mVisualizer;
    private AbstractRenderer renderer;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        renderer = new SimpleRenderer(this);

        mVisualizer = new Visualizer(0);
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {

            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {}

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                renderer.onFft(bytes, samplingRate);
            }
        }, Visualizer.getMaxCaptureRate() / 4, true, true);

        renderer.start();
        mVisualizer.setEnabled(true);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        renderer.stop();
        mVisualizer.setEnabled(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if(startCheapCastService) {
            final Intent serviceIntent = new Intent();
            serviceIntent.setClassName("at.maui.cheapcast", "at.maui.cheapcast.service.CheapCastService");
            startService(serviceIntent);
        }

        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

}
