"""Required case 1: create automation from valid natural language. Owner: Subh."""


def test_health_and_config(client):
    h = client.get("/health").json()
    assert h["status"] == "ok"
    c = client.get("/config").json()
    assert "focus_mode" in c["supported_actions"]
    assert c["autonomy_levels"] == ["suggest", "confirm", "automatic"]


def test_parse_college_focus_routine(client):
    r = client.post("/ai/parse", json={
        "text": "When I reach college, start my focus routine"})
    assert r.status_code == 200
    body = r.json()
    intent = body["intent"]
    assert body["valid"] is True
    assert intent["trigger"]["type"] == "location_enter"
    assert intent["trigger"]["place"] == "college"
    types = [a["type"] for a in intent["actions"]]
    assert "focus_mode" in types
    assert intent["confidence"] >= 0.7
    assert "location" in intent["required_permissions"]
    assert intent["summary"]


def test_parse_time_trigger(client):
    r = client.post("/ai/parse", json={
        "text": "Every day at 10pm put my phone on silent"})
    intent = r.json()["intent"]
    assert intent["trigger"]["type"] == "time_schedule"
    assert intent["trigger"]["time"] == "22:00"
    assert any(a["type"] == "silent_mode" for a in intent["actions"])


def test_parse_driving_context(client):
    r = client.post("/ai/parse", json={
        "text": "When my car connects, start navigation to work and play my drive playlist"})
    intent = r.json()["intent"]
    assert intent["trigger"]["type"] == "bluetooth_connect"
    types = [a["type"] for a in intent["actions"]]
    assert "navigation_start" in types and "play_music" in types


def test_parse_empty_text_rejected(client):
    r = client.post("/ai/parse", json={"text": "   "})
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "BAD_REQUEST"
