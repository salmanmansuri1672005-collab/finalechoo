# Git & Collaboration Workflow (SRS §21)

```
main
├── feature/salman-frontend
├── feature/swati-backend
├── feature/tushar-data-security
├── feature/rajersh-ai-automation
└── feature/subh-qa-devops
```

Rules:

1. Never push unfinished experimental work directly to `main`.
2. Small feature branches; open a PR once reviewable.
3. Changes touching shared contracts (`docs/API_CONTRACTS.md`, `docs/AI_ACTION_SCHEMA.md`,
   Room entities, Pydantic schemas) require review by at least one other member and a heads-up
   in the team channel **before** merge.
4. Subh maintains the integration/release branch and the regression checklist.
5. Resolve merge conflicts on the feature branch before requesting final review.
6. Commit style: `module: short imperative summary` (e.g. `ai: clamp confidence to [0,1]`).

Integration checkpoints (SRS §22): UI↔Backend (Salman+Swati), Backend↔Data (Swati+Tushar),
Backend↔AI (Swati+Rajersh), Automation↔Security (Rajersh+Tushar), All↔QA (Subh).
