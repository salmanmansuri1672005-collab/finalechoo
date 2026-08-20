"""Pydantic contracts + the strict AI action catalog.

Owners: Swati (contracts) + Rajersh (AI schema). See docs/AI_ACTION_SCHEMA.md.
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

SCHEMA_VERSION = 1

SUPPORTED_TRIGGERS = {
    "location_enter", "location_exit", "time_schedule", "calendar_event",
    "bluetooth_connect", "bluetooth_disconnect", "pattern_detected", "manual",
}

SUPPORTED_CONDITIONS = {"time_between", "day_of_week", "mode_active"}

# action type -> (sensitive, required_permission or None, simulated_on_device)
SUPPORTED_ACTIONS: Dict[str, tuple] = {
    "focus_mode":       (False, "dnd", False),
    "dnd_on":           (False, "dnd", False),
    "dnd_off":          (False, "dnd", False),
    "silent_mode":      (False, "dnd", False),
    "wifi_toggle":      (False, "connectivity", True),
    "bluetooth_toggle": (False, "connectivity", True),
    "open_app":         (False, None, False),
    "play_music":       (False, None, False),
    "navigation_start": (False, "location", False),
    "send_message":     (True,  "messaging", False),
    "create_reminder":  (False, None, False),
    "calendar_summary": (False, "calendar", False),
    "notify_user":      (False, "notifications", False),
    "set_alarm":        (False, None, False),
    "brightness":       (False, "settings", True),
}

SENSITIVE_ACTIONS = {a for a, (s, _, _) in SUPPORTED_ACTIONS.items() if s}
AUTONOMY_LEVELS = ["suggest", "confirm", "automatic"]
MAX_ACTIONS = 5 


# ---------- Structured intent ----------

class Trigger(BaseModel):
    type: str
    place: Optional[str] = None
    time: Optional[str] = None
    days: Optional[List[str]] = None
    match: Optional[str] = None
    offset_minutes: Optional[int] = None
    device: Optional[str] = None
    pattern_id: Optional[str] = None


class Condition(BaseModel):
    type: str
    start: Optional[str] = None
    end: Optional[str] = None
    days: Optional[List[str]] = None
    mode: Optional[str] = None


class Action(BaseModel):
    type: str
    value: Optional[Any] = None
    enabled: Optional[bool] = None
    package: Optional[str] = None
    app_name: Optional[str] = None
    playlist: Optional[str] = None
    destination: Optional[str] = None
    to: Optional[str] = None
    text: Optional[str] = None
    title: Optional[str] = None
    time: Optional[str] = None
    event_time: Optional[str] = None
    level: Optional[int] = None


class Intent(BaseModel):
    schema_version: int = SCHEMA_VERSION
    trigger: Trigger
    conditions: List[Condition] = Field(default_factory=list)
    actions: List[Action]
    duration_minutes: Optional[int] = None
    required_permissions: List[str] = Field(default_factory=list)
    requires_confirmation: bool = True
    confidence: float = 0.5
    summary: str = ""


# ---------- /ai/parse ----------

class ParseRequest(BaseModel):
    text: str
    user_context: Dict[str, Any] = Field(default_factory=dict)


class ParseResponse(BaseModel):
    intent: Intent
    valid: bool
    rejected_actions: List[str] = Field(default_factory=list)
    engine: str = "rule_based"


# ---------- /ai/commitment ----------

class SourceItem(BaseModel):
    id: str
    source: str = "notification"
    sender: Optional[str] = None
    text: str


class CommitmentRequest(BaseModel):
    items: List[SourceItem]
    today: Optional[str] = None


class Commitment(BaseModel):
    item_id: str
    task: str
    person: Optional[str] = None
    deadline: Optional[str] = None
    confidence: float
    raw_excerpt: str


class ClassifiedItem(BaseModel):
    item_id: str
    priority: str  # needs_attention | later | low


class CommitmentResponse(BaseModel):
    commitments: List[Commitment]
    classified: List[ClassifiedItem]


# ---------- /ai/pattern ----------

class ContextEvent(BaseModel):
    type: str
    value: Optional[str] = None
    ts: str


class PatternRequest(BaseModel):
    events: List[ContextEvent]
    min_frequency: int = 3


class PatternResult(BaseModel):
    sequence: List[str]
    frequency: int
    confidence: float
    suggestion_summary: str
    suggested_intent: Optional[Intent] = None


class PatternResponse(BaseModel):
    patterns: List[PatternResult]


# ---------- /ai/plan ----------

class CalendarEvent(BaseModel):
    title: str
    start: str
    end: str


class TaskItem(BaseModel):
    title: str
    est_minutes: int = 30
    priority: int = 3  # 1 = highest


class CommitmentItem(BaseModel):
    task: str
    deadline: Optional[str] = None


class PlanRequest(BaseModel):
    date: str
    calendar: List[CalendarEvent] = Field(default_factory=list)
    tasks: List[TaskItem] = Field(default_factory=list)
    commitments: List[CommitmentItem] = Field(default_factory=list)
    day_start: str = "08:00"
    day_end: str = "22:00"


class PlanBlock(BaseModel):
    start: str
    end: str
    title: str
    kind: str  # fixed | task | commitment | break
    source: str
    editable: bool


class PlanResponse(BaseModel):
    date: str
    blocks: List[PlanBlock]
    notes: List[str] = Field(default_factory=list)
