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

    /**
     * Contains the magnitude for each frequency
     */
    private float magnitudePoints[] = null;

    private float ledMags[] = null;
    private float ledMaxMags[] = null;
    private float ledMinMags[] = null;

    private int rgbs[] = null;

    private int indexOffset = 0;
    private int indexOffsetMod = 1;
    private int indexOffsetRate = 1;
    private int indexOffsetNext = 0;

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

    /**
     * Rotating the hue means the colors representing specific
     * frequencies are shifted over time.
     */
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

    /**
     * This 10 second interval TimerTask randomly changes
     * the speed and direction of the rotation. The actual
     * rotation is done in the drawLEDsTask TimerTask.
     */
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

    /**
     * This 20Hz TimerTask updates the LEDs. The FFT data is
     * updated only at 10Hz, so this task also "fades down"
     * the magnitudes after calculating the RGB value. This
     * way the next iteration over the "same" data set yields
     * a different result.
     */
    private final TimerTask drawLEDsTask = new TimerTask() {
        @Override
        public void run() {
            if(!isConnected || leds==0 || magnitudePoints == null)
                return;

            for(int i=0;i<leds;i++) {
                int c,r,g,b;
                final float h = (hueOffset + (hueRange * (float) i / (float) leds)) % 360;
                final float mag = ledMaxMags[i] - ledMinMags[i];
                final float v;
                if (mag>0)
                    v = (ledMags[i] - ledMinMags[i]) / mag;
                else {
                    Log.w(TAG, "zero/neg mag!");
                    v = 0;
                }

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

                ledMags[i] *= 0.95f;
            }

            try {
                mLedService.setLedRange(mBinder, 0, leds, rgbs);
            } catch (RemoteException e) {
                Log.e(TAG, "Error setting LEDs", e);
            }

            if (indexOffsetNext-- <= 0) {
                indexOffset += indexOffsetMod;
                if (indexOffset < -leds)
                    indexOffset=0;

                indexOffsetNext = indexOffsetRate;
            }
        }
    };

    /**
     * Does some fancy math to calculate the magnitude for
     * samples of interest only.
     *
     * This method is based on work by Felix Palmer:
     * https://github.com/felixpalmer/android-visualizer
     *
     * @param data byte array of FFT data, see Visualizer.getFft for details
     * @param sampleRate
     */
    public void onFft(byte[] data, int sampleRate) {

        final int n = data.length/2;
        if (magnitudePoints == null || magnitudePoints.length != n)
            magnitudePoints = new float[n];

        final int numFreqs = magnitudePoints.length;

        // not every frequency in the result set is used
        // last time I checked data[] was 1024 meaning 512 frequencies
        // from ??? to ???
        // but there are only n leds, so only every numFreqs frequency
        // is actually used
        final int magPointsPerLed = numFreqs / leds;
        int led=0;

        float magnitude;
        for (int i=0;i<n;i++) {
            if (i % magPointsPerLed != 0)
                continue;

            byte rfk = data[2 * i];
            byte ifk = data[2 * i + 1];

            magnitude = ((rfk * rfk + ifk * ifk));
            int dbValue = (int) (10 * Math.log10(magnitude));
            magnitude = Math.round(dbValue * 8);

            magnitudePoints[led] = 0.8f * magnitudePoints[led] + 0.2f * magnitude;
            ledMags[led] = magnitudePoints[led];

            if (ledMags[led] >= ledMaxMags[led])
                ledMaxMags[led] = ledMags[led];
            else ledMaxMags[led] = 0.7f * ledMaxMags[led] + 0.3f * ledMags[led];

            if (ledMags[led] <= ledMinMags[led])
                ledMinMags[led] = ledMags[led];
            else ledMinMags[led] = 0.9f * ledMinMags[led] + 0.1f * ledMags[led];

            // make sure the delta is > 0
            if(ledMaxMags[led]==ledMinMags[led])ledMaxMags[led]+=1;

            led++;
        }

    }
}
