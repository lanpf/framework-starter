---
name: test-java-service
description: Use when creating, modifying, reviewing, or diagnosing Java unit tests, module contract tests, integration tests, Testcontainers infrastructure, test documentation, or Maven test lifecycle configuration.
---

# Test Java Service

## Workflow

1. Classify the scenario as a unit, module contract, or integration test.
2. Read `references/testing.md` completely before editing test code or lifecycle configuration.
3. Test observable behavior and public contracts; mock only true external collaborators.
4. Keep infrastructure-backed scenarios isolated, repeatable, and manually observable.
5. Run Surefire or Failsafe through the lifecycle phase required by the scenario.
6. Confirm resources are released and integration-test documentation is complete.

