# Test Plan & Acceptance Checklist (SRS §18, §25)

Owner: Subh · Contributors: all

## Levels
Unit (backend pytest, Android JUnit) · API/contract · AI schema · Android integration ·
E2E user flows · Permission tests · Offline/error tests · Regression · Demo rehearsal.

## Required cases → where covered

| # | Case (SRS §18.2)                                    | Automated in                          |
|---|------------------------------------------------------|---------------------------------------|
| 1 | Create automation from valid natural language        | backend/tests/test_parse.py           |
| 2 | Reject unsupported AI actions                        | backend/tests/test_validation.py      |
| 3 | Require confirmation for sensitive actions           | backend/tests/test_validation.py + AutonomyEngineTest.kt |
| 4 | Trigger location enter/exit                          | android TriggerManagerTest.kt + demo sim |
| 5 | Trigger scheduled automation                         | android TriggerManagerTest.kt         |
| 6 | Classify sample notifications                        | backend/tests/test_commitment.py      |
| 7 | Extract commitment/person/deadline/confidence        | backend/tests/test_commitment.py      |
| 8 | Detect repeated pattern → suggestion                 | backend/tests/test_pattern.py         |
| 9 | Generate editable daily plan                         | backend/tests/test_plan.py            |
|10 | Disable data source → no longer used                 | android PermissionGateTest.kt + manual |
|11 | History shows success AND failure outcomes           | android ExecutionRepoTest.kt + manual |
|12 | Saved automation/history usable offline              | manual (airplane mode) + Room tests   |
|13 | AI backend failure doesn't corrupt local state       | android ApiClientFallbackTest.kt      |

## Feature Definition of Done (SRS §25.2)
Implemented · integrated (UI/API/data/AI) · happy path tested · ≥1 negative case tested ·
error+loading states · permission implications reviewed · audit behavior · code reviewed ·
no critical regression.

## Release gate
`pytest -q` green · Android unit tests green · demo runbook executed twice without deviation ·
security/privacy review (no hard-coded secrets, deletion controls work) · release build signed.
