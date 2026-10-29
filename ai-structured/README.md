# ai-structured

Typed extraction to validated POJOs using Spring AI structured output.

## Features

- **Structured output** — Map free text to typed Java records/POJOs
- **Bean Validation** — Validate extracted objects against constraints; report violations
- **Multiple extraction targets** — Candidate profiles and customer orders out of the box

## APIs

- `POST /api/structured/extract` — Extract structured data (`type`: `candidate` or `order`)
- OpenAPI docs at `/v3/api-docs`

## Run

```bash
mvn -pl ai-structured -am spring-boot:run
```
