# Java Testing Rules

## Contents

- [Test boundaries](#test-boundaries)
- [Structure and lifecycle](#structure-and-lifecycle)
- [Documentation and observation](#documentation-and-observation)

## Test boundaries

- **TEST-LOCATION-001** — Keep unit and module contract tests in the tested module's `src/test`.
- **TEST-INTEGRATION-001** — Treat cross-module, full auto-configuration, real database, or real middleware scenarios as integration tests and keep them out of production modules.
- **TEST-BEHAVIOR-001** — Verify observable behavior and public contracts rather than unnecessary implementation details.
- **TEST-MOCK-001** — In unit tests, mock only injected external collaborators; do not partially mock or spy on the subject under test.
- **TEST-CONTAINERS-001** — Prefer Testcontainers for isolated and repeatable real-infrastructure scenarios.

## Structure and lifecycle

- **TEST-SELF-CONTAINED-001** — Keep integration scenarios self-contained; place containers, test application, configuration, probes, and lifecycle management in the corresponding `*IT` when practical.
- **TEST-REUSE-001** — Extract test support only for stable and meaningful reuse; do not create a common test base merely to remove small duplication.
- **TEST-UNIT-NAME-001** — Name unit and module contract test classes `*Test` and execute them with Maven Surefire in the test phase.
- **TEST-IT-NAME-001** — Name integration test classes `*IT` and execute them with Maven Failsafe in the integration-test and verify phases.
- **TEST-METHOD-NAME-001** — Name test methods `should<ExpectedBehavior>` to state the observable behavior directly.
- **TEST-CLEANUP-001** — Manage external infrastructure with `@Testcontainers` and `@Container` where possible, and release created application contexts and executors in `@AfterEach`.

## Documentation and observation

- **TEST-DOC-001** — Every integration test has same-named documentation covering its purpose, conditions, execution, manual observation steps, observation entry points, and expected result.
- **TEST-DOC-002** — The root README of a standalone test module contains only coverage scope, environment requirements, unified execution, report location, and a documentation index; detailed scenarios live in `docs/<TestClass>.md`.
- **TEST-OBSERVABLE-001** — Integration tests must allow manual observation of the key process and final result; automatic assertions or source reading alone are insufficient.
