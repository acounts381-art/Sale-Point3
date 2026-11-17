# Sale Point – Android APK (GitHub Actions)

Is project me Sale Point app ki web file (`www/index.html`) hai. GitHub apne aap isse Android APK bana deta hai.

## APK banane ke steps

1. GitHub par naya repository banao (jese `sale-point`).
2. Is folder ki **saari files** (hidden folder `.github` samet) repository me daalo. Terminal se:

   ```
   git init
   git add .
   git commit -m "Sale Point app"
   git branch -M main
   git remote add origin https://github.com/YOUR-NAME/sale-point.git
   git push -u origin main
   ```

3. GitHub me repository ka **Actions** tab kholo. **Build Android APK** workflow push par khud chal jata hai
   (ya **Run workflow** dabao).
4. Build khatam hone par (5 se 10 minute) run ka page kholo, neeche **Artifacts** me `sale-point-apk` download karo.
5. Zip kholo, andar `app-debug.apk` hai. Isse phone me copy karke install karo
   (phone me "Install unknown apps" allow karna hoga).

## Zaroori baatein

- Data (products, retailers, sales, ledger) phone ke andar save rehta hai. App uninstall karne ya phone ka app data clear karne par ye khatam ho jata hai.
- Excel/CSV export file banake share menu kholta hai, wahan se Drive, WhatsApp ya Files me save karo.
- App badalni ho to `www/index.html` edit karke dobara push karo, naya APK ban jayega.
- Ye debug APK hai (testing aur khud ke istemal ke liye theek). Play Store ke liye signed release build alag se banani hogi.
- App ka naam `capacitor.config.json` me `appName`, aur package naam `appId` me hai.

## Background location tracking (app band ho tab bhi)

APK me ek native Android service hai jo subah 6 baje se raat 8 baje tak har 1 minute baad location point save karti hai.
Ye app khuli ho ya band, phone lock ho ya na ho, chalti rehti hai. 6 baje khud start hoti hai (alarm se), aur phone restart hone par bhi.

Pehli bar app kholkar **Report -> Total Area Covered** (3 bar Report heading par tap karke dikhao) me ye 4 cheezein allow karo:

1. **Location: Allow**
2. **Allow all the time** (phone ki settings khulengi, wahan "Allow all the time" chuno). Ye sab se zaroori hai, iske baghair app band hone par tracking nahi chalegi.
3. **Tracking notification: Allow** (Android 13 aur upar). Tracking chalte waqt upar ek chhoti notification dikhti hai, ye Android ka niyam hai.
4. **No battery restriction: Set** (battery saver band karo).

Sab ho jane par card me "Background tracking is ON" likha aata hai.

Dhyan rakhne ki baatein:

- Kuch phones (Xiaomi, Oppo, Vivo, Realme, Huawei) apni battery safai se background apps band kar dete hain. Un phones me app ko "Autostart" allow karo aur battery me "No restrictions" karo.
- Agar app ko Settings se "Force stop" karo to Android uska 6 baje ka alarm bhi hata deta hai. App ek bar dobara kholni hogi.
- Raat 8 baje se subah 6 baje tak koi location nahi li jati.
- Location ke points phone ke andar save hote hain aur app kholne par (ya Update dabane par) Total Area Covered me aa jate hain.
- Is native hisse ka code `native/java/com/salepoint/app/` me hai, aur build ke waqt `scripts/patch-android.js` use Android project me jod deta hai.
