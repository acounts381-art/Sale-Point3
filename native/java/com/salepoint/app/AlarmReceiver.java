package com.salepoint.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at 6 AM, and every 15 minutes during working time to make sure tracking is still running. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Sched.scheduleNext(context);
        Sched.startServiceIfNeeded(context, null);
    }
}
