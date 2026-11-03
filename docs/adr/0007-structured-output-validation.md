# ADR 0007: Separate structured conversion from domain validation

## Status

Accepted; records the typed-extraction behavior delivered by PR #18 under issue #5.

## Context and decision

A model can return parseable JSON that violates application constraints. `ai-structured` selects a Java record target (`CandidateProfile` or `CustomerOrder`), uses `ChatClient.call().entity(Class)` for conversion, and then validates the parsed object with Jakarta Validation. Return the value plus sorted violations and derived `valid` in an HTTP 200 response when conversion succeeded. Reject unsupported targets, blank input, missing results, and conversion failures as bad requests.

This chooses an explicit inspection contract over assuming schema-shaped text is valid, rejecting every invalid record without details, or silently repairing fields. Invalid extracted values remain visible for callers to inspect; downstream callers must check `valid` before accepting them. There is currently no automated repair/retry loop.

## Consequences and references

Record constraints are a second, deterministic check independent of provider JSON behavior. They verify declared shape/range constraints, not factual fidelity to the input. Provider portability remains subject to conversion support and actual model output; the default tests use a mocked ChatModel rather than establishing native strict-schema support for each provider.

See [module contract and diagrams](../../ai-structured/README.md), [provider selection](0001-provider-portability-profiles-ollama-default.md), and [testing coverage](../architecture.md).
