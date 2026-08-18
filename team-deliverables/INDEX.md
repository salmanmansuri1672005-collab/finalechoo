# EchoOS — Team Deliverables (named per SRS §19 work plan)

Reference copies of every project source file, renamed after the work item that produced it.

> ⚠️ **Not a buildable project.** Gradle does not compile this folder. Build from the
> project root instead (`app/` + `settings.gradle.kts`). If Android Studio shows squiggles
> here, right-click this folder → **Mark Directory as → Excluded**.


## Salman — Frontend Developer

Android UI / Jetpack Compose (SRS §19.1)

### `Salman_01_Compose_App_Shell_And_Navigation.kt`
- **Work item:** Project navigation and Compose application shell
- **Real path:** `android/app/src/main/java/com/echoos/MainActivity.kt`
- **What it does:** The front door of the Android app. Sets up the Compose UI, the bottom navigation bar with five tabs (Home, Create, EchoLens, Planner, Control), and routes each tab to its screen. Also checks whether the AI backend is reachable when the app starts.

### `Salman_02_App_Theme.kt`
- **Work item:** Compose theme — light/dark color system
- **Real path:** `android/app/src/main/java/com/echoos/ui/theme/Theme.kt`
- **What it does:** Defines the app's colors for light and dark mode in one place, so every screen looks consistent. Screens just wrap themselves in EchoTheme and get the right palette.

### `Salman_03_Dashboard_And_NL_Automation_Screens.kt`
- **Work item:** Home/dashboard, NL automation creation, preview & edit screens
- **Real path:** `android/app/src/main/java/com/echoos/ui/screens/CoreScreens.kt`
- **What it does:** Two key screens. The Dashboard shows active automations, recent activity, commitments and the demo test-event buttons. The Create screen lets the user type a sentence, sends it to the AI, and shows a full preview (trigger, actions, permissions, confidence, autonomy choice) before anything is saved — nothing exists until the user approves it.

### `Salman_04_EchoLens_Patterns_And_Planner_UI.kt`
- **Work item:** EchoLens UI, pattern suggestions (accept/edit/dismiss), AI daily planner UI
- **Real path:** `android/app/src/main/java/com/echoos/ui/screens/IntelligenceScreens.kt`
- **What it does:** The intelligence screens. EchoLens lists commitment candidates with person, deadline and a confidence bar, each with Accept / Edit / Dismiss. Pattern suggestions appear the same way. The Planner screen asks the backend for tomorrow's plan and renders an editable timeline of blocks.

### `Salman_05_History_And_Permission_Center_UI.kt`
- **Work item:** Activity history UI + permission & autonomy center + loading/error/empty states
- **Real path:** `android/app/src/main/java/com/echoos/ui/screens/SystemScreens.kt`
- **What it does:** The trust screens. Activity History shows every detection, suggestion, execution and block with a timestamp, a color-coded outcome and the reason why. The Permission Center is a list of switches — one per data source — plus the privacy delete buttons.

### `Salman_06_ViewModel_State_Orchestration.kt`
- **Work item:** Screen state, backend calls with offline fallback, demo-mode controls
- **Real path:** `android/app/src/main/java/com/echoos/viewmodel/EchoViewModel.kt`
- **What it does:** The glue between the screens and everything else. Holds the UI state as reactive flows, calls the backend for parsing/EchoLens/patterns/planning, falls back to the on-device parser when offline, and exposes the demo simulator buttons. If the backend dies, screens keep working from local data.

### `Salman_07_Login_And_User_Profile_Screens.kt`
- **Work item:** Login screen + user info/profile screen (local account, SRS §15)
- **Real path:** `android/app/src/main/java/com/echoos/ui/screens/AuthScreens.kt`
- **What it does:** The account screens. Login asks for a name and email, validates both, and creates a profile that never leaves the device — there is no server and no password. 'Continue as guest' skips it entirely. The profile screen shows who you are, when you joined, your default autonomy, a summary of your automations and commitments, and one-tap controls to edit details, delete any category of data, wipe everything, or sign out (signing out keeps your automations — only the profile is cleared).

### `Salman_08_Application_Bootstrap_And_Wiring.kt`
- **Work item:** Application class — wires database, repository, engine, scheduler, simulator
- **Real path:** `android/app/src/main/java/com/echoos/EchoApp.kt`
- **What it does:** Runs once when the app process starts. Creates the single database, repository, automation engine, trigger scheduler and demo simulator that the rest of the app shares, and seeds all permission switches to OFF so the user enables sources progressively — never everything at first launch.

### `Salman_09_App_Resources_Strings.xml`
- **Work item:** App name and tagline resources
- **Real path:** `android/app/src/main/res/values/strings.xml`
- **What it does:** Text resources: the app name and the 'Understand. Decide. Automate. Learn.' tagline.

### `Salman_10_App_Resources_Theme.xml`
- **Work item:** Android platform theme entry
- **Real path:** `android/app/src/main/res/values/themes.xml`
- **What it does:** The minimal Android XML theme the app launches with before Compose takes over.

### `Salman_11_App_Icon_Vector.xml`
- **Work item:** Launcher icon (vector)
- **Real path:** `android/app/src/main/res/drawable/ic_launcher.xml`
- **What it does:** The EchoOS launcher icon drawn as a vector — an indigo tile with a stylized 'e' echo mark.


## Swati — Backend Developer

FastAPI / API orchestration (SRS §19.2)

### `Swati_01_FastAPI_App_And_AI_Endpoints.py`
- **Work item:** FastAPI project structure, health/config + AI orchestration endpoints, error envelopes, logging
- **Real path:** `backend/app/main.py`
- **What it does:** The backend server. Exposes six endpoints: /health and /config for diagnostics, and /ai/parse, /ai/commitment, /ai/pattern, /ai/plan for the four AI functions. Every AI answer passes through the validator before it is returned, all errors come back in one standard envelope, and the backend never touches the device — it only returns structured intent for the app to check again.

### `Swati_02_Request_Response_Contracts.py`
- **Work item:** Request/response contracts (Pydantic) — coordinated with Rajersh & Tushar
- **Real path:** `backend/app/schemas.py`
- **What it does:** The single source of truth for what requests and responses look like, plus the strict catalogs: 8 allowed trigger types and 15 allowed action types (with which ones are sensitive and which permission each needs). If a field or action isn't defined here, it doesn't exist for the system.

### `Swati_03_Local_Dev_Configuration.py`
- **Work item:** Local development configuration; LLM credentials via environment only
- **Real path:** `backend/app/config.py`
- **What it does:** Reads settings from environment variables. If ECHOOS_LLM_API_KEY is set the backend uses a real LLM; if not, it runs fully rule-based. API keys are never hard-coded.

### `Swati_04_API_Contracts_Doc.md`
- **Work item:** Integration contracts for UI↔Backend (with Salman)
- **Real path:** `docs/API_CONTRACTS.md`
- **What it does:** Human-readable documentation of every endpoint: example requests, example responses, error codes, and the rules the team agreed on (e.g. breaking changes need a version bump and a heads-up before merge).

### `Swati_05_Backend_Dependencies.txt`
- **Work item:** Backend integration environment
- **Real path:** `backend/requirements.txt`
- **What it does:** The five Python packages the backend needs: FastAPI, Uvicorn, Pydantic, httpx and pytest.


## Tushar — Data + Security Developer

Room/SQLite, permissions, local state (SRS §19.3)

### `Tushar_01_Room_Entities_7_Core_Models.kt`
- **Work item:** User, Automation, ContextEvent, Commitment, Pattern, Execution, Permission entities
- **Real path:** `android/app/src/main/java/com/echoos/data/entity/Entities.kt`
- **What it does:** The seven database tables from the SRS data model (§12), written as Room entities. Each table stores the minimum needed: automations keep their trigger/actions as validated JSON, executions record the outcome AND the explanation, and every context event is marked simulated or real.

### `Tushar_02_DAO_Layer.kt`
- **Work item:** Repositories/DAO layer over Room
- **Real path:** `android/app/src/main/java/com/echoos/data/dao/Daos.kt`
- **What it does:** The database access layer — one DAO per table with exactly the queries the app needs: observe lists reactively (Flow), fetch active automations, flip statuses, and delete. Delete methods exist for every user-facing store because the SRS requires deletion controls.

### `Tushar_03_Local_Persistence_Database.kt`
- **Work item:** Local persistence & migration strategy (offline-first)
- **Real path:** `android/app/src/main/java/com/echoos/data/EchoDatabase.kt`
- **What it does:** Builds the single Room database ('echoos.db') as a thread-safe singleton. Local-first: saved automations and history stay readable with no network at all.

### `Tushar_04_Repositories_And_Deletion_Controls.kt`
- **Work item:** Repository layer, permission-state storage, data deletion controls, audit consistency
- **Real path:** `android/app/src/main/java/com/echoos/data/repo/Repositories.kt`
- **What it does:** The one class the rest of the app talks to for data. Converts between typed domain objects and stored JSON, logs every execution with its reason, tracks which data sources the user has enabled, and implements the privacy deletes (commitments, patterns, full history).

### `Tushar_05_Autonomy_Rules_And_Permission_Gate.kt`
- **Work item:** Suggest/Confirm/Approved-Automatic rules; execution blocked when permissions missing
- **Real path:** `android/app/src/main/java/com/echoos/domain/AutonomyEngine.kt`
- **What it does:** The security gate every automation passes through when its trigger fires. Order matters: missing data source → BLOCKED (with reason); autonomy Suggest → recommend only; sensitive action → always ask; autonomy Confirm → ask; only Automatic with everything enabled executes. Each decision carries a human-readable explanation for the history log.


## Rajersh — AI + Automation Developer

LLM layer + automation intelligence (SRS §19.4)

### `Rajersh_01_Strict_AI_Action_Schema.md`
- **Work item:** Strict AI action schema + supported action catalog
- **Real path:** `docs/AI_ACTION_SCHEMA.md`
- **What it does:** The rulebook for what AI is allowed to output: the exact JSON shape of a structured intent, every allowed trigger/condition/action with its fields, and the validation rules — sensitive actions always confirm, unknown actions are rejected, permissions are recomputed from the catalog (never trusted from the model).

### `Rajersh_02_NL_Intent_Parser.py`
- **Work item:** Natural-language intent parser — trigger/conditions/actions/duration/permissions
- **Real path:** `backend/app/services/parser.py`
- **What it does:** Turns a plain sentence like 'When I reach college, start my focus routine' into a structured intent. Finds the trigger (place/time/car), the actions (focus, silent, music, navigation, reminder…), duration and conditions, then writes a one-line human summary and a confidence score. Works fully offline — no LLM needed.

### `Rajersh_03_Schema_Validation_And_Unsafe_Output_Fallback.py`
- **Work item:** Validation of AI output; fallback for malformed/unsafe output; confidence handling
- **Real path:** `backend/app/validation.py`
- **What it does:** The safety net between AI and execution. Strips unknown actions, rejects unknown triggers, caps actions at five, clamps confidence to 0..1, forces confirmation for sensitive or low-confidence intents, and recomputes required permissions from the catalog. If nothing safe remains it raises a clear 'unsupported action' error.

### `Rajersh_04_Commitment_And_Deadline_Extraction.py`
- **Work item:** EchoLens: commitment/person/deadline extraction + notification classification
- **Real path:** `backend/app/services/commitments.py`
- **What it does:** EchoLens's brain. Scans permitted messages for promise language ('I'll…', 'can you…', 'don't forget…'), extracts the task, the person and the deadline ('by Tuesday 6pm' → an actual date), and scores confidence. Also sorts notifications into Needs Attention / Later / Low so OTPs surface and sale spam sinks.

### `Rajersh_05_Repeated_Pattern_Analysis.py`
- **Work item:** Pattern analysis + automation suggestion generation
- **Real path:** `backend/app/services/patterns.py`
- **What it does:** Watches the approved context event stream for repeated sequences — e.g. car connects → open Maps → play music, four mornings in a row. When a sequence repeats at least three times it produces a suggestion (never a silent rule) with frequency, confidence and a ready-made intent the user can accept, edit or dismiss.

### `Rajersh_06_AI_Day_Planner_Logic.py`
- **Work item:** Daily planner — fixed events respected, deadlines honored, editable blocks
- **Real path:** `backend/app/services/planner.py`
- **What it does:** Builds tomorrow's schedule. Calendar events are fixed and never moved; commitments with deadlines are placed first (earliest deadline wins); tasks fill the gaps by priority; breaks are inserted after long stretches. Anything that doesn't fit produces an honest 'rolls over to tomorrow' note. Every non-fixed block is editable.

### `Rajersh_07_LLM_Prompts_And_Structured_Output.py`
- **Work item:** Prompts/structured-output rules; LLM integration with rule-based fallback
- **Real path:** `backend/app/services/llm.py`
- **What it does:** Optional LLM mode. The system prompt tells the model to output ONLY strict JSON using the allowed catalogs. Any failure — network, bad JSON, schema violation — silently falls back to the rule-based parser, and even good LLM output still goes through the validator. The LLM can never invent an action the catalog doesn't have.

### `Rajersh_08_OnDevice_Intent_Validator.kt`
- **Work item:** Second line of defense — on-device schema validation
- **Real path:** `android/app/src/main/java/com/echoos/domain/IntentValidator.kt`
- **What it does:** The same validation rules as the backend, re-implemented on the phone. Even if the backend were compromised or buggy, the app independently rejects unknown actions, forces confirmation for sensitive ones, and recomputes permissions before anything can run. Defense in depth.

### `Rajersh_09_Domain_Models_And_Action_Catalog.kt`
- **Work item:** Shared domain models + action catalog (with Tushar)
- **Real path:** `android/app/src/main/java/com/echoos/domain/Models.kt`
- **What it does:** The typed vocabulary the whole Android app shares: autonomy levels, automation states, trigger/action/intent classes, and the on-device copy of the action catalog with each action's sensitivity and required capability.

### `Rajersh_10_Automation_Engine_Trigger_To_Executor.kt`
- **Work item:** Trigger manager → validator → permission checker → action executor → activity log
- **Real path:** `android/app/src/main/java/com/echoos/engine/AutomationEngine.kt`
- **What it does:** The heart of EchoOS on the device. When a context event arrives it finds matching automations, asks the AutonomyEngine what's allowed, then executes / asks / suggests / blocks accordingly — and logs every step with its reason. A failed action is recorded as a failure, never dressed up as success. Simulated actions are labeled SIMULATED.

### `Rajersh_11_Context_Collectors.kt`
- **Work item:** Geofence/Bluetooth/time/notification collectors — event-driven, permission-gated
- **Real path:** `android/app/src/main/java/com/echoos/engine/ContextCollectors.kt`
- **What it does:** The app's senses. A geofence receiver for arrive/leave, a Bluetooth receiver for the car, a WorkManager worker for scheduled times, and a notification listener for EchoLens. All event-driven (no battery-draining polling) and every one checks the user's Permission Center switch before recording anything.

### `Rajersh_12_Trigger_Scheduler.kt`
- **Work item:** Geofence registration + WorkManager time triggers
- **Real path:** `android/app/src/main/java/com/echoos/engine/TriggerScheduler.kt`
- **What it does:** Keeps Android in sync with the user's automations: registers a geofence for every place-based trigger and schedules a WorkManager job for every time-based trigger, re-syncing whenever automations change.

### `Rajersh_13_Offline_Fallback_Parser.kt`
- **Work item:** On-device fallback parser (offline-first, SRS §13.1)
- **Real path:** `android/app/src/main/java/com/echoos/ai/FallbackParser.kt`
- **What it does:** A compact on-device version of the sentence parser. When the backend is unreachable, the user can still create automations — the result is marked as an offline draft and still passes the on-device validator.

### `Rajersh_14_Backend_API_Client.kt`
- **Work item:** Retrofit client + DTOs mirroring backend contracts (with Swati)
- **Real path:** `android/app/src/main/java/com/echoos/ai/ApiClient.kt`
- **What it does:** The app's phone line to the backend: a Retrofit client with data classes that mirror Swati's contracts exactly, short timeouts, and a converter that turns wire responses into the app's typed domain objects.

### `Rajersh_15_Sample_Dataset_For_QA.json`
- **Work item:** Prepared demo dataset — notifications, context events, calendar, tasks
- **Real path:** `backend/sample_data/demo_dataset.json`
- **What it does:** The prepared demo data: 8 realistic notifications (commitments, an OTP, spam), 4 repeated driving sessions for pattern detection, plus the calendar and task list the planner uses. Both the tests and the 3-minute demo run on this.


## Subh — QA + DevOps

Testing, integration, release (SRS §19.5)

### `Subh_01_Test_Strategy_And_Acceptance_Checklist.md`
- **Work item:** Test strategy, required cases mapped to suites, release gate
- **Real path:** `docs/TEST_PLAN.md`
- **What it does:** The QA master document: all test levels, a table mapping each of the 13 required SRS test cases to the exact file that automates it, the Definition of Done, and the release gate the build must pass.

### `Subh_02_API_Tests_Parse.py`
- **Work item:** Create automation from valid natural language (+ health/config, empty-input rejection)
- **Real path:** `backend/tests/test_parse.py`
- **What it does:** Proves the headline feature: a plain sentence becomes a correct structured automation (right trigger, right actions, sane confidence, permissions computed). Also checks health/config and that empty input is politely rejected.

### `Subh_03_AI_Schema_And_Unsafe_Output_Tests.py`
- **Work item:** Reject unsupported actions · force confirmation for sensitive actions
- **Real path:** `backend/tests/test_validation.py`
- **What it does:** The safety tests. A made-up 'transfer_money' action is stripped; an all-unsupported intent is rejected outright; send_message always requires confirmation even if the model said otherwise; a lying permission list gets recomputed; confidence is clamped.

### `Subh_04_Commitment_Extraction_Tests.py`
- **Work item:** Classify sample notifications · extract task/person/deadline/confidence
- **Real path:** `backend/tests/test_commitment.py`
- **What it does:** Runs EchoLens over the demo notifications and asserts the exact expectations: the report commitment is found with Rohit and Tuesday 18:00, the OTP is Needs Attention, the sale spam is Low, and no commitment is invented from promotions.

### `Subh_05_Pattern_Detection_Tests.py`
- **Work item:** Repeated pattern → suggestion; below-threshold produces nothing
- **Real path:** `backend/tests/test_pattern.py`
- **What it does:** Feeds four simulated driving sessions in and asserts a car→maps→music suggestion comes out with sensible confidence — and that too few repetitions produce no suggestion at all.

### `Subh_06_Daily_Planner_Tests.py`
- **Work item:** Editable plan, fixed events respected, no overlaps, deadline + rollover notes
- **Real path:** `backend/tests/test_plan.py`
- **What it does:** Asserts the plan never moves calendar events, never overlaps blocks, schedules the commitment before its deadline, marks the right blocks editable, and admits honestly when a day is too full.

### `Subh_07_Test_Fixtures.py`
- **Work item:** Pytest fixtures / API test client
- **Real path:** `backend/tests/conftest.py`
- **What it does:** Small shared setup: puts the backend on the import path and provides the FastAPI test client every test uses.

### `Subh_08_Android_Validator_Unit_Tests.kt`
- **Work item:** On-device schema validation unit tests
- **Real path:** `android/app/src/test/java/com/echoos/IntentValidatorTest.kt`
- **What it does:** JVM unit tests for the on-device validator — same safety assertions as the backend suite, proving the second line of defense works independently.

### `Subh_09_Android_Autonomy_And_Permission_Tests.kt`
- **Work item:** Autonomy gating + disabled-source blocking unit tests
- **Real path:** `android/app/src/test/java/com/echoos/AutonomyEngineTest.kt`
- **What it does:** Tests the gate: a disabled data source blocks even an Automatic automation (and names the missing source), Confirm asks first, Suggest never executes, and a sensitive action asks for confirmation even on Automatic.

### `Subh_10_Demo_Reliability_Simulator.kt`
- **Work item:** Demo mode / test-event controls for reliable three-minute demo
- **Real path:** `android/app/src/main/java/com/echoos/engine/DemoContextSimulator.kt`
- **What it does:** Fires clearly-labeled SIMULATED context events (enter college, car connects, time trigger) and seeds the driving pattern — so the demo works on stage regardless of Android background restrictions.

### `Subh_11_Three_Minute_Demo_Runbook.md`
- **Work item:** SRS §26 demo script + pre-demo checklist
- **Real path:** `docs/DEMO_SCRIPT.md`
- **What it does:** The stage script: what to say and tap in each 30-second window of the 3-minute demo, plus the pre-demo checklist (backend health, demo data loaded, autonomy levels set, rehearsal done).

### `Subh_12_Git_And_Collaboration_Workflow.md`
- **Work item:** Branch policy, review rules, integration checkpoints
- **Real path:** `docs/GIT_WORKFLOW.md`
- **What it does:** How the five branches flow into main: small feature branches, PR review rules, the requirement to announce contract changes before merge, and who owns integration.

### `Subh_13_Release_Build_Config_Gradle.kts`
- **Work item:** Release build configuration (signing-ready, minify)
- **Real path:** `android/app/build.gradle.kts`
- **What it does:** The app module's build file: SDK versions, the BACKEND_URL build field, a minified release build type, and every dependency (Compose, Room+KSP, WorkManager, location, Retrofit, tests).

### `Subh_14_Project_Build_Setup.kts`
- **Work item:** Root Gradle plugins + repo settings
- **Real path:** `android/build.gradle.kts`
- **What it does:** Pins the toolchain: Android Gradle Plugin 8.5.2, Kotlin 2.0.20, the Compose compiler plugin and KSP, so every teammate builds with identical versions.

### `Subh_15_App_Manifest_Permissions_And_Services.xml`
- **Work item:** Manifest — progressive permissions, receivers, notification listener
- **Real path:** `android/app/src/main/AndroidManifest.xml`
- **What it does:** Declares what the app may ask for (location, notifications, Bluetooth, calendar — requested progressively, never all at launch) and registers the geofence receiver, Bluetooth receiver and notification listener service.

### `Subh_16_Gradle_Settings_And_Repositories.kts`
- **Work item:** Gradle settings — module layout + dependency repositories
- **Real path:** `android/settings.gradle.kts`
- **What it does:** Tells Gradle the project is called EchoOS with one ':app' module, and locks dependency downloads to Google and Maven Central only.

### `Subh_17_Gradle_Properties.properties`
- **Work item:** JVM and AndroidX build flags
- **Real path:** `android/gradle.properties`
- **What it does:** Build machine settings: 3 GB JVM heap for the build, AndroidX enabled, official Kotlin code style.

### `Subh_18_Release_Shrinker_Rules.pro`
- **Work item:** ProGuard/R8 keep rules for release builds
- **Real path:** `android/app/proguard-rules.pro`
- **What it does:** Keeps the Retrofit/Gson model classes from being renamed by the code shrinker in release builds, so JSON parsing keeps working.

### `Subh_19_Project_README.md`
- **Work item:** Final technical documentation
- **Real path:** `README.md`
- **What it does:** The front page of the repository: what EchoOS is, the architecture diagram, how to run the backend and the Android app, team ownership, and the acceptance criteria checklist.
