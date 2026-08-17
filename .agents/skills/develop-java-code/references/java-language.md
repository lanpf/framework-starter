# Java Language Rules

## Contents

- [Language and data design](#language-and-data-design)
- [Readable modern Java](#readable-modern-java)
- [Utilities](#utilities)

## Language and data design

- **JAVA-VERSION-002** — Compile and run with Java 17.
- **JAVA-RECORD-001** — Prefer `record` for data carriers whose state is complete at construction, remains immutable, and does not require inheritance, proxies, or JavaBean setter binding.
- **JAVA-RECORD-002** — Defensively copy collection record components with `List.copyOf`, `Set.copyOf`, or the corresponding immutable-copy operation.
- **JAVA-CLASS-001** — Use an ordinary class when the type requires inheritance, mutable state, framework proxying, JavaBean shape, or complex domain behavior.
- **JAVA-TYPE-001** — Prefer wrapper types such as `Integer`, `Long`, and `Boolean`; use primitives only for intentional default values, performance characteristics, or explicitly non-null semantics.

## Readable modern Java

- **JAVA-STYLE-001** — Prefer `forEach` for simple collection traversal and Stream for simple transformation, filtering, and mapping.
- **JAVA-STYLE-002** — Prefer lambdas and method references for simple callbacks.
- **JAVA-OPTIONAL-001** — Use `Optional` for possibly absent return values and chained handling; do not use it as a Bean property or method parameter.
- **JAVA-READABILITY-001** — For complex business rules, nested conditions, and exception flows, prioritize readability over functional style.

## Utilities

- **JAVA-UTILITY-001** — Prefer the JDK, an already-used framework, or a mature maintained library over reimplementing general technical capabilities.
- **JAVA-UTILITY-002** — When Spring Framework is already present, prefer its string and collection utilities for common null, string, and collection checks.
- **JAVA-UTILITY-003** — Before adding a library, evaluate maintenance activity, security, license, and dependency cost; do not add overlapping functionality when existing dependencies suffice.
