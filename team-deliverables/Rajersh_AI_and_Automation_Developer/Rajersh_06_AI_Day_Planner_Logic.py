"""AI day planner: calendar + tasks + commitments → prioritized editable plan.

Owner: Rajersh. Implements FR-10 / SRS §9.5. Fixed events and explicit deadlines
are respected; task blocks are editable.
"""
from __future__ import annotations

from datetime import datetime, timedelta
from typing import List, Optional, Tuple

from ..schemas import PlanBlock, PlanRequest, PlanResponse

BREAK_AFTER_MINUTES = 120
BREAK_LEN = 15
LUNCH = ("13:00", "13:40")


def _t(date: str, hhmm: str) -> datetime:
    return datetime.fromisoformat(f"{date}T{hhmm}:00")


def _fmt(dt: datetime) -> str:
    return dt.strftime("%H:%M")


def _free_slots(req: PlanRequest, fixed: List[Tuple[datetime, datetime]]
                ) -> List[Tuple[datetime, datetime]]:
    day_start, day_end = _t(req.date, req.day_start), _t(req.date, req.day_end)
    slots, cursor = [], day_start
    for s, e in sorted(fixed):
        if s > cursor:
            slots.append((cursor, min(s, day_end)))
        cursor = max(cursor, e)
    if cursor < day_end:
        slots.append((cursor, day_end))
    return [(s, e) for s, e in slots if (e - s).total_seconds() >= 15 * 60]


def build_plan(req: PlanRequest) -> PlanResponse:
    notes: List[str] = []
    blocks: List[PlanBlock] = []

    fixed: List[Tuple[datetime, datetime]] = []
    for ev in req.calendar:
        s, e = _t(req.date, ev.start), _t(req.date, ev.end)
        fixed.append((s, e))
        blocks.append(PlanBlock(start=ev.start, end=ev.end, title=ev.title,
                                kind="fixed", source="calendar", editable=False))
    # lunch as a soft fixed block if free
    ls, le = _t(req.date, LUNCH[0]), _t(req.date, LUNCH[1])
    if all(e <= ls or s >= le for s, e in fixed) and \
            _t(req.date, req.day_start) <= ls and le <= _t(req.date, req.day_end):
        fixed.append((ls, le))
        blocks.append(PlanBlock(start=LUNCH[0], end=LUNCH[1], title="Lunch",
                                kind="break", source="planner", editable=True))

    # Order work: commitments with deadlines first (earliest deadline), then tasks by priority.
    work: List[Tuple[str, int, str, Optional[datetime]]] = []  # (title, minutes, kind, deadline)
    for c in req.commitments:
        dl = None
        if c.deadline:
            try:
                dl = datetime.fromisoformat(c.deadline)
            except ValueError:
                dl = None
        work.append((c.task, 45, "commitment", dl))
    work.sort(key=lambda w: w[3] or datetime.max)
    for t in sorted(req.tasks, key=lambda t: t.priority):
        work.append((t.title, t.est_minutes, "task", None))

    slots = _free_slots(req, fixed)
    unplaced: List[str] = []
    si = 0
    minutes_since_break = 0
    cursor = slots[si][0] if slots else None

    for title, minutes, kind, deadline in work:
        placed = False
        while si < len(slots):
            slot_start, slot_end = slots[si]
            if cursor is None or cursor < slot_start:
                cursor = slot_start
            # insert a short break after long stretches
            if minutes_since_break >= BREAK_AFTER_MINUTES and \
                    (slot_end - cursor).total_seconds() / 60 >= BREAK_LEN + minutes:
                b_end = cursor + timedelta(minutes=BREAK_LEN)
                blocks.append(PlanBlock(start=_fmt(cursor), end=_fmt(b_end),
                                        title="Break", kind="break",
                                        source="planner", editable=True))
                cursor = b_end
                minutes_since_break = 0
            if (slot_end - cursor).total_seconds() / 60 >= minutes:
                end = cursor + timedelta(minutes=minutes)
                if deadline and end > deadline:
                    notes.append(f"'{title}' may miss its deadline "
                                 f"({deadline.strftime('%H:%M')}) — consider moving it earlier.")
                blocks.append(PlanBlock(start=_fmt(cursor), end=_fmt(end), title=title,
                                        kind=kind, source="planner", editable=True))
                if deadline:
                    notes.append(f"'{title}' scheduled at {_fmt(cursor)} "
                                 f"(deadline {deadline.strftime('%a %H:%M')}).")
                cursor = end
                minutes_since_break += minutes
                placed = True
                break
            si += 1
            cursor = None
        if not placed:
            unplaced.append(title)

    if unplaced:
        notes.append("Not enough free time today for: " + ", ".join(unplaced) +
                     ". They roll over to tomorrow.")
    blocks.sort(key=lambda b: b.start)
    return PlanResponse(date=req.date, blocks=blocks, notes=notes)
