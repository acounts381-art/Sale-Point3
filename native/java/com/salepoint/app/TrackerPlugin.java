package com.salepoint.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONArray;

/** Bridge between the web app and the background tracker. */
@CapacitorPlugin(
    name = "Tracker",
    permissions = {
        @Permission(alias = "location", strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION })
    }
)
public class TrackerPlugin extends Plugin {

    private boolean granted(String permission) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return getContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private JSObject statusObject() {
        JSObject o = new JSObject();
        boolean fine = granted(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean bg = Build.VERSION.SDK_INT < 29 || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        boolean notif = Build.VERSION.SDK_INT < 33 || granted("android.permission.POST_NOTIFICATIONS");
        boolean battery = true;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            battery = pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
        }
        o.put("fine", fine);
        o.put("background", bg);
        o.put("notifications", notif);
        o.put("battery", battery);
        return o;
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(statusObject());
    }

    /** Step 1: normal location permission. */
    @PluginMethod
    public void askLocation(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            call.resolve(statusObject());
        } else {
            requestPermissionForAlias("location", call, "locationResult");
        }
    }

    @PermissionCallback
    private void locationResult(PluginCall call) {
        call.resolve(statusObject());
    }

    /** Step 2: "Allow all the time", needed so tracking continues when the app is closed. */
    @PluginMethod
    public void askBackground(PluginCall call) {
        if (Build.VERSION.SDK_INT < 29) {
            call.resolve(statusObject());
            return;
        }
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            call.reject("Allow location first");
            return;
        }
        Activity a = getActivity();
        if (a != null && Build.VERSION.SDK_INT >= 23) {
            a.requestPermissions(new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION }, 4001);
        }
        call.resolve(statusObject());
    }

    /** Step 3 (Android 13 and above): notification for the "tracking" bar. */
    @PluginMethod
    public void askNotifications(PluginCall call) {
        Activity a = getActivity();
        if (a != null && Build.VERSION.SDK_INT >= 33) {
            a.requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 4002);
        }
        call.resolve(statusObject());
    }

    /** Step 4: do not let the phone's battery saver stop the tracker. */
    @PluginMethod
    public void askBattery(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getContext().getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
            }
        } catch (Exception e) {
            // ignore
        }
        call.resolve(statusObject());
    }

    /** Sets the 6 AM alarm and starts tracking at once when it is working time. */
    @PluginMethod
    public void start(PluginCall call) {
        Sched.scheduleNext(getContext());
        Sched.startServiceIfNeeded(getContext(), null);
        call.resolve(statusObject());
    }

    /** Asks for one location point right now. */
    @PluginMethod
    public void fixNow(PluginCall call) {
        Sched.scheduleNext(getContext());
        Sched.startServiceIfNeeded(getContext(), TrackerService.ACTION_FIX);
        call.resolve();
    }

    /** All points saved by the background tracker after the given time (milliseconds). */
    @PluginMethod
    public void points(PluginCall call) {
        Long since = call.getLong("since", 0L);
        JSONArray src = TrackStore.since(getContext(), since == null ? 0L : since);
        JSArray arr = new JSArray();
        for (int i = 0; i < src.length(); i++) {
            try {
                arr.put(src.getJSONObject(i));
            } catch (Exception e) {
                // skip
            }
        }
        JSObject o = new JSObject();
        o.put("points", arr);
        call.resolve(o);
        // keep about 45 days only
        TrackStore.prune(getContext(), System.currentTimeMillis() - 45L * 24 * 60 * 60 * 1000);
    }
}
