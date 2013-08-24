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

    private float ledMags[] = null;
    private float ledMaxMags[] = null;
    private float ledMinMags[] = null;

    private int rgbs[] = null;

    private int indexOffset = 0;
    private int indexOffsetMod = 1;

    private int hueRange = 360;
    private int hueRangeMod = 0;
    private int hueOffset = 0;

    public SimpleRenderer(Context context) {
        super(context);
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        ledMags = new float[leds];
        ledMaxMags = new float[leds];
        ledMinMags = new float[leds];
        rgbs = new int[leds * 3];
    }

    @Override
    public void start() {
        super.start();
        timer = new Timer();
        timer.scheduleAtFixedRate(drawLEDsTask, 0, 50);
        timer.scheduleAtFixedRate(changeOffsetMods, 0, 10000);
        timer.scheduleAtFixedRate(changeHueMods, 0, 200);
    }

    @Override
    public void stop() {
        super.stop();
        if (timer != null)
            timer.cancel();
    }

    private final TimerTask changeHueMods = new TimerTask() {
        @Override
        public void run() {
            hueOffset = (hueOffset + 1) % 360;

//            hueRange = hueRange + hueRangeMod;
//            if (hueRange <= 60) {
//                hueRangeMod = 1;
//                hueRange = 60;
//            } else if (hueRange >= 360) {
//                hueRangeMod = -1;
//                hueRange = 360;
//            }
        }
    };

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

            final int numFreqs = magnitudePoints.length;
            if (leds > numFreqs) {
                return; // should not happen, or does it with low sampling frequencies?
            }

            final int magPointsPerLed = numFreqs / leds;
            int i=0, led=0;

            while (i< numFreqs) {
                float mag = 0;
//                for(int j=0;j<magPointsPerLed;j++)
//                    mag += magnitudePoints[i + j];
                    mag += magnitudePoints[i];

                ledMags[led] = 0.6f * ledMags[led] + 0.4f * mag;

                if (ledMags[led] >= ledMaxMags[led])
                    ledMaxMags[led] = ledMags[led];
                else ledMaxMags[led] = 0.7f * ledMaxMags[led] + 0.3f * ledMags[led];

                if (ledMags[led] <= ledMinMags[led])
                    ledMinMags[led] = ledMags[led];
                else ledMinMags[led] = 0.9f * ledMinMags[led] + 0.1f * ledMags[led];

                // make sure the delta is > 0
                if(ledMaxMags[led]==ledMinMags[led])ledMaxMags[led]+=1;

                led++;
                i+=magPointsPerLed;
            }

            for(i=0;i<leds;i++) {
                int c,r,g,b;
                final float h = (hueOffset + (hueRange * (float) i / (float) leds)) % 360;
                final float v = (ledMags[i] - ledMinMags[i]) / (ledMaxMags[i] - ledMinMags[i]);
                c = Color.HSVToColor(new float[]{
                        h,
                        1f,
                        v});

                r = (c >> 16) & 0xff;
                g = (c >>  8) & 0xff;
                b = c & 0xff;
                final int j = (i + indexOffset + leds) % leds;
//                final int j = i;
                rgbs[3 * j] = r;
                rgbs[3 * j + 1] = g;
                rgbs[3 * j + 2] = b;
            }

            try {
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
