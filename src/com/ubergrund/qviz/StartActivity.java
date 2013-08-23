package com.ubergrund.qviz;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created with IntelliJ IDEA.
 * User: Tim
 * Date: 8/22/13
 * Time: 6:28 PM
 */
public class StartActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, QVizService.class));
        finish();
    }
}