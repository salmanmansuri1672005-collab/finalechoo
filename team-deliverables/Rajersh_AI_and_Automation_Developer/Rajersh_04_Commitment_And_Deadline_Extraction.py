"""EchoLens: commitment + deadline extraction and notification classification.

Owner: Rajersh. Implements FR-06, FR-07, FR-08.
"""
from __future__ import annotations

import re
from datetime import datetime, timedelta
from typing import List, Optional, Tuple

from ..schemas import ClassifiedItem, Commitment, SourceItem

COMMITMENT_PATTERNS = [
    r"\bi(?:'|’)?ll\s+(.+)",
    r"\bi\s+will\s+(.+)",
    r"\bcan you\s+(.+?)\??$",
    r"\bcould you\s+(.+?)\??$",
    r"\bplease\s+(send|share|submit|finish|complete|review|call|pay)\s+(.+)",
    r"\bdon'?t forget to\s+(.+)",
    r"\bremind(?:er)?:?\s+(.+)",
    r"\b(?:need|have)\s+to\s+(.+?)(?:\s+by\b.*)?$",
    r"\bpromised?\s+(?:to\s+)?(.+)",
    r"\bdue\b(.+)",
    r"\bsubmit\s+(.+)",
    r"\bdeadline\b(.+)",
]

URGENT_WORDS = ("urgent", "asap", "immediately", "now", "today", "deadline",
                "due", "overdue", "important", "reminder", "otp", "payment",
                "interview", "exam", "meeting")
LOW_WORDS = ("sale", "offer", "discount", "% off", "new video", "subscribed",
             "liked your", "followed you", "streak", "promo", "cashback",
             "recommended for you", "weekly digest")

DAYS = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]

_TIME_RE = re.compile(r"(\d{1,2})(?::(\d{2}))?\s*(am|pm)?", re.IGNORECASE)


def _extract_deadline(text: str, today: datetime) -> Optional[str]:
    low = text.lower()
    date: Optional[datetime] = None

    if "day after tomorrow" in low:
        date = today + timedelta(days=2)
    elif "tomorrow" in low:
        date = today + timedelta(days=1)
    elif "tonight" in low or "today" in low or "eod" in low:
        date = today
    elif "next week" in low:
        date = today + timedelta(days=7)
    else:
        for i, day in enumerate(DAYS):
            if re.search(rf"\b(?:by |on |before |next )?{day}\b", low):
                delta = (i - today.weekday()) % 7 or 7
                date = today + timedelta(days=delta)
                break
    m = re.search(r"\bby the (\d{1,2})(?:st|nd|rd|th)?\b", low)
    if not date and m:
        d = int(m.group(1))
        date = today.replace(day=d) if d >= today.day else (
            (today.replace(day=1) + timedelta(days=32)).replace(day=d)
        )
    if not date:
        return None

    hour, minute = 18, 0  # default end-of-workday
    tm = re.search(r"(?:by|at|before)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b", low)
    if tm:
        hour = int(tm.group(1)) % 12 + (12 if tm.group(3).lower() == "pm" else 0)
        minute = int(tm.group(2) or 0)
    elif "eod" in low or "tonight" in low:
        hour = 20 if "tonight" in low else 18
    elif "morning" in low:
        hour = 9
    return date.replace(hour=hour, minute=minute, second=0, microsecond=0).isoformat(timespec="minutes")


def _extract_task(text: str) -> Tuple[Optional[str], float]:
    low = text.lower().strip()
    for pat in COMMITMENT_PATTERNS:
        m = re.search(pat, low)
        if m:
            task = m.group(m.lastindex or 1).strip(" .!?,")
            task = re.sub(r"\bby\s+(tomorrow|today|tonight|monday|tuesday|wednesday|"
                          r"thursday|friday|saturday|sunday|next week|eod|the \d+\w*)"
                          r"(\s+at\s+[\w: ]+)?$", "", task).strip(" .!?,")
            if len(task) >= 3:
                base = 0.85 if pat.startswith(r"\bi") or "can you" in pat or "could you" in pat else 0.7
                return task, base
    return None, 0.0


def _extract_person(item: SourceItem) -> Optional[str]:
    if item.sender:
        return item.sender
    m = re.search(r"\b(?:to|for|with)\s+([A-Z][a-z]{2,})\b", item.text)
    return m.group(1) if m else None


def classify(item: SourceItem, has_commitment: bool) -> str:
    low = item.text.lower()
    if any(w in low for w in LOW_WORDS):
        return "low"
    if has_commitment or any(w in low for w in URGENT_WORDS):
        return "needs_attention"
    if item.source in ("whatsapp", "sms", "email", "slack", "teams"):
        return "later"
    return "low"


def extract(items: List[SourceItem], today_str: Optional[str] = None
            ) -> Tuple[List[Commitment], List[ClassifiedItem]]:
    today = datetime.fromisoformat(today_str) if today_str else datetime(2026, 8, 17)
    commitments: List[Commitment] = []
    classified: List[ClassifiedItem] = []

    for item in items:
        task, conf = _extract_task(item.text)
        deadline = _extract_deadline(item.text, today) if task else None
        if task:
            if deadline:
                conf = min(0.95, conf + 0.1)
            commitments.append(Commitment(
                item_id=item.id,
                task=task,
                person=_extract_person(item),
                deadline=deadline,
                confidence=round(conf, 2),
                raw_excerpt=item.text[:140],
            ))
        classified.append(ClassifiedItem(item_id=item.id,
                                         priority=classify(item, task is not None)))
    return commitments, classified
