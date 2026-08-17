# Validation and Exception Rules

## Validation

- **JAVA-VALIDATION-001** — Use Jakarta Bean Validation for constraints on bindable Bean properties and method parameters.
- **JAVA-VALIDATION-002** — Use explicit checks such as `Assert` only when framework validation cannot express the constraint or an internal API requires fast failure.

## Exceptions and logging

- **JAVA-EXCEPTION-001** — Use framework exception types for framework, binding, and validation failures; use explicit domain exceptions for invariants and business constraints.
- **JAVA-EXCEPTION-002** — Business exceptions must belong to a defined exception base or error-code hierarchy; generic runtime exceptions must not carry business semantics.
- **JAVA-EXCEPTION-003** — Handle or propagate business exceptions; log non-business exceptions with enough context to locate the problem, and never silently ignore them.
