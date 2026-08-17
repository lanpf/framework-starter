# Lombok Rules

## General use

- **LOMBOK-EXPERIMENTAL-001** — Do not use annotations from `lombok.experimental`.
- **LOMBOK-BUILDER-001** — Do not use `@Builder`; use explicit constructors or factory methods for staged or fluent construction.
- **LOMBOK-CONSTRUCTOR-001** — Use Lombok constructors only for direct field assignment without validation or conversion; write an explicit constructor when initialization contains invariants, transformations, or other logic.
- **LOMBOK-LOG-001** — Use `@Slf4j` instead of declaring a Logger field manually.

## Type-specific use

- **LOMBOK-ENTITY-001** — Aggregate roots and entities use only `@Getter`; state changes go through domain behavior, and equality based on the unique identifier is implemented explicitly without `@Data` or Lombok `@EqualsAndHashCode`.
- **LOMBOK-VALUE-001** — Prefer `record` for value objects; otherwise use final fields, getters, an appropriate all-arguments constructor, and full-field equality when suitable.
- **LOMBOK-SPRING-001** — Spring Beans use final dependencies and constructor injection, normally through `@RequiredArgsConstructor`.
