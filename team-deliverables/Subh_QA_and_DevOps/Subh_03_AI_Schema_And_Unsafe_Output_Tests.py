"""Required cases 2 & 3: reject unsupported actions; force confirmation for
sensitive actions. Owner: Subh (tests) / Rajersh + Tushar (rules)."""
import pytest

from app.schemas import Action, Intent, Trigger
from app.validation import ValidationError, validate_intent


def _intent(actions, trigger=None, **kw):
    return Intent(trigger=trigger or Trigger(type="manual"), actions=actions, **kw)


def test_unsupported_action_stripped_but_valid_ones_kept():
    intent, rejected = validate_intent(_intent([
        Action(type="focus_mode", value="work"),
        Action(type="transfer_money"),          # must never survive
    ]))
    assert rejected == ["transfer_money"]
    assert [a.type for a in intent.actions] == ["focus_mode"]


def test_all_unsupported_actions_rejected_entirely():
    with pytest.raises(ValidationError) as exc:
        validate_intent(_intent([Action(type="disable_lock_screen")]))
    assert exc.value.code == "UNSUPPORTED_ACTION"


def test_unsupported_trigger_rejected():
    with pytest.raises(ValidationError):
        validate_intent(_intent([Action(type="dnd_on")],
                                trigger=Trigger(type="root_shell")))


def test_sensitive_action_forces_confirmation():
    intent, _ = validate_intent(_intent(
        [Action(type="send_message", to="Rohit", text="hi")],
        requires_confirmation=False, confidence=0.99))
    assert intent.requires_confirmation is True


def test_low_confidence_forces_confirmation():
    intent, _ = validate_intent(_intent(
        [Action(type="dnd_on")], requires_confirmation=False, confidence=0.2))
    assert intent.requires_confirmation is True


def test_permissions_recomputed_from_catalog_not_model():
    intent, _ = validate_intent(_intent(
        [Action(type="calendar_summary")],
        trigger=Trigger(type="location_enter", place="college"),
        required_permissions=["contacts", "microphone"]))  # model lies
    assert intent.required_permissions == ["calendar", "location"]


def test_max_five_actions():
    intent, _ = validate_intent(_intent([Action(type="dnd_on")] * 9))
    assert len(intent.actions) == 5


def test_confidence_clamped():
    intent, _ = validate_intent(_intent([Action(type="dnd_on")], confidence=7.5))
    assert intent.confidence == 1.0
