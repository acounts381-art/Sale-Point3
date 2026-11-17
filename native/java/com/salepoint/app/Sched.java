package com.salepoint.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;

/** Working hours (6 AM to 8 PM) and the alarm that wakes the tracker. */
public class Sched {
    static final int START_HOUR = 6;
    static final int END_HOUR = 20;

    static boolean inHours(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        int h = c.get(Calendar.HOUR_OF_DAY);
        return h >= START_HOUR && h < END_HOUR;
    }

    static boolean hasLocation(Context ctx) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Next wake up: in 15 minutes while it is working time (a check that the tracker is alive), else tomorrow 6 AM. */
    static long nextTrigger(long now) {
        if (inHours(now)) {
            long soon = now + 15 * 60 * 1000L;
            if (inHours(soon)) return soon;
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, START_HOUR);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    static void scheduleNext(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 100, new Intent(ctx, AlarmReceiver.class), flags);
            long when = nextTrigger(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (Exception e) {
            // never crash because of the alarm
        }
    }

    /** Starts the tracking service if it is working time and location is allowed. */
    static void startServiceIfNeeded(Context ctx, String action) {
        try {
            if (!inHours(System.currentTimeMillis()) || !hasLocation(ctx)) return;
            Intent s = new Intent(ctx, TrackerService.class);
            if (action != null) s.setAction(action);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(s);
            else ctx.startService(s);
        } catch (Exception e) {
            // the system may refuse a background start; the next alarm tries again
        }
    }
}
