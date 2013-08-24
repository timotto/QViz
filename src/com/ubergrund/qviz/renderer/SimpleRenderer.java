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

    boolean active = false;

    private int fftUsageOffset = 10;
    private float maxMag = 0;

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

                default:
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
                int currentMaxMag = 0;
                for(int i=0;i<leds;i++) {
                    currentMaxMag = Math.max(currentMaxMag, magnitudePoints[fftUsageOffset + i]);
                }
                if (currentMaxMag>maxMag)
                    maxMag = currentMaxMag;
                else
                    maxMag = 0.5f * maxMag + 0.5f * currentMaxMag;

                if (maxMag<1){
                    if (active) {
                        final int rgbs[] = new int[leds * 3];
                        mLedService.setLedRange(mBinder, 0, leds, rgbs);
                        active = false;
                    }
                    return;
                }
                active = true;

                final int rgbs[] = new int[leds * 3];
                for(int i=0;i<leds;i++) {
                    int c,r,g,b;
                    c = Color.HSVToColor(new float[]{
                            360f * (float)i / (float)leds,
                            1f,
                            magnitudePoints[fftUsageOffset + i] / maxMag});

                    r = (c >> 16) & 0xff;
                    g = (c >>  8) & 0xff;
                    b = c & 0xff;
                    rgbs[3 * ((i + indexOffset + leds) % leds)] = r;
                    rgbs[3 * ((i + indexOffset + leds) % leds) + 1] = g;
                    rgbs[3 * ((i + indexOffset + leds) % leds) + 2] = b;
//                    mLedService.setLed(mBinder, (i + indexOffset + leds) % leds, r, g, b);
                }
                mLedService.setLedRange(mBinder, 0, leds, rgbs);
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
            magnitudePoints[i] = (int) (0.4f * magnitude + 0.6f * magnitudePoints[i]);
        }
    }
}
