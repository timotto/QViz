QViz
====

Nexus Q music visualization service for the LED ring

Using CheapCast (https://play.google.com/store/apps/details?id=at.maui.cheapcast) most of the Q's original functionality is back. However synchronized multiroom audio and the LED music visualization are still missing. Even though I miss multiroom audio the most, I started working on the visualization.

This app will provide a background service that uses the Android Visualizer (http://developer.android.com/reference/android/media/audiofx/Visualizer.html) to grab currently played audio. The Visualizer class provides waveform and FFT data.

I learned how to control the LEDs of the Nexus Q from QRemote (https://github.com/docBliny/qremote). I'll connect the basics here and hope to learn more about music visualization.
