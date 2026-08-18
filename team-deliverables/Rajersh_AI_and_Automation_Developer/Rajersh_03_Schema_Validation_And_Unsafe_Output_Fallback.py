"""Strict validation of AI output against the supported action schema.

Owners: Swati + Rajersh (+ security review Tushar). Implements FR-03 and SRS §10:
the LLM never controls the phone — every intent passes through here first,
and again through the on-device validator (defense in depth).
"""
from __future__ import annotations

from typing import List, Tuple

from .schemas import (
    Intent, MAX_ACTIONS, SENSITIVE_ACTIONS, SUPPORTED_ACTIONS,
    SUPPORTED_CONDITIONS, SUPPORTED_TRIGGERS,
)


class ValidationError(Exception):
    def __init__(self, code: str, message: str, details: list | None = None):
        self.code = code
        self.message = message
        self.details = details or []
        super().__init__(message)


def validate_intent(intent: Intent) -> Tuple[Intent, List[str]]:
    """Returns (sanitized_intent, rejected_action_types). Raises ValidationError
    when nothing executable remains."""
    rejected: List[str] = []

    if intent.trigger.type not in SUPPORTED_TRIGGERS:
        raise ValidationError(
            "UNSUPPORTED_ACTION",
            f"Trigger type '{intent.trigger.type}' is not supported.",
        )

    intent.conditions = [c for c in intent.conditions if c.type in SUPPORTED_CONDITIONS]

    kept = []
    for action in intent.actions:
        if action.type in SUPPORTED_ACTIONS:
            kept.append(action)
        else:
            rejected.append(action.type)
    if not kept:
        raise ValidationError(
            "UNSUPPORTED_ACTION",
            "No supported actions remain after validation.",
            details=rejected,
        )
    intent.actions = kept[:MAX_ACTIONS]

    # Sensitive actions ALWAYS require confirmation, whatever the model said.
    if any(a.type in SENSITIVE_ACTIONS for a in intent.actions):
        intent.requires_confirmation = True

    # Clamp confidence; low confidence downgrades to suggestion-only (confirmation).
    intent.confidence = max(0.0, min(1.0, intent.confidence))
    if intent.confidence < 0.4:
        intent.requires_confirmation = True

    # Recompute required permissions from the catalog (never trust the model's list).
    perms = set()
    trigger_perm = {
        "location_enter": "location", "location_exit": "location",
        "calendar_event": "calendar",
        "bluetooth_connect": "connectivity", "bluetooth_disconnect": "connectivity",
    }.get(intent.trigger.type)
    if trigger_perm:
        perms.add(trigger_perm)
    for a in intent.actions:
        p = SUPPORTED_ACTIONS[a.type][1]
        if p:
            perms.add(p)
    intent.required_permissions = sorted(perms)

    return intent, rejected
