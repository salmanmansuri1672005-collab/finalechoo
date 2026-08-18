"""EchoOS backend configuration. Owner: Swati.

LLM credentials come from environment only — never hard-coded (SRS §23).
Without a key the backend runs in deterministic rule-based mode, which is
sufficient for the full MVP demo.
"""
import os
import time

APP_VERSION = "1.0.0"
SCHEMA_VERSION = 1
START_TIME = time.time()

LLM_API_KEY = os.environ.get("ECHOOS_LLM_API_KEY", "")
LLM_BASE_URL = os.environ.get("ECHOOS_LLM_BASE_URL", "https://api.anthropic.com/v1")
LLM_MODEL = os.environ.get("ECHOOS_LLM_MODEL", "claude-sonnet-4-5")
LLM_TIMEOUT_S = float(os.environ.get("ECHOOS_LLM_TIMEOUT_S", "20"))


def llm_enabled() -> bool:
    return bool(LLM_API_KEY)


def llm_mode() -> str:
    return "llm" if llm_enabled() else "rule_based"
