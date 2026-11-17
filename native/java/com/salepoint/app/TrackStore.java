package com.salepoint.app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;

/** Location points are kept in a small file, one JSON line per point. */
public class TrackStore {
    static final String FILE = "tracks.jsonl";

    static synchronized void append(Context ctx, long t, double lat, double lng, float acc) {
        try {
            FileOutputStream f = ctx.openFileOutput(FILE, Context.MODE_APPEND);
            String line = "{\"t\":" + t + ",\"lat\":" + lat + ",\"lng\":" + lng + ",\"acc\":" + acc + "}\n";
            f.write(line.getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {
            // ignore
        }
    }

    static synchronized JSONArray since(Context ctx, long since) {
        JSONArray out = new JSONArray();
        try {
            File file = ctx.getFileStreamPath(FILE);
            if (!file.exists()) return out;
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JSONObject o = new JSONObject(line);
                    if (o.getLong("t") > since) out.put(o);
                } catch (Exception ignore) {
                    // skip a broken line
                }
            }
            r.close();
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /** Removes points older than the given time so the file does not grow forever. */
    static synchronized void prune(Context ctx, long before) {
        try {
            File file = ctx.getFileStreamPath(FILE);
            if (!file.exists()) return;
            StringBuilder keep = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    if (new JSONObject(line).getLong("t") >= before) keep.append(line).append('\n');
                } catch (Exception ignore) {
                    // drop a broken line
                }
            }
            r.close();
            FileOutputStream f = ctx.openFileOutput(FILE, Context.MODE_PRIVATE);
            f.write(keep.toString().getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
