# ADR 0004: Eval Harness in ai-common (pure logic, not a Spring AI Evaluator)

## Status
Accepted

## Context
We need a reusable evaluation harness for regression and spot-checks across demos without coupling to Spring AI's Evaluator abstractions. The harness must be deterministic, testable in isolation, and follow repo vocabulary (avoid `evaluator`/`grader` terms; use `Criterion`/`Grade`/`EvalHarness`).

## Decision
- **Location**: `ai-common` module under `dev.prasadgaikwad.springaiapps.common.eval` as pure logic (Spring `Assert` only, no heavy framework deps).
- **API**: `EvalCase(id, question, answer)`, `Criterion` with factories (`contains`, `excludes`, `matchesRegex`, `equalsText`), `of(name, expectation, Predicate<EvalCase>)`. `Grade` + `CaseReport` (with `passed` derived) + `EvalReport`/`Summary`. `EvalHarness.run(...)` produces report; `regressionsAgainst(baseline)` returns pass→fail in current-report order (first baseline occurrence wins on dup ids).
- **Semantics**: `contains` and `excludes` ignore case; `equalsText` also trims and collapses whitespace. `matchesRegex` uses find semantics with fail-fast on bad patterns and is case-sensitive unless regex flags request otherwise. Answer may be empty.
- **Testing**: Focused unit tests live in `ai-common`; `ai-eval` separately verifies REST orchestration using model mocks.

## Alternatives and implementation limits

A model-based judge or Spring AI-specific evaluation abstraction would couple deterministic checks to model calls and transport. Keeping the shared harness pure makes repeated inputs reproducible and permits custom predicates over the whole case. Answer generation belongs in `ai-eval`, not in the shared harness.

The harness rejects duplicate IDs within a run. Manually constructed reports can contain duplicates; comparison takes the first baseline occurrence. `CaseReport` derives its passed flag, while a manually supplied `EvalReport` summary is not recomputed from cases. `Summary` validates non-negative/range/add-up checks, not every possible relationship to an externally supplied case list.

`ai-eval` currently assigns positional IDs and keeps unbounded in-memory history. These are demo-level limitations rather than requirements of the shared harness. Regression IDs describe pass→fail only; they do not establish semantic answer quality.

## Consequences
- Reusable across demos with a clean secondary testing seam
- Deterministic, framework-agnostic logic easy to reason about
- Vocabulary aligned with repo glossary

## References
- Issue #4, PR #15, CONTEXT.md
- [Shared harness guide](../../ai-common/README.md)
- [ai-eval orchestration and limitations](../../ai-eval/README.md)
