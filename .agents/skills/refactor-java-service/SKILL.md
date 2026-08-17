---
name: refactor-java-service
description: Plan and apply cross-layer Java service refactors, including standards-driven migrations. Use when a Java service needs coordinated changes across architecture, naming, dependencies, tests, or error codes.
---

# Refactor Java Service

## Workflow

1. Read the project's `AGENTS.md`, root `README.md`, and every authoritative document routed for the affected area.
2. Inspect Git status and record unrelated pre-existing changes before editing.
3. Use `$develop-java-code` for Java code structure, language, and naming changes.
4. Use `$develop-layered-service` for module boundaries, API, domain, application, infrastructure, interfaces, clients, and boot changes.
5. Use `$manage-java-dependencies` when the refactor changes Maven dependencies, module dependencies, versions, scopes, BOMs, starters, or runtime implementations.
6. Use `$manage-service-error-codes` when the refactor changes error definitions, ranges, names, or their documentation.
7. Use `$test-java-service` for test updates, test design, and verification.
8. Produce a plan before modifying code. For every item, identify the triggering rule or requested objective, impacted modules/files, compatibility considerations, implementation approach, and verification commands.
9. After explicit user confirmation, apply only the approved plan, preserve unrelated changes, run the planned verification, and report results. Commit only when separately authorized or when an invoking workflow explicitly authorizes it.
