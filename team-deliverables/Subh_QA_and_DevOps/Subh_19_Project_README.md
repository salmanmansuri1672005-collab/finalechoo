# EchoOS — AI-Powered Personal Automation Layer for Android

> **"Your phone shouldn't just wait for your instructions. It should learn how to help."**
>
> **ECHOOS — Understand. Decide. Automate. Learn.**

EchoOS is an intelligent automation and context layer above Android. It senses user-approved
context (time, location, calendar, notifications, connectivity), understands natural-language
intent, detects commitments, recommends actions, and executes only supported, permitted
automations — always keeping the user in control.

Built per **SRS v2.0 (Detailed MVP + Development Work Plan)**.

## Repository Layout

```
echoos/
├── android/          # Native Android app — Kotlin + Jetpack Compose + Room (Salman, Tushar, Rajersh)
├── backend/          # FastAPI AI orchestration backend (Swati, Rajersh)
├── demo/             # Installable single-file web app implementing the full MVP + 3-min demo flow
├── docs/             # API contracts, AI action schema, git workflow, test plan, demo script
└── README.md
```

## Architecture

```
USER
 ↓
ANDROID APP (Kotlin + Jetpack Compose)
 ├── Context Collector      (location/geofencing, time, calendar, notifications, connectivity)
 ├── Automation Engine      (TriggerManager → Validator → PermissionChecker → ActionExecutor)
 ├── Local Data Store       (Room: Automations, Commitments, Preferences, Execution History)
 └── UI / Activity / Permission Center
 ↓
FASTAPI AI BACKEND (/ai/parse, /ai/commitment, /ai/pattern, /ai/plan, /health, /config)
 ↓
LLM SERVICE (optional — rule-based fallback built in)
```

**Safety invariant:** the LLM never controls the phone. It emits structured intent only.
The app validates that intent against a strict action schema, checks permissions and the
autonomy level (Suggest / Confirm / Approved-Automatic), asks for confirmation when
required, and only then executes a supported action. Every step is logged to Activity History.

## Quick Start

### Backend (Python 3.10+)

```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
# tests
pytest -q
```

Optional LLM mode: set `ECHOOS_LLM_API_KEY` (and `ECHOOS_LLM_BASE_URL`, `ECHOOS_LLM_MODEL`).
Without a key, the deterministic rule-based engine handles all four AI functions — the full
demo works offline with zero API cost.

### Android app

Open `android/` in Android Studio (Hedgehog+), let Gradle sync, then Run.
Min SDK 26, target SDK 34. The app points at `http://10.0.2.2:8000` (emulator → local backend)
by default; change `BuildConfig` field `BACKEND_URL` in `app/build.gradle.kts` for a device.
When the backend is unreachable, the on-device fallback parser keeps core flows working
(offline-first requirement, SRS §13.1).

### Instant demo app

Open `demo/echoos_app.html` in any browser (or "Add to Home Screen" on Android for an
app-like install). It implements every MVP feature — NL automation with preview, context
modes, EchoLens, notification intelligence, pattern suggestions, AI day planner, activity
history, permission & autonomy center — with the prepared demo dataset and simulated
context events, clearly labeled as simulation per SRS §5.

## Team Ownership (SRS §19)

| Member  | Role               | Owns                                                        |
|---------|--------------------|-------------------------------------------------------------|
| Salman  | Frontend           | Compose UI: all screens, navigation, states                  |
| Swati   | Backend            | FastAPI structure, contracts, orchestration, diagnostics     |
| Tushar  | Data + Security    | Room models, repositories, permissions, autonomy enforcement |
| Rajersh | AI + Automation    | Parser, EchoLens, patterns, planner, action schema, prompts  |
| Subh    | QA + DevOps        | Test suite, integration, build, deployment, demo rehearsal   |

## Documentation

- `docs/API_CONTRACTS.md` — request/response contracts for every endpoint
- `docs/AI_ACTION_SCHEMA.md` — the strict action schema + supported action catalog
- `docs/GIT_WORKFLOW.md` — branch policy and review rules (SRS §21)
- `docs/TEST_PLAN.md` — test levels, required cases, acceptance checklist (SRS §18, §25)
- `docs/DEMO_SCRIPT.md` — the three-minute hackathon demo runbook (SRS §26)

## MVP Acceptance Criteria (SRS §25.1)

1. A judge can create an automation with one natural-language sentence. ✔
2. At least three context triggers work reliably in the demo (location, time, Bluetooth/driving). ✔
3. EchoLens detects commitments from the prepared notification dataset. ✔
4. Every suggestion and execution shows a clear explanation. ✔
5. At least one repeated pattern produces a useful automation suggestion. ✔
6. The daily planner creates an editable plan from prepared data. ✔
7. Permission and autonomy controls visibly affect execution. ✔
8. The complete demo runs in ~three minutes. ✔
