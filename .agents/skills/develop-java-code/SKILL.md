---
name: develop-java-code
description: Use when creating, modifying, refactoring, or reviewing Java code, including records, collections, validation, exceptions, Lombok, MapStruct, Spring beans, entities, and value objects.
---

# Develop Java Code

## Workflow

1. Inspect the target module, nearby types, and its existing public contracts.
2. Read `references/java-language.md` for every Java implementation or review.
3. Read `references/validation-exceptions.md` when changing inputs, validation, failures, or logging.
4. Read `references/lombok.md` before adding or changing Lombok annotations.
5. Read `references/mapstruct.md` before creating or modifying object conversion.
6. Preserve compatibility unless the task explicitly authorizes a breaking change.
7. Compile and run the narrowest relevant tests.

Prefer explicit, readable business logic. Treat every `required` rule as a release-blocking constraint.

