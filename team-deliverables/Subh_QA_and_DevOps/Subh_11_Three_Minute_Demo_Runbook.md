# Three-Minute Hackathon Demo Runbook (SRS §26)

Owner: Subh (rehearsal) · Presenter uses `demo/echoos_app.html` or the Android build with
Demo Mode ON. All simulated actions are labeled "SIMULATED" on screen (SRS §15).

| Time      | Section           | Action                                                                 |
|-----------|-------------------|------------------------------------------------------------------------|
| 0:00–0:20 | Problem           | "We repeat the same phone actions daily; assistants wait to be asked." |
| 0:20–0:50 | Natural language  | Type **"When I reach college, start my focus routine"** → show preview (trigger, actions, permissions, autonomy) → Save. |
| 0:50–1:20 | Execution         | Fire test event **Location: enter College** → College Mode activates, DND on, calendar summary notification → open Activity History, show the *why*. |
| 1:20–1:50 | Pattern detection | Fire 3× **Bluetooth: car connected → open Maps → play music** → EchoOS suggests "Automate your driving routine?" → Accept. |
| 1:50–2:20 | EchoLens          | Load demo notifications → show priority classes + extracted commitment "Send report to Rohit — Tue 6pm (85%)" → Accept. |
| 2:20–2:50 | Daily planner     | Tap **Plan my day** → editable plan from calendar + tasks + the accepted commitment → drag a block to edit. |
| 2:50–3:00 | Close             | "EchoOS — Understand. Decide. Automate. Learn. Your phone finally learns how to help." |

Pre-demo checklist: backend `/health` ok (or offline fallback verified), demo dataset loaded,
autonomy of College Mode set to *Automatic (approved)*, driving routine set to *Confirm*,
airplane-mode rehearsal done once, phone/browser zoom at 125% for judges.
