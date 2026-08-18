# EchoOS — complete project

> **Your phone shouldn't just wait for your instructions. It should learn how to help.**
> **ECHOOS — Understand. Decide. Automate. Learn.**

Everything for EchoOS in one place: the Android app, the AI backend, the docs, the
runnable demo app, and each team member's deliverables.

## ▶ Open in Android Studio

`File → Open` → select **this folder** (the one containing `settings.gradle.kts`) → Sync.

That's it. `team-deliverables/`, `backend/`, `docs/` and `demo/` are **not** part of the
Gradle build, so they can't break the sync. Full setup notes and the list of bugs that
were fixed: **[ANDROID_SETUP.md](ANDROID_SETUP.md)**.

## What's in here

```
EchoOS/
├── app/                  ← the Android app module (Kotlin + Compose + Room)
├── gradle/ gradlew…      ← Gradle 8.7 wrapper (already included)
├── backend/              ← FastAPI AI backend (Python) — 24/24 tests passing
├── docs/                 ← API contracts, AI action schema, test plan, demo runbook
├── demo/EchoOS_App.html  ← the working app — open in any browser, no install
├── team-deliverables/    ← every source file renamed by its SRS §19 work item,
│                           grouped per member (reference copies — see below)
├── README.md
└── ANDROID_SETUP.md
```

## Sign-in & profile

Both the Android app and the demo app open on a **login screen**: name + email, validated,
with a **Continue as guest** option. There is no server, no password and no account — the
profile is created and stored **on the device only**, the same rule EchoOS applies to your
automations, commitments and history.

After signing in, the **You / Profile** page shows your details, when you joined, your
default autonomy level, a live summary of your automations and commitments, and one-tap
controls to edit details, export your data, delete any category of it, wipe everything, or
sign out. Signing out clears the profile but deliberately **keeps your automations**.

## Run it

**Android app** — open this folder in Android Studio and press Run.
Min SDK 26, target 34. On an emulator the backend URL `http://10.0.2.2:8000` already
points at your machine; for a physical device change `BACKEND_URL` in `app/build.gradle.kts`.
With no backend at all the app still works — it falls back to the on-device parser.

**Backend**

```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
pytest -q          # 24 passed
```

Optional LLM mode: set `ECHOOS_LLM_API_KEY`. Without it everything runs rule-based, offline.

**Instant demo** — open `demo/EchoOS_App.html` in a browser, or "Add to Home Screen" on
Android for an app-like install. Press `/` inside it for the command palette.

## team-deliverables/ — read this before opening those files

These are **reference copies** of the same sources, renamed after the work item each one
delivers (SRS §19) and grouped per member, for the report and for review.

They are intentionally *not* part of the build: their filenames don't match their class
names and their folders don't match their `package` statements. Gradle ignores the folder
entirely, so it cannot affect compilation. If you open one of those files in Android Studio
and see red squiggles, that's expected — right-click the `team-deliverables` folder →
**Mark Directory as → Excluded** and they disappear.

**Edit the real sources in `app/src/…` and `backend/app/…`, never the copies.**

| Member  | Role            | Files | Owns |
|---------|-----------------|-------|------|
| Salman  | Frontend        | 11 | Compose UI, screens, navigation, login & profile, states |
| Swati   | Backend         | 5  | FastAPI structure, contracts, orchestration |
| Tushar  | Data + Security | 5  | Room models, repositories, permissions, autonomy |
| Rajersh | AI + Automation | 15 | Parser, EchoLens, patterns, planner, action schema |
| Subh    | QA + DevOps     | 19 | Tests, build, deployment, demo runbook, docs |

## Status

- Android sources: compiled with Kotlin 2.0.20 — **0 errors, 0 warnings** (21 files, 119 classes)
- Core logic executed against 17 assertions — **17/17 passed**
- Backend: **pytest 24/24 passing**
- All 8 SRS §25.1 acceptance criteria demonstrable
