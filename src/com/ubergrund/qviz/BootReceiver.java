package com.ubergrund.qviz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/23/13
 * Time: 10:14 AM
 */
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, QVizService.class);
        context.startService(serviceIntent);

        context.startService(new Intent("at.maui.cheapcast/.service.CheapCastService"));
    }
}
