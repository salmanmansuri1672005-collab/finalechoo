# EchoOS Backend API Contracts (v1)

Owner: Swati · AI payloads: Rajersh · Consumed by: Salman (UI), Tushar (data)

Base URL: `http://<host>:8000` · All bodies JSON · Errors use the standard error envelope.

## Error envelope

```json
{ "error": { "code": "VALIDATION_FAILED", "message": "human readable", "details": [] } }
```

Codes: `VALIDATION_FAILED`, `UNSUPPORTED_ACTION`, `LLM_UNAVAILABLE`, `BAD_REQUEST`, `INTERNAL`.

## GET /health
→ `{ "status": "ok", "version": "1.0.0", "llm_mode": "rule_based" | "llm", "uptime_s": 123 }`

## GET /config
→ `{ "schema_version": 1, "supported_triggers": [...], "supported_actions": [...],
     "sensitive_actions": [...], "autonomy_levels": ["suggest","confirm","automatic"] }`

## POST /ai/parse — natural language → structured automation (FR-01, FR-02, FR-03)

Request:
```json
{ "text": "When I reach college, start my focus routine",
  "user_context": { "places": ["college","home"], "hour": 9 } }
```
Response 200:
```json
{ "intent": { ...structured intent per AI_ACTION_SCHEMA... },
  "valid": true, "rejected_actions": [], "engine": "rule_based" }
```
Response 422: error envelope `UNSUPPORTED_ACTION` when nothing valid can be produced.

## POST /ai/commitment — EchoLens extraction (FR-07, FR-08)

Request:
```json
{ "items": [ { "id": "n1", "source": "whatsapp", "sender": "Rohit",
               "text": "Can you send the report by Tuesday 6pm?" } ],
  "today": "2026-08-17" }
```
Response:
```json
{ "commitments": [ { "item_id": "n1", "task": "...", "person": "...", "deadline": "...",
                     "confidence": 0.85, "raw_excerpt": "..." } ],
  "classified": [ { "item_id": "n1", "priority": "needs_attention|later|low" } ] }
```

## POST /ai/pattern — pattern interpretation (FR-09)

Request:
```json
{ "events": [ { "type": "bluetooth_connect", "value": "car", "ts": "2026-08-15T08:31:00" },
              { "type": "open_app", "value": "maps", "ts": "2026-08-15T08:32:00" } ],
  "min_frequency": 3 }
```
Response:
```json
{ "patterns": [ { "sequence": [...], "frequency": 5, "confidence": 0.8,
                  "suggestion_summary": "...", "suggested_intent": { ... } } ] }
```

## POST /ai/plan — daily planner (FR-10)

Request:
```json
{ "date": "2026-08-18",
  "calendar": [ { "title": "DSA Lecture", "start": "09:00", "end": "10:00" } ],
  "tasks":    [ { "title": "Revise OS notes", "est_minutes": 60, "priority": 2 } ],
  "commitments": [ { "task": "send report to Rohit", "deadline": "2026-08-18T18:00:00" } ],
  "day_start": "08:00", "day_end": "22:00" }
```
Response: planner output per AI_ACTION_SCHEMA (`blocks`, `notes`), all task blocks `editable: true`.

## Contract rules

1. Backend never executes device actions — it returns intent only (SRS §7, §14).
2. Every response the app consumes is schema-validated on-device again (defense in depth).
3. Breaking changes require a version bump and team notification before merge (SRS §21).
