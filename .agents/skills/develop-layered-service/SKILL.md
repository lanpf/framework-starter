---
name: develop-layered-service
description: Use when designing, implementing, refactoring, or reviewing a layered Spring Boot service, including API, domain, application, infrastructure, interfaces, OpenFeign client, boot, persistence, messaging, scheduling, events, and naming.
---

# Develop Layered Service

## Workflow

1. Read the service's project documentation entry, then follow its routing to the authoritative domain document and any other documents relevant to the task; also inspect module POMs and nearby implementations.
2. Read `references/architecture.md` before creating modules, changing dependencies, or reviewing boundaries.
3. Read `references/api-domain-application.md` when changing API contracts, domain models, repositories, events, commands, queries, or use cases.
4. Read `references/infrastructure-interfaces-boot.md` when changing persistence, adapters, configuration, protocols, clients, scheduling, messaging, or assembly.
5. Read `references/data-carriers.md` before selecting `record` versus ordinary classes across layers.
6. Read `references/naming.md` before introducing or renaming service types.
7. Keep rules in domain, orchestration in application, technology in infrastructure, protocols in interfaces, and assembly in boot.
8. Verify dependency direction and run affected tests and builds.
