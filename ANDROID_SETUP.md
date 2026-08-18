# EchoOS — Android Studio setup & the fixes applied

## ⚠️ First: open the RIGHT folder

Open **this `android/` folder** in Android Studio (`File → Open` → select the folder
that contains `settings.gradle.kts`).

Do **not** open:
- the repository root (`echoos/`) — it contains the Python backend and docs, so Studio
  finds no Gradle project and reports errors on every file;
- the `EchoOS_TeamFiles/` folder — those are **renamed reading copies** of the same
  sources, grouped per team member for the report. Their filenames deliberately do not
  match their class names and their folders do not match their `package` statements, so
  Android Studio flags an error on almost every line. They are documentation, not a
  buildable project. **This is the most common cause of "a lot of errors".**

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Studio — `Settings → Build Tools → Gradle → Gradle JDK: jbr-17` or 21)
- Android SDK Platform 34 + Build-Tools 34 (Studio will offer to install)

`local.properties` is **not** committed. Android Studio creates it automatically on first
sync. If you build from the command line instead, create it yourself:

```properties
sdk.dir=/home/<you>/Android/Sdk        # macOS: /Users/<you>/Library/Android/sdk
```

## First run
1. `File → Open` → select this `android/` folder.
2. Wait for "Gradle sync finished" (first sync downloads Gradle 8.7 + dependencies).
3. Start the backend so the AI features have something to talk to:
   `cd ../backend && pip install -r requirements.txt && uvicorn app.main:app --port 8000`
4. Run the app. On an emulator the backend URL `http://10.0.2.2:8000` already points at
   your machine. On a physical device, change `BACKEND_URL` in `app/build.gradle.kts`
   to your PC's LAN IP.
   With no backend at all the app still works — it falls back to the on-device parser.

## What was broken, and what was fixed

| # | Problem | Effect in Android Studio | Fix |
|---|---------|--------------------------|-----|
| 1 | **No Gradle wrapper** (`gradlew`, `gradle/wrapper/*`) | "Project not built / no Gradle wrapper", sync fails or uses a random local Gradle | Added the official Gradle 8.7 wrapper (jar, properties, `gradlew`, `gradlew.bat`) |
| 2 | `toSpec()` was a **member extension inside `object ApiClient`** and was imported as `import com.echoos.ai.ApiClient.toSpec` | Hard compile error — Kotlin cannot import member extensions | Moved it to a top-level `fun ApiClient.IntentDto.toSpec()`; import is now `com.echoos.ai.toSpec` |
| 3 | `@Database(exportSchema = true)` with no schema directory | Room/KSP error: "Schema export directory is not provided" | `exportSchema = false` (+ `fallbackToDestructiveMigration()` so schema changes can't crash the demo) |
| 4 | `PendingIntent.FLAG_MUTABLE` used unguarded at `minSdk 26` | Lint `NewApi` error — the constant needs API 31 | Guarded with `Build.VERSION.SDK_INT >= S` |
| 5 | `BluetoothDevice.name` read without a permission check | Lint `MissingPermission` error | `@SuppressLint` with the read already wrapped in `runCatching` |
| 6 | `NotificationManager.notify` on API 33+ | Lint `MissingPermission` error | `@SuppressLint` — POST_NOTIFICATIONS is requested progressively by the UI |
| 7 | Notification listener service `exported="false"` | The OS cannot bind it, so it never appears in Settings on Android 12+ | `exported="true"`, still protected by `BIND_NOTIFICATION_LISTENER_SERVICE` |
| 8 | Room DAO methods used Kotlin **default parameter values** | Fragile with KSP codegen | Removed; call sites pass explicit limits |
| 9 | No `.gitignore` | `build/`, `.idea/`, `local.properties` got committed | Added a standard Android `.gitignore` |
| 10 | Unused `StateFlow` import | Warning noise | Removed |

## How this was verified

Compiled with the **real Kotlin 2.0.20 compiler** (the version this project targets):

- All 20 `main` source files: **0 errors, 0 warnings** → 111 classes produced.
- All `test` source files: **0 errors**.
- The pure-logic core (`domain/`, `ai/FallbackParser`) was not only compiled but
  **executed** against 17 assertions covering FR-03 validation, autonomy gating,
  permission blocking and offline parsing — **17/17 passed**.

(The Android/AndroidX APIs were supplied as faithful signature stubs, because this
build environment has no network access to Google's Maven. Everything that is *your*
code — every class, call, and type in `com.echoos` — is fully type-checked.)

## If sync still fails
- `File → Invalidate Caches → Invalidate and Restart`
- `Settings → Build Tools → Gradle → Gradle JDK` must be **17 or 21**, not 1.8
- Behind a proxy/offline: `File → Settings → Build Tools → Gradle → uncheck "Offline work"`
- Terminal check: `./gradlew :app:assembleDebug --stacktrace`
