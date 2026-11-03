# ADR 0004: Eval Harness in ai-common (pure logic, not a Spring AI Evaluator)

## Status
Accepted

## Context
We need a reusable evaluation harness for regression and spot-checks across demos without coupling to Spring AI's Evaluator abstractions. The harness must be deterministic, testable in isolation, and follow repo vocabulary (avoid `evaluator`/`grader` terms; use `Criterion`/`Grade`/`EvalHarness`).

## Decision
- **Location**: `ai-common` module under `dev.prasadgaikwad.springaiapps.common.eval` as pure logic (Spring `Assert` only, no heavy framework deps).
- **API**: `EvalCase(id, question, answer)`, `Criterion` with factories (`contains`, `excludes`, `matchesRegex`, `equalsText`), `of(name, expectation, Predicate<EvalCase>)`. `Grade` + `CaseReport` (with `passed` derived) + `EvalReport`/`Summary`. `EvalHarness.run(...)` produces report; `regressionsAgainst(baseline)` returns pass→fail in current-report order (first baseline occurrence wins on dup ids).
- **Semantics**: Case-insensitive for string helpers, `matchesRegex` uses find semantics with fail-fast on bad pattern, `equalsText` trims + whitespace-collapses + case-insensitive. Answer may be empty.
- **Testing**: Unit tests live in `ai-common` (29+ tests); used by `ai-eval` demo via REST.

## Consequences
- Reusable across demos with a clean secondary testing seam
- Deterministic, framework-agnostic logic easy to reason about
- Vocabulary aligned with repo glossary

## References
- Issue #4, PR #15, CONTEXT.md
- `ai-common/src/main/java/.../common/eval/*`
- `ai-eval` demo
