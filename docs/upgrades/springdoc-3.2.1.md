# springdoc 3.2.1 upgrade

**Status:** implemented in PR pending review. The parent now pins springdoc `3.2.1`; Spring Boot remains `4.2.0` and Spring AI remains `2.1.0`.

Tracking issues: [#73](https://github.com/prasadgaikwad/spring-ai-apps/issues/73) and [#76](https://github.com/prasadgaikwad/spring-ai-apps/issues/76).

## Why this bump

springdoc `3.2.1` is built for the Boot `4.2.x` line and fixes three DOMPurify advisories bundled with Swagger UI by upgrading to swagger-ui `5.35.1` / DOMPurify `3.4.17` (GHSA-cg2g-xh64-v4q7, GHSA-mrv2-3pqr-j5jr, GHSA-c6gr-qvv4-5v3f). It is not a drop-in patch: springdoc now runs on swagger-core `3.0.0`, whose mappers are Jackson 3 (`tools.jackson`), and the retired `*-jakarta` Swagger artifacts were renamed.

## Decisions

### Application JSON mapping stays on Jackson 2, but is now explicit

Before this upgrade, `ai-decision` and `ai-observability` used `com.fasterxml.jackson.databind` through a transitive dependency from springdoc `3.2.0`. springdoc `3.2.1` no longer supplies Jackson 2, so both modules now declare `jackson-databind` and `jackson-datatype-jsr310` directly. Versions remain Boot-managed (`2.21.8` at this writing).

We did not migrate the application's Jev payload mapping or DuckDB tag serialization to Jackson 3. Those paths are isolated from Spring MVC's Jackson 3 mappers, and Boot 4.2 still manages Jackson 2 for compatibility. This keeps the behavioral surface small; a dedicated Jackson 3 migration would need its own schema/date-time tests. springdoc and swagger-core use Jackson 3 independently for OpenAPI generation.

### Swagger artifacts align on 3.0.0

The root POM now pins `io.swagger.core.v3:swagger-annotations` to `3.0.0`, matching springdoc's swagger-core `3.0.0` and swagger-models `3.0.0`. The retired `swagger-annotations-jakarta` artifact is excluded centrally in root `dependencyManagement` for both `spring-ai-model` and `spring-ai-client-chat`. The latter is still pulled transitively by the six chat demos' Ollama/OpenAI/Anthropic starters; the managed exclusion removes it without adding new direct dependencies to those modules.

`ai-decision` has no chat starters, so its Swagger annotations come only from springdoc. `ai-common` has no springdoc dependency, so the managed exclusion keeps the retired annotation artifact off the shared model API classpath.

## Verification

- `./mvnw -B -DskipTests compile` passed with Enforcer dependency convergence; no suppressions.
- `./mvnw -B test -DexcludedGroups=docker -Dsurefire.failIfNoSpecifiedTests=false` passed 176 tests. App-context tests in all seven REST modules still assert `/v3/api-docs` and `/swagger-ui/index.html`.
- `dependency:tree -Dincludes=io.swagger.core.v3:*` shows only `swagger-annotations`, `swagger-models`, and `swagger-core` at `3.0.0`; no `swagger-annotations-jakarta` remains on any module classpath.
- No live OpenAI, Anthropic, Ollama, Jev, or MCP peer calls were made.

## Out of scope

REST contracts, controller annotations, provider behavior, the routing `security` profile, and a full application Jackson 3 migration are unchanged. `json-schema-validator` (#75) and Boot-managed Logback/Jackson patches (#77) remain separate tracks.

## Sources

- [springdoc-openapi v3.2.1 release notes](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.1)
- [Swagger Core 3.X migration guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-3.X---Migration-from-2.X)
- [Jackson 3 migration guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md)
