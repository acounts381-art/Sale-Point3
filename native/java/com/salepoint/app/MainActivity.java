package com.salepoint.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TrackerPlugin.class);
        super.onCreate(savedInstanceState);
        // Tracking keeps its 6 AM alarm even if the user never opens the screen again.
        Sched.scheduleNext(this);
    }
}
