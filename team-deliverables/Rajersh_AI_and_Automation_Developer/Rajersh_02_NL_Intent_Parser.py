"""Natural-language → structured intent. Owner: Rajersh.

Deterministic rule-based engine (always available, offline-friendly). When an
LLM key is configured, services.llm tries the model first and this module acts
as the fallback for malformed/unsafe output (SRS §19.4 'fallback behavior').
"""
from __future__ import annotations

import re
from typing import Optional

from ..schemas import Action, Condition, Intent, Trigger

PLACES = ["college", "work", "home", "gym", "office", "school", "library"]
DAY_WORDS = {
    "monday": "mon", "tuesday": "tue", "wednesday": "wed", "thursday": "thu",
    "friday": "fri", "saturday": "sat", "sunday": "sun",
    "weekday": "weekdays", "weekend": "weekend",
}

_TIME_RE = re.compile(
    r"\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?\b", re.IGNORECASE
)


def _extract_time(text: str) -> Optional[str]:
    m = _TIME_RE.search(text)
    if not m:
        return None
    hour = int(m.group(1))
    minute = int(m.group(2) or 0)
    ampm = (m.group(3) or "").replace(".", "").lower()
    if hour > 23 or minute > 59:
        return None
    if ampm == "pm" and hour < 12:
        hour += 12
    if ampm == "am" and hour == 12:
        hour = 0
    # Bare numbers without am/pm and without ':' are too ambiguous ("send 2 messages")
    if not ampm and m.group(2) is None:
        return None
    return f"{hour:02d}:{minute:02d}"


def _extract_place(text: str) -> Optional[str]:
    for p in PLACES:
        if re.search(rf"\b{p}\b", text, re.IGNORECASE):
            return "work" if p == "office" else ("college" if p == "school" else p)
    return None


def _extract_trigger(text: str) -> Trigger:
    low = text.lower()
    place = _extract_place(low)

    if re.search(r"\b(leave|leaving|exit|depart)\b", low) and place:
        return Trigger(type="location_exit", place=place)
    if re.search(r"\b(reach|arrive|arriving|get to|enter|at the)\b", low) and place:
        return Trigger(type="location_enter", place=place)
    if re.search(r"\b(car|driving|drive)\b", low) and re.search(
        r"\b(connect|connects|start|starts|bluetooth|in the car|when i drive|driving)\b", low
    ):
        return Trigger(type="bluetooth_connect", device="car")

    t = _extract_time(low)
    if t or re.search(r"\b(every (day|morning|evening|night)|daily)\b", low):
        days = [d for w, d in DAY_WORDS.items() if w in low] or None
        if not t:
            t = {"morning": "07:00", "evening": "19:00", "night": "22:00"}.get(
                next((w for w in ("morning", "evening", "night") if w in low), ""), "08:00"
            )
        return Trigger(type="time_schedule", time=t, days=days)

    if re.search(r"\b(before|after) (my |the )?(meeting|class|event)\b", low):
        m = re.search(r"\b(meeting|class|event)\b", low)
        return Trigger(type="calendar_event", match=m.group(1), offset_minutes=-10)

    if place:  # "my college routine" without a verb — assume arrival
        return Trigger(type="location_enter", place=place)
    return Trigger(type="manual")


def _extract_actions(text: str, trigger: Trigger) -> list[Action]:
    low = text.lower()
    actions: list[Action] = []

    if re.search(r"\bfocus\b|\bstudy mode\b|\bfocus routine\b", low):
        actions.append(Action(type="focus_mode", value=trigger.place or "focus"))
    if re.search(r"\b(silent|silence|mute)\b", low):
        actions.append(Action(type="silent_mode", enabled=True))
    if re.search(r"\b(do not disturb|dnd)\b", low):
        actions.append(Action(type="dnd_on"))
    if re.search(r"\b(music|playlist|spotify|song)\b", low):
        pl = re.search(r"(?:play|start)\s+(?:my\s+)?([\w\s]+?)\s*(?:playlist|music)", low)
        actions.append(Action(type="play_music", playlist=(pl.group(1).strip() if pl else None)))
    if re.search(r"\b(navigate|navigation|directions|maps)\b", low):
        dest = _extract_place(low) or "home"
        actions.append(Action(type="navigation_start", destination=dest))
    if re.search(r"\b(remind|reminder)\b", low):
        m = re.search(r"remind (?:me )?(?:to |about )?(.+?)(?: at \d|$)", low)
        actions.append(Action(
            type="create_reminder",
            title=(m.group(1).strip().rstrip(".") if m else "Reminder"),
            time=_extract_time(low),
        ))
    if re.search(r"\b(alarm|wake me)\b", low):
        actions.append(Action(type="set_alarm", time=_extract_time(low) or "07:00"))
    if re.search(r"\b(text|message|send .*message|whatsapp|tell)\b", low):
        to = None
        m = re.search(r"\b(?:text|message|tell)\s+([A-Za-z]+)", text)
        if m and m.group(1).lower() not in ("me", "my", "the", "a"):
            to = m.group(1)
        actions.append(Action(type="send_message", to=to or "contact",
                              text="(compose on confirm)"))
    if re.search(r"\b(calendar|schedule|agenda|my day|today'?s events)\b", low):
        actions.append(Action(type="calendar_summary"))
    if re.search(r"\bwifi\b", low):
        actions.append(Action(type="wifi_toggle", enabled=not bool(re.search(r"\b(off|disable)\b", low))))
    if re.search(r"\bbrightness\b", low):
        m = re.search(r"(\d{1,3})\s*%?", low)
        actions.append(Action(type="brightness", level=min(100, int(m.group(1))) if m else 40))
    if re.search(r"\bopen\s+([a-z][\w\s]*)", low):
        m = re.search(r"\bopen\s+([a-z][\w]*)", low)
        if m and m.group(1) not in ("the", "my"):
            actions.append(Action(type="open_app", app_name=m.group(1)))

    # "focus routine" style requests with nothing else: focus + calendar summary
    if not actions and re.search(r"\broutine\b", low):
        actions.append(Action(type="focus_mode", value=trigger.place or "routine"))
        actions.append(Action(type="calendar_summary"))
    return actions


def parse(text: str, user_context: dict | None = None) -> Intent:
    trigger = _extract_trigger(text)
    actions = _extract_actions(text, trigger)

    confidence = 0.9 if (trigger.type != "manual" and actions) else (
        0.6 if actions else 0.2
    )
    conditions: list[Condition] = []
    low = text.lower()
    if "weekday" in low:
        conditions.append(Condition(type="day_of_week",
                                    days=["mon", "tue", "wed", "thu", "fri"]))

    dur = None
    m = re.search(r"for\s+(\d{1,3})\s*(minutes|min|hours|hour|hrs)", low)
    if m:
        dur = int(m.group(1)) * (60 if "h" in m.group(2) else 1)

    summary_bits = []
    tdesc = {
        "location_enter": f"When you arrive at {trigger.place}",
        "location_exit": f"When you leave {trigger.place}",
        "time_schedule": f"Every day at {trigger.time}",
        "bluetooth_connect": "When your car connects",
        "calendar_event": f"Around your {trigger.match}",
        "manual": "On demand",
    }.get(trigger.type, "When triggered")
    adesc = ", ".join(a.type.replace("_", " ") for a in actions) or "no supported actions"
    summary_bits.append(f"{tdesc}: {adesc}.")

    return Intent(
        trigger=trigger,
        conditions=conditions,
        actions=actions,
        duration_minutes=dur,
        requires_confirmation=True,  # validator may relax later per autonomy level
        confidence=confidence,
        summary=" ".join(summary_bits),
    )
