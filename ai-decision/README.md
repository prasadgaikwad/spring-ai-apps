# ai-decision

System One / Jev-style decision model support for typed decisions (Choice, Score, Noul).

## What are Decision models?

System One / Decision models (e.g., TypeSafe AI's **Jev**) evaluate state and return **typed, structured answers** with calibrated probabilities — they do **not** generate free-form text. They're designed for decision routing, gating, scoring, and guardrails where you need type-safe structured outputs.

## Primitives

- **Choice** — Select one option from choices with calibrated probability
- **Score** — Numeric score with calibrated probability/uncertainty  
- **Noul** — "None/null" answer indicating no valid choice applies

## APIs

- `POST /api/decision/decide` — Make a decision with state and questions
- `GET /api/decision/ping` — Health check
- OpenAPI docs at `/v3/api-docs`

## Configuration

```yaml
ai:
  decision:
    provider: jev
    base-url: ${JEV_BASE_URL:https://api.typesafe.ai}
    api-key: ${JEV_API_KEY:}  # or TYPESAFE_API_KEY / SYSTEMONE_API_KEY
    model: ${JEV_MODEL:jev-latest}
    timeout: 10s
```

## Architecture

- `DecisionClient` interface (pluggable) + `JevDecisionClient` implementation
- Typed DTOs in `DecisionPrimitives` for all three answer types
- Uses Spring WebClient for HTTP calls

## Run

```bash
mvn -pl ai-decision -am spring-boot:run
```

## Notes

- Default tests use mocked HTTP calls; no external network calls required.
- Local/offline implementations can be added via additional `DecisionClient` beans.
