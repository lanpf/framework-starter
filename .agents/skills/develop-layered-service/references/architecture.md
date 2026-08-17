# Service Architecture Rules

## Contents

- [Service domain contract](#service-domain-contract)
- [Modules](#modules)
- [Dependency direction](#dependency-direction)

## Service domain contract

- **SERVICE-DOC-ENTRY-001** — Every service declares a project documentation entry that summarizes the project and routes tasks to authoritative documents.
- **SERVICE-DOMAIN-DOC-003** — Every service keeps an authoritative domain document defining its bounded context, domain language, business rules, error definitions, domain events, and API semantics; its path is declared by the project documentation entry and project guidance.
- **SERVICE-DOMAIN-DOC-002** — Project domain guidance may add or tighten shared rules but must not duplicate, relax, or override them.

## Modules

- **ARCH-MODULE-API-001** — `<service>-api` defines stable external API contracts.
- **ARCH-MODULE-DOMAIN-001** — `<service>-domain` owns the core domain model and rules.
- **ARCH-MODULE-APPLICATION-001** — `<service>-application` orchestrates write use cases and read-only queries.
- **ARCH-MODULE-INFRA-001** — `<service>-infrastructure` contains technical adapters, persistence abstractions, shared conversions, and persistence objects.
- **ARCH-MODULE-TECH-001** — Concrete persistence, scheduler, and message implementations live in `<service>-infrastructure-<kind>-<technology>` modules.
- **ARCH-MODULE-INTERFACES-001** — `<service>-interfaces` implements protocol-neutral Facades and hosts REST, RPC, and message subscription adapters.
- **ARCH-MODULE-CLIENT-001** — `<service>-openfeign-client` provides the OpenFeign client for the service API.
- **ARCH-MODULE-BOOT-001** — `<service>-boot` starts, configures, packages, and assembles runtime implementations.

## Dependency direction

- **ARCH-DEP-API-001** — API does not depend on business implementations; domain does not depend on other business layers; application depends on domain.
- **ARCH-DEP-INFRA-001** — Infrastructure implements domain and application ports; concrete persistence, scheduler, and message modules depend on infrastructure.
- **ARCH-DEP-OUTER-001** — Interfaces depends on API and application, OpenFeign client depends on API, and boot performs final assembly.
- **ARCH-DEP-INWARD-001** — No inner module may depend on outer protocol types, persistence objects, concrete technologies, or boot.
