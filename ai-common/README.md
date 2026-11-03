# ai-common

Shared infrastructure for all Spring AI demo modules in this monorepo.

## Contents

- **Provider-portable chat configuration** (`ChatModelConfiguration`) — marks the ChatModel for the active profile as `@Primary` (Ollama/OpenAI/Anthropic).
- **Vector store helpers** (`VectorStoreConfiguration`) — shared wiring for vector-backed demos.
- **REST error handling** (`ApiExceptionHandler`, `CommonError`, `BadRequestException`, `ResourceNotFoundException`) — uniform JSON errors across modules.
- **Eval harness** (`dev.prasadgaikwad.springaiapps.common.eval`) — pure-logic evaluation of Q&A pairs against criteria, producing regression-style reports. The secondary test seam for the monorepo.

## Design Notes

- **No heavy starters**: `ai-common` does not include `spring-ai-starter-vector-store-pgvector` (to avoid leaking a mandatory DataSource into every demo). PGVector lives in `ai-rag`.
- **Spring AI 2.0 compatibility**: Handles the all-beans auto-configuration behavior by making the profile-specific ChatModel primary.
- **Package**: `dev.prasadgaikwad.springaiapps.common.*`

## Usage

Add as a dependency:

```xml
<dependency>
    <groupId>dev.prasadgaikwad.spring-ai-apps</groupId>
    <artifactId>ai-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Eval Harness

Pure-logic evaluation (no framework dependencies beyond Spring Assert). See ADR-0004.

### Quick Example

```java
var report = new EvalHarness().run(List.of(
  new EvalCase("q1", "What is Spring AI?", "Spring AI is...")),
  List.of(Criterion.contains("spring-ai", "Mentions Spring AI"))
);
```

### Key Concepts
- `EvalCase(id, question, answer)`, `Criterion`, `Grade`, `CaseReport`, `EvalReport`/`Summary`
- Factories: `contains`, `excludes`, `matchesRegex`, `equalsText`
- `regressionsAgainst(baseline)` finds pass→fail
