"""Required cases 6 & 7: classify notifications; extract commitment fields."""
import json
from pathlib import Path

DATA = json.loads((Path(__file__).resolve().parents[1] /
                   "sample_data" / "demo_dataset.json").read_text())


def _run(client):
    return client.post("/ai/commitment", json={
        "items": DATA["notifications"], "today": "2026-08-17"}).json()


def test_extracts_report_commitment_with_deadline(client):
    body = _run(client)
    by_item = {c["item_id"]: c for c in body["commitments"]}
    assert "n1" in by_item
    c = by_item["n1"]
    assert "report" in c["task"]
    assert c["person"] == "Rohit"
    assert c["deadline"] is not None and c["deadline"].startswith("2026-08-18")
    assert c["deadline"].endswith("18:00")
    assert 0.5 <= c["confidence"] <= 1.0
    assert c["raw_excerpt"]


def test_extracts_bill_and_assignment(client):
    body = _run(client)
    ids = {c["item_id"] for c in body["commitments"]}
    assert {"n2", "n3", "n6"} <= ids


def test_classification_buckets(client):
    body = _run(client)
    pr = {c["item_id"]: c["priority"] for c in body["classified"]}
    assert pr["n1"] == "needs_attention"     # commitment
    assert pr["n5"] == "needs_attention"     # OTP
    assert pr["n4"] == "low"                 # sale spam
    assert pr["n7"] == "low"                 # youtube
    assert len(pr) == len(DATA["notifications"])  # nothing silently dropped


def test_no_commitment_in_promo(client):
    body = _run(client)
    assert all(c["item_id"] != "n4" for c in body["commitments"])
