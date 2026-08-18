# EchoOS AI Action Schema (v1)

Owner: Rajersh (schema) · Swati (contract enforcement) · Tushar (security validation)

The LLM/AI layer may only emit documents conforming to this schema. Anything else is
rejected by the validator before it reaches the automation engine (SRS §10, FR-03).

## Structured Intent

```json
{
  "schema_version": 1,
  "trigger":   { "type": "<trigger_type>", "...": "type-specific fields" },
  "conditions": [ { "type": "<condition_type>", "...": "..." } ],
  "actions":   [ { "type": "<action_type>", "...": "..." } ],
  "duration_minutes": 60,
  "required_permissions": ["location", "notifications"],
  "requires_confirmation": true,
  "confidence": 0.92,
  "summary": "When you arrive at College, enable Focus mode and show today's calendar."
}
```

## Trigger Types

| type              | fields                              | permission     |
|-------------------|-------------------------------------|----------------|
| `location_enter`  | `place` (college/work/home/gym)     | location       |
| `location_exit`   | `place`                             | location       |
| `time_schedule`   | `time` "HH:MM", `days` [mon..sun]   | —              |
| `calendar_event`  | `match` (title contains), `offset_minutes` | calendar |
| `bluetooth_connect` | `device` (e.g. "car")             | connectivity   |
| `bluetooth_disconnect` | `device`                       | connectivity   |
| `pattern_detected` | `pattern_id`                       | —              |
| `manual`          | —                                   | —              |

## Condition Types

| type          | fields                    |
|---------------|---------------------------|
| `time_between`| `start` "HH:MM", `end`    |
| `day_of_week` | `days` [mon..sun]         |
| `mode_active` | `mode`                    |

## Action Types (supported action catalog)

| type                | fields                       | sensitive | permission      |
|---------------------|------------------------------|-----------|-----------------|
| `focus_mode`        | `value` (college/work/…)     | no        | dnd             |
| `dnd_on` / `dnd_off`| —                            | no        | dnd             |
| `silent_mode`       | `enabled` bool               | no        | dnd             |
| `wifi_toggle`       | `enabled` bool *(simulated)* | no        | connectivity    |
| `bluetooth_toggle`  | `enabled` bool *(simulated)* | no        | connectivity    |
| `open_app`          | `package` or `app_name`      | no        | —               |
| `play_music`        | `playlist?`                  | no        | —               |
| `navigation_start`  | `destination`                | no        | location        |
| `send_message`      | `to`, `text`                 | **yes**   | messaging       |
| `create_reminder`   | `title`, `time?`             | no        | —               |
| `calendar_summary`  | `event_time?`                | no        | calendar        |
| `notify_user`       | `title`, `text`              | no        | notifications   |
| `set_alarm`         | `time`                       | no        | —               |
| `brightness`        | `level` 0–100 *(simulated)*  | no        | settings        |

Actions marked *(simulated)* cannot be executed live on modern Android without special
privileges; the executor performs a clearly-labeled simulated execution in demo mode (SRS §5, §15).

## Validation Rules (enforced in backend `validation.py` AND on-device `IntentValidator.kt`)

1. `trigger.type` and every `actions[].type` MUST be in the catalogs above → else **reject**.
2. Unknown fields are stripped; missing required fields → **reject** with reason.
3. `send_message` (and any sensitive action) forces `requires_confirmation = true`
   regardless of what the LLM emitted.
4. Financial, password, security-bypass or arbitrary-shell actions do not exist in the
   catalog and therefore can never be executed (SRS non-goals §4.3).
5. `confidence` clamped to [0,1]; below 0.4 → downgrade to suggestion-only.
6. Max 5 actions per automation in MVP.

## Commitment Extraction Output

```json
{ "task": "send the report", "person": "Rohit", "deadline": "2026-08-19T18:00:00",
  "confidence": 0.85, "source": "whatsapp", "raw_excerpt": "I'll send the report by Tuesday 6pm" }
```

## Pattern Output

```json
{ "sequence": ["bluetooth_connect:car", "open_app:maps", "play_music"],
  "frequency": 5, "window_days": 7, "confidence": 0.8,
  "suggestion_summary": "When your car connects, start navigation and play music?" }
```

## Planner Output

```json
{ "date": "2026-08-18",
  "blocks": [ { "start": "09:00", "end": "10:00", "title": "DSA Lecture",
                "kind": "fixed|task|commitment|break", "source": "calendar",
                "editable": false } ],
  "notes": ["Report to Rohit due 18:00 — scheduled at 16:30."] }
```
