---
name: manage-java-dependencies
description: Use when adding, removing, upgrading, or reviewing Maven dependencies, dependency scopes, BOMs, starters, modules, framework versions, runtime adapters, or dependency conflicts.
---

# Manage Java Dependencies

## Workflow

1. Inspect the parent POM, dependency management, the target module, and the dependency tree.
2. Read `references/dependency-rules.md` for every dependency or module relationship change.
3. For a service framework, starter, persistence, messaging, or scheduling change, also read `references/service-baseline.md`.
4. Prefer the smallest already-managed artifact that provides the required capability.
5. Reject cycles, hidden version overrides, incorrect scopes, and speculative dependencies.
6. Run the dependency tree check and the affected module build.

