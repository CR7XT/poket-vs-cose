# How to Get an Installable PocketCode APK

A `.zip` exported from AI Studio contains **uncompiled source code**, so renaming `.zip` to `.apk` cannot install on Android. Use either option below to produce a real signed `.apk`:

## Option 1 — Directly in AI Studio (No PC Required)
1. In the **AI Studio** window (not inside the PocketCode app itself), open the **Settings / More** menu in the top bar.
2. Select **Generate APK / AAB** (or **Download APK**).
3. Download the built `.apk` file onto your Android phone, tap it, and allow **Install unknown apps** when prompted.

## Option 2 — Convert Exported ZIP to APK Locally
1. Export and unzip the project folder on your computer.
2. Run the included builder script:
   - **Windows**: Double-click `build-local-apk.bat`
   - **macOS / Linux**: Run `bash build-local-apk.sh`
   *(The script automatically decodes `debug.keystore.base64` into `debug.keystore` and runs `:app:assembleDebug`.)*
3. Or open the unzipped folder in **Android Studio** (after running the script once or decoding `debug.keystore.base64` to `debug.keystore`) and click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Your installable APK is written to:
   `app/build/outputs/apk/debug/app-debug.apk`
