# Session-memory migration plan (issue #47)

**Status:** conditional; **not applicable today**. No code migration is planned until a demo adds multi-turn conversation memory, or until the deprecated `ChatMemory` API must be removed before Spring AI 3.0.

**Last reviewed:** 2026-11-16

**Evidence version:** Spring AI `2.1.0-RC2` documentation and APIs. Re-check names and properties when Spring AI 2.1 reaches GA; the module baseline remains on `2.0.2` until the upgrade in #36.

Tracking: [#47](https://github.com/prasadgaikwad/spring-ai-apps/issues/47), parent [#36](https://github.com/prasadgaikwad/spring-ai-apps/issues/36).

## Current state

The demos are intentionally stateless between requests:

- `ai-agents` stores no application-level conversation history; `/api/agents/ask`, `/api/agents/stream`, and the `/chat` page treat each request independently.
- `ai-rag` answers each `/api/rag/query` and `/chat` request independently. Retrieval context is assembled per query and is not conversation history.
- `ai-eval` keeps bounded, process-local run history for the evaluation harness. That is not chat memory and must not be moved into a session store.
- `ai-decision`, `ai-structured`, `ai-multimodal`, and `ai-observability` have no conversation-memory feature.

No module directly uses `ChatMemory`, `MessageChatMemoryAdvisor`, `ChatMemoryRepository`, or the session APIs. The deprecated chat-memory auto-configuration may still be present transitively; that does not create conversation state because no advisor is wired.

**Decision now:** do not add a session dependency, database schema, or migration code preemptively. Keep the existing endpoints unchanged. Close or defer this issue after recording the trigger; reopen it, or open a narrower implementation issue, when a real feature needs history.

### Non-goals

- This is not a design for semantic long-term memory. `VectorStoreChatMemoryAdvisor` is not deprecated and addresses vector-based recall, not the turn-by-turn history described here; evaluate it separately if a feature needs that.
- Do not move evaluation run history, observation spans, or RAG vector retention into a session store. They have their own lifecycle and tests.

## Trigger

Start the migration when **all** of these are true:

1. A demo adds a user-visible conversation that spans more than one request, most likely the `ai-agents` `/chat` page or the `ai-rag` `/chat` page.
2. The feature needs the model to receive earlier turns, not just the current request and its retrieved context.
3. The session scope, owner/isolation, retention, and delete/export behavior below can be answered for that feature.

The second trigger is **Spring AI 3.0 removal**: if no feature has needed history by then, remove any deprecated chat-memory usage and do not add a session store just to preserve an unused API.

## Inventory to complete before implementation

Fill this in for the first feature that triggers the migration; do not guess it from the demo alone.

| Question | Why it matters | Proposed default for the first feature |
| --- | --- | --- |
| Which endpoint/page gets history? | Determines whether the feature is `ai-agents`, `ai-rag`, or both | One module first; do not add history to every demo. |
| What is a conversation? | Session identity and scope | One session per user-visible conversation, not per request or per browser tab unless the UI says otherwise. |
| Who owns a session? | Authorization and isolation | The authenticated principal if authentication exists; otherwise the demo is explicitly single-user/local. |
| Is the session ID client- or server-generated? | Prevents callers from reading another user's history | Server-generated UUID returned to the UI; if the client supplies an ID, authorize every read/write against the principal. |
| Which messages/tools/media are stored? | Privacy and prompt correctness | User, assistant, tool calls/results needed for turn integrity; no system prompts by default. |
| Are retrieved RAG chunks stored in the session? | Stale context and data retention | Store only the conversation. Keep retrieval context in the request/response, or record source metadata as a non-prompt event payload for display. |
| What is the retention promise? | Privacy and storage cost | 30 days for a durable demo, with a scheduled cleanup; shorter if the data is sensitive. |
| What happens on restart? | In-memory vs JDBC choice | In-memory is acceptable for a local demo; durable profile uses JDBC. |
| Are multiple app instances expected? | Repository choice | JDBC; in-memory is not shared across instances. |
| Is the history visible/exportable/deletable by the user? | Privacy obligations | Add a delete path before enabling history for real users; document export/erasure limits. |

## Storage choice

### In-memory: default for a local demo

`InMemorySessionRepository` is the simplest option and needs no database. It is suitable for tests and a single-process local demo.

Limits, from the Spring AI documentation:

- Sessions are lost on restart.
- Sessions are not shared across instances.
- It still supports the full `SessionService`, events, turn-aware compaction, TTL, and optimistic concurrency APIs, so the application boundary can be exercised without a database.

Use it only when the feature is explicitly local/demo-only. Do not describe it as durable history.

### JDBC: durable profile

`JdbcSessionRepository` / `spring-ai-starter-model-session-jdbc` persists sessions in a relational database. As of 2.1 RC2 it supports PostgreSQL, MySQL, MariaDB, and H2 out of the box; other databases are rejected at startup unless a custom dialect is supplied.

The JDBC schema has three tables:

| Table | Contents |
| --- | --- |
| `AI_SESSION` | Session metadata: id, created/expiry timestamps, metadata JSON, event version. |
| `AI_SESSION_INDEX` | Indexed metadata attributes, always including the principal name, for scoped lookup. |
| `AI_SESSION_EVENT` | Append-only event log ordered by a database-assigned sequence. |

The schema scripts live on the classpath. `spring.ai.session.repository.jdbc.initialize-schema` defaults to `embedded`; for PostgreSQL, either set it to `always` in a disposable demo environment or apply the PostgreSQL script with the existing migration process. The script is safe to re-run.

Recommended placement:

- **`ai-rag`:** reuse the existing PostgreSQL server but use a **separate schema or database** for session tables. Do not put session rows in the PGVector `vector_store` table or share its cleanup logic. If the feature needs durable history, the `ai-rag` Testcontainers PostgreSQL instance can host both schemas, but tests must prove the session tables are isolated from the vector-store schema.
- **`ai-agents`:** it currently has no database dependency. Add JDBC sessions only behind a durable profile, or keep the local demo on the in-memory repository. Do not make a database mandatory for the basic agent demo.
- **`ai-eval`:** keep its run history where it is. It is not a conversation and has different retention/reset semantics.

The old `JdbcChatMemoryRepository` and the session repository do **not** share tables. `SPRING_AI_CHAT_MEMORY` rows are not read by session memory; any migration of existing conversations needs a deliberate export/import, not a configuration switch.

## Session scope and isolation

The session ID is required on every call. There is no shared default session: the documentation deliberately throws when the memory advisor is called without an ID, because a default would silently merge histories.

Proposed scope:

- One session per `(principal, agent, conversation)`. The conversation ID is a UUID created by the server when the UI starts a conversation.
- The principal is stored in session metadata under `SessionAttributes.PRINCIPAL_NAME`; the JDBC index makes `findByMetadata(PRINCIPAL_NAME, value)` possible. A session has metadata, not an owner field, so authorization is application code.
- On every request, resolve the session by ID and verify the principal before reading or appending. Never accept a session ID alone as authorization.
- If authentication is absent, the demo is single-user/local only. State that limitation in the module guide; do not imply multi-tenant isolation from an unguessable UUID.
- Keep the agent name/version/model in `SessionAttributes` metadata for display and audit. Do not put secrets, prompts, or arbitrary user profile data in metadata.
- For `ai-rag`, retrieval context remains scoped to the current query. Do not replay old retrieved chunks as if they were current evidence. If the UI needs to show sources per turn, store them as a non-message event payload or return them to the client; do not add them to the model prompt automatically.
- For `ai-agents`, a session may contain tool calls and results. Tool traffic must stay turn-consistent; do not filter out one half of a tool call/result pair.

## Retention, eviction, and compaction

### Session expiry

A service-created session always has an expiry: the request's TTL or the service default. The framework default is **60 days**; `spring.ai.session.time-to-live` is configurable.

Proposed repo default for a durable demo: **30 days**, set explicitly rather than relying on the framework default. Revisit per feature and data sensitivity. Sessions are **not swept automatically**; schedule `sessionService.deleteExpiredSessions(Instant.now())` (for example once a day) and test the cleanup. In-memory sessions still expire logically, but process restart removes them anyway.

### Conversation size

Use turn-aware compaction, not a raw message count:

- Trigger: `TurnCountTrigger(20)` initially, optionally combined with a token trigger if the chosen model needs a hard budget.
- Strategy: `WindowCompactionStrategy.builder().lastTurns(10).build()` initially.
- Prefer a turn window over `lastEvents(n)` so a tool call is never separated from its result and the newest turn always remains.
- If summarization is added, use a separate summarizer model/options and treat a failed summary as "leave the view unchanged"; the reply must still succeed.
- The event log is append-only. Compaction writes a boundary; it does not delete old events. Only session deletion removes the log.

### System messages

Keep the system prompt out of the conversation by default:

- `spring.ai.session.allow-system-messages=false` (the default).
- Supply the system prompt on every request from code or `Session.metadata`.
- If the feature truly needs a stored system prompt, enable it explicitly, document that the latest stored message wins, and test supersession/pinning.

### Privacy and deletion

Before history is available to real users, add or document:

- A user-initiated delete path mapped to `sessionService.delete(id)` and checked against the principal.
- A retention/cleanup job for expired sessions.
- Log redaction: do not log message content, tool arguments, API keys, or retrieved source text at INFO.
- A policy for tool results and media: they are part of the event log and inherit the session TTL.
- A statement that deleting a session removes its events; there is no tombstone.

## Concurrency

The JDBC repository uses an `event_version` counter and optimistic concurrency:

- A normal append is idempotent for a supplied event ID; a retry is a no-op.
- `appendEvents(sessionId, events, expectedVersion)` appends only if the log version is unchanged; otherwise it returns `false`.
- Compaction uses the same compare-and-swap and can rebase over appends that landed while it was summarizing.
- `findOrCreate` is safe for concurrent first use: the stored session wins.
- `AI_SESSION_EVENT.id` is globally unique in the table; reusing an ID from another session is rejected.

Application rules:

- Do not run an unbounded number of turns against the same session concurrently. Serialize per session in the UI/API, or handle a version conflict by reloading and retrying the turn once.
- Different sessions can run concurrently on any instance with JDBC.
- In-memory sessions are process-local; do not claim multi-instance concurrency for the local demo.
- Add a test for two concurrent appends and one stale compaction; assert no lost or duplicated message and no split turn.

## Migration steps from `ChatMemory`

If the trigger is an existing `ChatMemory` implementation rather than a new feature:

1. **Inventory**: record the conversation ID source, stored message types, tool/media payloads, retention, and whether any users rely on the old rows.
2. **Keep the key**: `ChatMemory.CONVERSATION_ID` and `MemoryAdvisor.CONVERSATION_ID` use the same context key (`chat_memory_conversation_id`), so call sites can keep passing the conversation ID.
3. **Add the session dependency**: `spring-ai-session-advisor` for the advisors and either `InMemorySessionRepository` or `spring-ai-starter-model-session-jdbc` for storage.
4. **Replace the advisor**: `MessageChatMemoryAdvisor.builder(chatMemory)` becomes `SessionAdvisors.builder(sessionService)`. `SessionMemoryAdvisor` handles each model round; `SessionTurnAdvisor` handles the turn and compaction.
5. **Create/find sessions**: use `findOrCreate` or create a session with metadata and TTL before the first turn. Do not rely on an implicit default.
6. **Create the schema**: apply the PostgreSQL/MySQL/H2 script or let the starter initialize it in a disposable environment.
7. **Migrate rows only if needed**: old chat-memory rows are in different tables. Export/import messages into session events deliberately, preserving order and tool-call/result pairs; do not assume a table rename or a property switch migrates data.
8. **Remove the old dependency**: after the new path passes isolation, restart, eviction, and concurrency tests, remove `MessageChatMemoryAdvisor`/`ChatMemoryRepository` before the 3.0 removal.
9. **Update docs and ADR**: record the storage choice, TTL, isolation rule, and the fact that old stores are not read by the new path.

## Test strategy

All tests use mocks/local infrastructure; no vendor call is required. Add tests only with the implementation, not now.

### Multi-turn isolation

- Two different session IDs with the same user question: the second prompt contains only its own history.
- Same session ID across two turns: the second prompt contains the first turn, and the model reply is appended once.
- A caller without a session ID is rejected; no shared default session is created.
- If authentication is present, a different principal cannot read/append the session; if it is absent, the demo's local-only limitation is documented.
- `ai-rag`: old retrieved chunks do not leak into a later turn's prompt as current evidence.

### Eviction and restart

- In-memory: a new application context loses sessions; document/test that behavior rather than pretending it is durable.
- JDBC: a new context/repository against the same database reads the same active view after restart.
- Expiry: create a short-TTL session, call `deleteExpiredSessions` with a deterministic future instant (no sleeps), and assert the session and events are gone.
- Compaction: create more than the trigger's turn count; assert the boundary covers old turns, the newest turn remains, and a tool call/result pair is never split.
- System messages: default rejects stored `SystemMessage`; with `allow-system-messages=true`, only the latest one is pinned and earlier ones are superseded.
- Window: `EventFilter.lastN(n)` extends to a turn start; a filter that hides user messages does not produce a malformed tool turn.

### Concurrent sessions

- Parallel requests with different session IDs create separate logs.
- Parallel first use of the same ID with `findOrCreate` yields one session.
- Two concurrent appends to the same session either both land or one observes a version conflict and retries without duplicating the message.
- A compaction racing an append does not cover the appended turn; the summary rebases or stands down.
- JDBC tests should use the real dialect path (PostgreSQL Testcontainer for `ai-rag`, or H2/Postgres for `ai-agents`) rather than only the in-memory repository. Add `spring-ai-test`'s `AbstractSessionRepositoryContractTests` only if a custom repository is introduced; the built-in repositories already run their contract suite upstream.

### Compatibility

- Existing stateless endpoint tests must remain green while history is added behind an explicit profile/flag.
- The public contract for a feature with history should name the conversation/session ID and the delete path; old clients that omit it must get a clear 400, not a shared history.
- Never add a live provider to the default suite just to test memory.

## Defaults summary

| Decision | Default | Notes |
| --- | --- | --- |
| Migration now? | No | No conversation feature exists. |
| Session scope | One per `(principal, agent, conversation)` | Conversation ID server-generated. |
| Storage | In-memory for local/demo; JDBC for durable | Separate schema/database from PGVector. |
| TTL | 30 days for a durable demo | Explicit; scheduled cleanup; framework default is 60 days. |
| Compaction | 20 turns triggers `lastTurns(10)` | Turn-aware; add token budget only if needed. |
| System messages | Not stored | Re-send from code; `allow-system-messages=false`. |
| Concurrency | One active turn per session; optimistic version conflicts retried once | In-memory is single-process only. |
| Delete | Principal-checked `delete(id)` before real-user history | No tombstone. |
| Test seam | Mocked `ChatModel` + in-memory for logic; JDBC for durability | No vendor calls. |

## GA re-check checklist

This plan used the `2.1.0-RC2` docs because 2.1 GA is not available. Before implementation, re-check at GA:

- [ ] Artifact names: `spring-ai-session`, `spring-ai-session-advisor`, `spring-ai-session-jdbc`, `spring-ai-starter-model-session-jdbc`.
- [ ] Property names/defaults: `spring.ai.session.time-to-live`, `allow-system-messages`, `repository.jdbc.initialize-schema`, `repository.jdbc.schema`, `indexed-attributes`.
- [ ] Whether any additional session stores/dialects shipped for the target database.
- [ ] Removal timeline for `ChatMemory`/`MessageChatMemoryAdvisor` in 3.0 and the final migration mapping.
- [ ] Advisor context keys and the `SessionAdvisors` builder API.
- [ ] The exact schema scripts and whether they changed.
- [ ] Whether the target module's Spring AI/Boot pairing still has the transitive chat-memory auto-configuration described above.

## References

- [Spring AI 2.1 upgrade plan](../upgrades/spring-boot-4.2-spring-ai-2.1.md)
- [Session memory](https://docs.spring.io/spring-ai/reference/2.1/api/session.html)
- [Session concepts](https://docs.spring.io/spring-ai/reference/2.1/api/session/concepts.html)
- [Session ChatClient integration](https://docs.spring.io/spring-ai/reference/2.1/api/session/chat-client.html)
- [JDBC session repository](https://docs.spring.io/spring-ai/reference/2.1/api/session/jdbc.html)
- [Session compaction](https://docs.spring.io/spring-ai/reference/2.1/api/session/compaction.html)
- [Chat memory migration table](https://docs.spring.io/spring-ai/reference/2.1/api/chat-memory.html#chat-memory-migration)
- [ai-agents guide](../../ai-agents/README.md)
- [ai-rag guide](../../ai-rag/README.md)
- [Architecture: persistence and lifecycle](../architecture.md#6-persistence-and-lifecycle)
