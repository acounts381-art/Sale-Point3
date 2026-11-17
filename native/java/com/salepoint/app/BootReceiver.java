package com.salepoint.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After the phone restarts, set the 6 AM alarm again and resume tracking if it is working time. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Sched.scheduleNext(context);
        Sched.startServiceIfNeeded(context, null);
    }
}
