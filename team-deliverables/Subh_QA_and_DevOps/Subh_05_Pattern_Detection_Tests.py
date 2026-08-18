"""Required case 8: detect repeated pattern and produce a suggestion."""
import json
from pathlib import Path

DATA = json.loads((Path(__file__).resolve().parents[1] /
                   "sample_data" / "demo_dataset.json").read_text())


def test_driving_pattern_detected(client):
    r = client.post("/ai/pattern", json={
        "events": DATA["context_events"], "min_frequency": 3})
    assert r.status_code == 200
    patterns = r.json()["patterns"]
    assert patterns, "expected at least one pattern"
    p = patterns[0]
    assert p["frequency"] >= 4
    assert p["sequence"][0] == "bluetooth_connect:car"
    assert 0 < p["confidence"] <= 1
    assert "Automate" in p["suggestion_summary"]
    intent = p["suggested_intent"]
    assert intent["trigger"]["type"] == "bluetooth_connect"
    assert intent["requires_confirmation"] is True  # never auto-created
    types = {a["type"] for a in intent["actions"]}
    assert {"open_app", "play_music"} <= types


def test_below_min_frequency_no_pattern(client):
    r = client.post("/ai/pattern", json={
        "events": DATA["context_events"][:3], "min_frequency": 3})
    assert r.json()["patterns"] == []
