package com.salepoint.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Foreground service: records a location point every minute between 6 AM and 8 PM, even when the app is closed. */
public class TrackerService extends Service {
    static final String CHANNEL = "sale_point_tracker";
    static final String ACTION_FIX = "com.salepoint.app.FIX";

    private LocationManager lm;
    private boolean started = false;
    private long lastSaved = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final LocationListener listener = new LocationListener() {
        @Override public void onLocationChanged(Location l) { record(l); }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) { }
    };

    private final Runnable stopCheck = new Runnable() {
        @Override public void run() {
            if (!Sched.inHours(System.currentTimeMillis())) {
                Sched.scheduleNext(TrackerService.this);
                stopSelf();
            } else {
                handler.postDelayed(this, 60000L);
            }
        }
    };

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(1, n);
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Sched.scheduleNext(this);
        if (!Sched.inHours(System.currentTimeMillis()) || !Sched.hasLocation(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startUpdates();
        if (intent != null && ACTION_FIX.equals(intent.getAction())) singleFix();
        handler.removeCallbacks(stopCheck);
        handler.postDelayed(stopCheck, 60000L);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(stopCheck);
        try { if (lm != null) lm.removeUpdates(listener); } catch (Exception e) { }
        started = false;
        super.onDestroy();
    }

    private void startUpdates() {
        if (started) return;
        try {
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            String[] providers = { LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER };
            for (String p : providers) {
                if (lm.isProviderEnabled(p)) lm.requestLocationUpdates(p, 60000L, 0f, listener, Looper.getMainLooper());
            }
            started = true;
        } catch (SecurityException e) {
            stopSelf();
        } catch (Exception e) {
            // try again on the next start
        }
    }

    @SuppressWarnings("deprecation")
    private void singleFix() {
        try {
            if (lm == null) lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            String[] providers = { LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER };
            for (String p : providers) {
                if (lm.isProviderEnabled(p)) lm.requestSingleUpdate(p, listener, Looper.getMainLooper());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void record(Location l) {
        long now = System.currentTimeMillis();
        if (l == null || !Sched.inHours(now)) return;
        if (now - lastSaved < 20000L) return;
        lastSaved = now;
        TrackStore.append(this, now, l.getLatitude(), l.getLongitude(), l.getAccuracy());
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Location tracking", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("Sale Point")
         .setContentText("Tracking location (6 AM to 8 PM)")
         .setSmallIcon(android.R.drawable.ic_menu_mylocation)
         .setOngoing(true);
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (open != null) {
            int fl = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) fl |= PendingIntent.FLAG_IMMUTABLE;
            b.setContentIntent(PendingIntent.getActivity(this, 0, open, fl));
        }
        return b.build();
    }
}
