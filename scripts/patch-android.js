// Adds the background location tracker to the generated Android project.
// Run after "npx cap add android".
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const javaSrc = path.join(root, "native", "java", "com", "salepoint", "app");
const javaDst = path.join(root, "android", "app", "src", "main", "java", "com", "salepoint", "app");
const manifestPath = path.join(root, "android", "app", "src", "main", "AndroidManifest.xml");

if (!fs.existsSync(manifestPath)) {
  throw new Error("android project not found - run 'npx cap add android' first");
}

// 1) Java files (this also replaces MainActivity so that the Tracker plugin is registered)
fs.mkdirSync(javaDst, { recursive: true });
for (const f of fs.readdirSync(javaSrc)) {
  fs.copyFileSync(path.join(javaSrc, f), path.join(javaDst, f));
  console.log("copied", f);
}

// 2) AndroidManifest.xml
let xml = fs.readFileSync(manifestPath, "utf8");

const permissions = [
  '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
  '<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
  '<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />',
  '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
  '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />',
  '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />',
  '<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />',
  '<uses-permission android:name="android.permission.USE_EXACT_ALARM" />',
  '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />',
  '<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />',
  '<uses-permission android:name="android.permission.WAKE_LOCK" />'
];
const missing = permissions.filter((p) => {
  const name = p.match(/android:name="([^"]+)"/)[1];
  return !xml.includes('android:name="' + name + '"');
});
if (missing.length) {
  xml = xml.replace("</manifest>", missing.map((p) => "    " + p).join("\n") + "\n</manifest>");
}

if (!xml.includes(".TrackerService")) {
  const components = `
        <service
            android:name=".TrackerService"
            android:exported="false"
            android:foregroundServiceType="location" />

        <receiver android:name=".AlarmReceiver" android:exported="false" />

        <receiver android:name=".BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>`;
  xml = xml.replace("</application>", components);
}

fs.writeFileSync(manifestPath, xml);

// 3) check
const check = fs.readFileSync(manifestPath, "utf8");
for (const need of ["TrackerService", "AlarmReceiver", "BootReceiver", "ACCESS_BACKGROUND_LOCATION", "FOREGROUND_SERVICE_LOCATION"]) {
  if (!check.includes(need)) throw new Error("manifest patch failed: " + need);
}
console.log("Android manifest patched");
