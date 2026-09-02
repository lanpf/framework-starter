# Project Documentation Rules

## Contents

- [Required documents](#required-documents)
- [Document boundaries](#document-boundaries)
- [Cross-service contracts](#cross-service-contracts)

## Required documents

- **DOCS-STRUCT-001** — Every project must provide a root `README.md` and `docs/RESPONSIBILITIES.md`; business (domain) services must also provide `docs/DOMAIN.md`, and infrastructure projects that own a real domain should provide one too.
- **DOCS-AUTH-DOC-001** — The required documents are a floor, not the full set; key infrastructure capabilities or external dependencies may have separate authoritative documents routed by the project documentation entry, and document names should stay short.

## Document boundaries

- **DOCS-README-001** — `README.md` is the project map answering what the project is, how to build, run, and verify it, where modules live, and where to read more; it must not carry responsibility philosophy or domain rules, and capability delivery status stays as a one-line overview linking to `docs/RESPONSIBILITIES.md`.
- **DOCS-RESPONSIBILITIES-001** — `docs/RESPONSIBILITIES.md` is the service charter answering what the service owns, what it does not own (each entry naming its owner), which collaboration contracts it honors, and what has been delivered; it is the single home for cross-service collaboration contracts.
- **DOCS-DOMAIN-001** — `docs/DOMAIN.md` is the domain design answering how the domain is modeled, carrying ubiquitous language, models, invariants, use cases, domain events and errors, and layering; it must not carry deployment or runtime configuration, delivery status, or other services' responsibilities.

## Cross-service contracts

- **DOCS-CROSS-SERVICE-001** — Each side of a cross-service contract writes only its own half in its own `docs/RESPONSIBILITIES.md` and links to the other side: the caller owns the end-to-end flow description, the callee owns the endpoint contract and its consumption semantics; never maintain the same contract table or flow in two repositories.
