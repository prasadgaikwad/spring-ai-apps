# ai-eval

In-app evaluation demo using the shared Eval harness from `ai-common`.

## Features

- **Harness-backed grading** — Grade Q&A pairs against criteria (`contains`, `excludes`, `matchesRegex`, `equalsText`).
- **Answer generation** — Automatically generate answers via the configured ChatModel when omitted.
- **Regression tracking** — Compare runs and surface regressions (passed previously, failing now).
- **Run history** — Store and retrieve evaluation runs.

## APIs

- `POST /api/eval/run` — Run evaluation
- `GET /api/eval/runs` — List stored runs
- `GET /api/eval/runs/{runId}` — Get a specific run
- OpenAPI docs at `/v3/api-docs`

## Run

```bash
mvn -pl ai-eval -am spring-boot:run
```
