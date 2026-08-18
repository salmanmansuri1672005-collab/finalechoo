"""Optional LLM integration for intent parsing. Owner: Rajersh + Swati.

When ECHOOS_LLM_API_KEY is set, /ai/parse tries the LLM first and falls back to
the rule-based parser on any failure (network, malformed JSON, schema violation).
The LLM output is never trusted directly — it always passes validation.py.
"""
from __future__ import annotations

import json
import logging
from typing import Optional

import httpx

from .. import config
from ..schemas import Intent, SUPPORTED_ACTIONS, SUPPORTED_TRIGGERS

log = logging.getLogger("echoos.llm")

SYSTEM_PROMPT = f"""You convert a user's automation request into STRICT JSON.
Output ONLY a JSON object with keys: trigger, conditions, actions, duration_minutes,
required_permissions, requires_confirmation, confidence, summary.
Allowed trigger.type values: {sorted(SUPPORTED_TRIGGERS)}.
Allowed actions[].type values: {sorted(SUPPORTED_ACTIONS)}.
Never invent other action types. Never include financial, password or security actions.
Set requires_confirmation=true for anything involving messages or uncertainty.
confidence is 0..1. summary is one human-readable sentence."""


def parse_with_llm(text: str) -> Optional[Intent]:
    if not config.llm_enabled():
        return None
    try:
        resp = httpx.post(
            f"{config.LLM_BASE_URL}/messages",
            headers={
                "x-api-key": config.LLM_API_KEY,
                "anthropic-version": "2023-06-01",
                "content-type": "application/json",
            },
            json={
                "model": config.LLM_MODEL,
                "max_tokens": 800,
                "system": SYSTEM_PROMPT,
                "messages": [{"role": "user", "content": text}],
            },
            timeout=config.LLM_TIMEOUT_S,
        )
        resp.raise_for_status()
        content = resp.json()["content"][0]["text"]
        start, end = content.find("{"), content.rfind("}")
        if start < 0 or end <= start:
            raise ValueError("no JSON object in LLM output")
        return Intent.model_validate(json.loads(content[start:end + 1]))
    except Exception as exc:  # any failure → deterministic fallback
        log.warning("LLM parse failed, falling back to rules: %s", exc)
        return None
