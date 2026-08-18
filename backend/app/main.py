"""EchoOS FastAPI AI orchestration backend. Owner: Swati.

The backend orchestrates AI — it never controls the device (SRS §7, §14).
Endpoints: /health, /config, /ai/parse, /ai/commitment, /ai/pattern, /ai/plan.
"""
from __future__ import annotations

import logging
import time

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from . import config
from .schemas import (AUTONOMY_LEVELS, CommitmentRequest, CommitmentResponse,
                      ParseRequest, ParseResponse, PatternRequest,
                      PatternResponse, PlanRequest, PlanResponse,
                      SENSITIVE_ACTIONS, SUPPORTED_ACTIONS, SUPPORTED_TRIGGERS)
from .services import commitments as svc_commitments
from .services import llm as svc_llm
from .services import parser as svc_parser
from .services import patterns as svc_patterns
from .services import planner as svc_planner
from .validation import ValidationError, validate_intent

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(name)s %(levelname)s %(message)s")
log = logging.getLogger("echoos")

app = FastAPI(title="EchoOS AI Backend", version=config.APP_VERSION)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"],
                   allow_headers=["*"])


def error_response(status: int, code: str, message: str, details: list | None = None):
    return JSONResponse(status_code=status, content={
        "error": {"code": code, "message": message, "details": details or []}})


@app.exception_handler(ValidationError)
async def validation_error_handler(_req: Request, exc: ValidationError):
    return error_response(422, exc.code, exc.message, exc.details)


@app.exception_handler(Exception)
async def internal_error_handler(_req: Request, exc: Exception):
    log.exception("internal error")
    return error_response(500, "INTERNAL", "Internal server error.")


@app.get("/health")
def health():
    return {"status": "ok", "version": config.APP_VERSION,
            "llm_mode": config.llm_mode(),
            "uptime_s": round(time.time() - config.START_TIME, 1)}


@app.get("/config")
def get_config():
    return {"schema_version": config.SCHEMA_VERSION,
            "supported_triggers": sorted(SUPPORTED_TRIGGERS),
            "supported_actions": sorted(SUPPORTED_ACTIONS),
            "sensitive_actions": sorted(SENSITIVE_ACTIONS),
            "autonomy_levels": AUTONOMY_LEVELS}


@app.post("/ai/parse", response_model=ParseResponse)
def ai_parse(req: ParseRequest):
    if not req.text.strip():
        raise ValidationError("BAD_REQUEST", "Text must not be empty.")
    engine = "rule_based"
    intent = svc_llm.parse_with_llm(req.text)
    if intent is not None:
        engine = "llm"
    else:
        intent = svc_parser.parse(req.text, req.user_context)
    intent, rejected = validate_intent(intent)  # raises 422 if nothing valid
    log.info("parse ok engine=%s trigger=%s actions=%d rejected=%s",
             engine, intent.trigger.type, len(intent.actions), rejected)
    return ParseResponse(intent=intent, valid=True,
                         rejected_actions=rejected, engine=engine)


@app.post("/ai/commitment", response_model=CommitmentResponse)
def ai_commitment(req: CommitmentRequest):
    found, classified = svc_commitments.extract(req.items, req.today)
    return CommitmentResponse(commitments=found, classified=classified)


@app.post("/ai/pattern", response_model=PatternResponse)
def ai_pattern(req: PatternRequest):
    return PatternResponse(patterns=svc_patterns.detect(req))


@app.post("/ai/plan", response_model=PlanResponse)
def ai_plan(req: PlanRequest):
    return svc_planner.build_plan(req)
