# Learnings: Scaffolding a Spring Boot 4.1 + Spring AI 2.0 monorepo

Working notes from building the first ticket (parent POM + `ai-common` + six demo
skeletons) on `spring-ai-apps`. These are the non-obvious, hard-won facts that cost
real debugging time and would otherwise trip up the next engineer.

## 1. Java package names forbid hyphens — the module name and package differ

The Maven root package we wanted, `dev.prasadgaikwad.spring-ai-apps`, is **illegal in
Java** — package segments may only contain `[0-9a-zA-Z_$]`. The first compile failed
with a baffling `';' expected` at `[1,33]`, which is exactly where the first `-`
sits in the `package` declaration.

Resolution: the **Java root package is `dev.prasadgaikwad.springaiapps.*`**, while the
Maven `groupId`/`artifactId` and repo name stay `spring-ai-apps`. This split is easy
to reintroduce by accident, so it's recorded in both `CONTEXT.md` and `AGENTS.md`.

## 2. Where MockMvc moved in Spring Boot 4.1

Two independent Boot 4.1 changes break the classic `@SpringBootTest` + MockMvc pattern:

- `AutoConfigureMockMvc` is **no longer** in `spring-boot-test-autoconfigure`. It moved
  to a new artifact **`spring-boot-webmvc-test`**, which must be added explicitly
  (test-scoped, once, in the parent POM — it is not pulled in by
  `spring-boot-starter-test`).
- Its package changed to **`org.springframework.boot.webmvc.test.autoconfigure`**
  (old: `org.springframework.boot.test.autoconfigure.web.servlet`).

Fix both or you'll see `package org.springframework.boot.test.autoconfigure.web.servlet
does not exist` at test-compile.

## 3. Spring AI 2.0 auto-configures model beans unconditionally

When the OpenAI or Anthropic **starter** is on the classpath, Spring AI auto-creates
**all** of that provider's model beans (chat, image, audio speech, audio transcription,
embedding) — even under the `ollama` profile where they're unused. With no `api-key`
set, context-load fails with `At least one credential source must be specified`.

We wired all three provider starters (per the "provider-agnostic" goal), so every demo
defines **non-empty placeholder keys in the base config**:

```yaml
openai:
  api-key: ${OPENAI_API_KEY:not-configured}
anthropic:
  api-key: ${ANTHROPIC_API_KEY:not-configured}
```

Real keys override via env when running the `openai` / `anthropic` profiles.

## 4. `ai-common` must not force the PGVector starter on every demo

Putting `spring-ai-starter-vector-store-pgvector` in `ai-common` makes **every** demo,
even ones with no Vector store, require a DataSource + JDBC driver. The failure is
`Failed to determine a suitable driver class` on context-load.

Rule: `ai-common` owns the **shared code** (Vector store config, Eval harness, prompt
templates, REST error handling) but **not** the heavy PGVector starter. A demo that
actually uses a Vector store (`ai-rag`) declares the starter itself. Documented in
ADR-0002.

## 5. ai-rag's smoke test excludes the DB autoconfigs

Because `ai-rag` deliberately keeps the PGVector starter, its context-load smoke test
excludes the DB-dependent autoconfigurations so it can load without a running Postgres:

```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
      "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration," +
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
```

The real Vector store integration is covered by the ETL/query work (issues #2/#3)
against a running Postgres.

## 6. POM XML gotcha: escape `&` in descriptions

A bare `&` in a POM `<description>` (e.g. "grading Q&A pairs") is not valid XML and
fails the parent read with `Non-parseable POM ... entity reference name can not contain
character '`. Use `&amp;`.

## 7. Running a single module needs `-am`

`mvn -pl ai-rag test` fails with `Could not find artifact ... ai-common` unless ai-common
is installed to the local repo. Use `-pl ai-rag -am` to build prereqs in-reactor.

## Commands that work

- Whole monorepo compile: `mvn -DskipTests compile`
- Whole monorepo test: `mvn test`
- Single demo test (builds ai-common in-reactor): `mvn test -pl ai-rag -am`
- Start Postgres/PGVector: `docker compose up -d`
