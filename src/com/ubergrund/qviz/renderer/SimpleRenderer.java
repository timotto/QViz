package com.ubergrund.qviz.renderer;

import android.content.Context;
import android.graphics.Color;
import android.os.RemoteException;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/23/13
 * Time: 9:08 AM
 */
public class SimpleRenderer extends AbstractRenderer {

    private Timer timer = null;
    private int magnitudePoints[] = null;
    private long nextFftPrint = 0;

    private int indexOffset = 0;
    private int indexOffsetMod = 1;

    public SimpleRenderer(Context context) {
        super(context);
    }

    @Override
    public void start() {
        super.start();
        timer = new Timer();
        timer.scheduleAtFixedRate(drawLEDsTask, 0, 50);
        timer.scheduleAtFixedRate(changeOffsetMods, 0, 10000);
    }

    @Override
    public void stop() {
        super.stop();
        if (timer != null)
            timer.cancel();
    }

    private final TimerTask changeOffsetMods = new TimerTask() {
        @Override
        public void run() {
            switch ((int)(Math.random() * 10)) {
                case 0:
                case 1:
                case 2:
                    indexOffsetMod *= -1;
                    break;

                case 3:
                    if (indexOffsetMod==0)
                        indexOffsetMod = -1;
                    else
                        indexOffsetMod = 0;
                    break;

                case 4:
                case 5:
                case 6:
                    if (indexOffsetMod==0)
                        indexOffsetMod=1;
            }
        }
    };

    private final TimerTask drawLEDsTask = new TimerTask() {
        @Override
        public void run() {
            if(!isConnected || leds==0 || magnitudePoints == null)
                return;

            try {
                int maxMag = 0;
                for(int i=0;i<leds;i++) {
                    maxMag = Math.max(maxMag, magnitudePoints[i]);
                }
                if (maxMag<1)return;

                for(int i=0;i<leds;i++) {
                    int c,r,g,b;
                    c = Color.HSVToColor(new float[]{
                            360f * (float)i / (float)leds,
                            1f,
                            magnitudePoints[i] / (float)maxMag});

                    r = (c >> 16) & 0xff;
                    g = (c >>  8) & 0xff;
                    b = c & 0xff;
                    mLedService.setLed(mBinder, (i + indexOffset + leds) % leds, r, g, b);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error setting LEDs", e);

            }
            indexOffset += indexOffsetMod;
            if (indexOffset < -leds)
                indexOffset=0;
        }
    };

    public void onFft(byte[] data, int sampleRate) {

        final int n = data.length/2;
        if (magnitudePoints == null || magnitudePoints.length != n)
            magnitudePoints = new int[n];

        double magnitude;
        for (int i=0;i<n;i++) {
            byte rfk = data[2 * i];
            byte ifk = data[2 * i + 1];

            magnitude = ((rfk * rfk + ifk * ifk));
            int dbValue = (int) (10 * Math.log10(magnitude));
            magnitude = Math.round(dbValue * 8);
            magnitudePoints[i] = (int) magnitude;
        }

//        final long now = System.currentTimeMillis();
//        if (now < nextFftPrint)
//            return;
//
//        nextFftPrint = now + 2000;
//        final StringBuilder sb = new StringBuilder();
//        sb.append(magnitudePoints[0]);
//        for(int i=1;i<leds;i++) {
//            sb.append(",");
//            sb.append(magnitudePoints[i]);
//        }
//        Log.d(TAG, "onFftDataCapture("+data.length+", "+sampleRate+") = " + sb.toString());
    }
}
