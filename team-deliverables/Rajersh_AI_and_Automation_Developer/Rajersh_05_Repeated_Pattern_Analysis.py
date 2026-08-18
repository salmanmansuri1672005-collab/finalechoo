"""Pattern detection: repeated context sequences → automation suggestions.

Owner: Rajersh. Implements FR-09 / SRS §9.2. Observes only the approved events
the app sends; never auto-creates rules — output is a suggestion the user can
accept, edit or dismiss.
"""
from __future__ import annotations

from collections import Counter
from datetime import datetime
from typing import List

from ..schemas import (Action, ContextEvent, Intent, PatternRequest,
                       PatternResult, Trigger)

SESSION_GAP_MINUTES = 15

# Events that can anchor a sequence as its trigger
ANCHOR_TYPES = {"bluetooth_connect", "location_enter", "location_exit", "time_schedule"}

# Follow-up event -> equivalent automation action
EVENT_TO_ACTION = {
    "open_app": lambda v: Action(type="open_app", app_name=v),
    "play_music": lambda v: Action(type="play_music", playlist=v),
    "navigation_start": lambda v: Action(type="navigation_start", destination=v or "work"),
    "dnd_on": lambda v: Action(type="dnd_on"),
    "silent_mode": lambda v: Action(type="silent_mode", enabled=True),
    "focus_mode": lambda v: Action(type="focus_mode", value=v or "focus"),
    "wifi_toggle": lambda v: Action(type="wifi_toggle", enabled=(v != "off")),
}


def _key(e: ContextEvent) -> str:
    return f"{e.type}:{e.value}" if e.value else e.type


def _sessions(events: List[ContextEvent]) -> List[List[ContextEvent]]:
    """Split the event stream into sessions anchored on an anchor event."""
    evs = sorted(events, key=lambda e: e.ts)
    sessions: List[List[ContextEvent]] = []
    current: List[ContextEvent] = []
    last_ts: datetime | None = None
    for e in evs:
        ts = datetime.fromisoformat(e.ts)
        gap = (ts - last_ts).total_seconds() / 60 if last_ts else None
        if e.type in ANCHOR_TYPES and (not current or gap is None or gap > 1):
            if current:
                sessions.append(current)
            current = [e]
        elif current and (gap is None or gap <= SESSION_GAP_MINUTES):
            current.append(e)
        elif current:
            sessions.append(current)
            current = []
        last_ts = ts
    if current:
        sessions.append(current)
    return [s for s in sessions if len(s) >= 2]


def detect(req: PatternRequest) -> List[PatternResult]:
    counter: Counter = Counter()
    for s in _sessions(req.events):
        anchor = _key(s[0])
        follow = tuple(sorted({_key(e) for e in s[1:] if e.type in EVENT_TO_ACTION}))
        if follow:
            counter[(anchor, follow)] += 1

    results: List[PatternResult] = []
    for (anchor, follow), freq in counter.most_common():
        if freq < req.min_frequency:
            continue
        confidence = round(min(0.95, 0.5 + 0.1 * freq), 2)
        a_type, _, a_val = anchor.partition(":")

        trigger = Trigger(type=a_type)
        if a_type in ("location_enter", "location_exit"):
            trigger.place = a_val or None
        elif a_type.startswith("bluetooth"):
            trigger.device = a_val or "car"

        actions = []
        for f in follow:
            f_type, _, f_val = f.partition(":")
            actions.append(EVENT_TO_ACTION[f_type](f_val or None))

        human_anchor = {
            "bluetooth_connect": f"your {trigger.device or 'car'} connects",
            "location_enter": f"you arrive at {trigger.place or 'a place'}",
            "location_exit": f"you leave {trigger.place or 'a place'}",
            "time_schedule": "this time of day",
        }.get(a_type, a_type)
        human_actions = ", ".join(a.type.replace("_", " ") +
                                  (f" ({a.app_name or a.playlist or a.destination})"
                                   if (a.app_name or a.playlist or a.destination) else "")
                                  for a in actions)
        summary = (f"You did this {freq} times: when {human_anchor}, "
                   f"you {human_actions}. Automate it?")

        intent = Intent(
            trigger=trigger, actions=actions, requires_confirmation=True,
            confidence=confidence,
            summary=f"When {human_anchor}: {human_actions}.",
        )
        results.append(PatternResult(
            sequence=[anchor, *follow], frequency=freq, confidence=confidence,
            suggestion_summary=summary, suggested_intent=intent,
        ))
    return results
