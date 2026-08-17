<!-- engineering-standards:begin version=1.2.3 -->
## Shared engineering guidance

- **STD-HIERARCHY-001** — Shared rules apply to every project; service rules additionally apply to service projects, and project-specific authoritative documents may only add or tighten constraints.
- **STD-DOCS-001** — Before changing a service, read the project documentation entry declared by its project guidance, then read every authoritative document routed for the task.
- **STD-DOMAIN-002** — Before changing domain boundaries, language, business rules, errors, domain events, or API business semantics, read the service's authoritative domain document.
- **JAVA-VERSION-001** — Use Java 17.
- **DEP-CYCLE-001** — Module dependency cycles are forbidden; fix responsibilities or extract a shared module rather than hiding a cycle through reflection or events.
- **ARCH-DIRECTION-001** — Inner modules must not depend on outer protocols, persistence implementations, persistence objects, or the boot module.
- **ARCH-RESPONSIBILITY-001** — Keep domain rules in domain, use-case orchestration in application, technical adapters in infrastructure, protocol handling in interfaces, and runtime assembly in boot.
- **ERROR-STABILITY-001** — Never modify, reuse, or reassign a published error code.
- **VERIFY-CHANGE-001** — Run verification proportional to the change and do not claim completion without reporting the commands and results.

## Skill routing

- Creating, modifying, refactoring, or reviewing Java code: use `$develop-java-code`.
- Changing Maven dependencies, versions, scopes, BOMs, starters, modules, or runtime implementations: use `$manage-java-dependencies`.
- Creating, modifying, reviewing, or diagnosing unit, contract, or integration tests: use `$test-java-service`.
- Adding service functionality, modules, contracts, domain behavior, adapters, protocols, or reviewing service architecture: use `$develop-layered-service`.
- Allocating, changing, documenting, or reviewing service error codes: use `$manage-service-error-codes`.
- Planning or applying cross-layer Java service refactors, including changes driven by updated engineering standards: use `$refactor-java-service`.

<!-- engineering-standards:end -->
