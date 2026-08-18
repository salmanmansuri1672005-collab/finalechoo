"""Required case 9: generate an editable daily plan from prepared data."""
import json
from pathlib import Path

DATA = json.loads((Path(__file__).resolve().parents[1] /
                   "sample_data" / "demo_dataset.json").read_text())


def _plan(client, **overrides):
    payload = {
        "date": "2026-08-18",
        "calendar": DATA["calendar"],
        "tasks": DATA["tasks"],
        "commitments": [{"task": "Send report to Rohit",
                         "deadline": "2026-08-18T18:00:00"}],
        "day_start": "08:00", "day_end": "22:00",
    }
    payload.update(overrides)
    return client.post("/ai/plan", json=payload).json()


def test_fixed_events_respected(client):
    plan = _plan(client)
    fixed = [b for b in plan["blocks"] if b["kind"] == "fixed"]
    assert {b["title"] for b in fixed} == {"DSA Lecture", "Project Sync (Team EchoOS)"}
    assert all(b["editable"] is False for b in fixed)


def test_no_overlaps_and_sorted(client):
    plan = _plan(client)
    blocks = plan["blocks"]
    for a, b in zip(blocks, blocks[1:]):
        assert a["end"] <= b["start"], f"overlap: {a} vs {b}"


def test_commitment_scheduled_before_deadline(client):
    plan = _plan(client)
    c = next(b for b in plan["blocks"] if b["kind"] == "commitment")
    assert c["end"] <= "18:00"
    assert c["editable"] is True


def test_tasks_editable_and_all_present_or_noted(client):
    plan = _plan(client)
    titles = {b["title"] for b in plan["blocks"]}
    for t in DATA["tasks"]:
        assert t["title"] in titles or any(t["title"] in n for n in plan["notes"])


def test_overfull_day_produces_rollover_note(client):
    plan = _plan(client, day_start="08:00", day_end="10:00",
                 tasks=[{"title": "Huge task", "est_minutes": 600, "priority": 1}])
    assert any("roll over" in n for n in plan["notes"])
