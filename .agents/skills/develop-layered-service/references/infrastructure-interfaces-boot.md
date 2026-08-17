# Infrastructure, Interfaces, Client, and Boot Rules

## Contents

- [Infrastructure and configuration](#infrastructure-and-configuration)
- [Persistence and asynchronous adapters](#persistence-and-asynchronous-adapters)
- [Interfaces, client, and boot](#interfaces,-client,-and-boot)

## Infrastructure and configuration

- **INFRA-RESPONSIBILITY-001** — Infrastructure provides repository, gateway, ID, event storage, messaging, scheduling, and other technical adapters without domain rules or use-case orchestration.
- **INFRA-CONFIG-001** — For JavaBean configuration binding, collection and nested-object fields are final, expose getters without setters, and begin as empty binding containers without business defaults.
- **INFRA-CONFIG-002** — Required configuration collections use `@NotEmpty` so missing configuration fails startup rather than running silently with an empty value.
- **INFRA-DURATION-001** — A standalone minimum for `Duration` uses both `@NotNull` and Hibernate Validator `@DurationMin` with explicit units; the module declares `hibernate-validator` directly.
- **INFRA-CROSS-FIELD-001** — Use `@AssertTrue` and similar type-level validation only for relationships between fields.

## Persistence and asynchronous adapters

- **INFRA-REPOSITORY-001** — Repository adapters implement domain repositories or application ports; persistence repositories expose data-access capability only.
- **INFRA-PERSISTENCE-001** — Keep concrete repository implementations, SQL XML, and technical assembly in the matching persistence implementation module; the packaged dependency determines the runtime implementation.
- **INFRA-DO-001** — Shared persistence models live in infrastructure; DOs declare only necessary entity, identifier, and enum mapping annotations, while table constraints and indexes live in schema SQL.
- **INFRA-MAPPER-001** — Mappers convert domain objects, application read models, and persistence objects; generic helpers remain mapper-only and technology-neutral.
- **INFRA-QUERY-001** — Name persistence queries by conditions, range, and ordering rather than by a single caller or use case.
- **INFRA-ASYNC-001** — Domain event storage, outbox, and partitioned messaging follow framework ports; scheduler modules expose task entries while compensation logic remains in shared infrastructure services.

## Interfaces, client, and boot

- **INTERFACES-RESPONSIBILITY-001** — Interfaces provides protocol-neutral Facade implementations and REST, RPC, and message entry points without domain rules or use-case orchestration.
- **INTERFACES-FACADE-001** — Facade implementations convert API/application objects, call application services, and wrap responses with `Result<T>` or `PageResult<T>`.
- **INTERFACES-REST-001** — Controllers handle routing, binding, and necessary protocol context only, convert the HTTP request into an API command/query, then invoke a Facade contract.
- **INTERFACES-HEADER-CONTEXT-001** — Inject or resolve Client, Channel, Device, and other HTTP header context only in interfaces; API, application, and domain must not depend on the HTTP header binding mechanism.
- **INTERFACES-REST-CONTEXT-001** — After the gateway supplies client context, each controller business request is one mutable `@Valid` HTTP request class extending `ClientRequest`, or `ClientChannelRequest` only when channel context is required; `ClientRequest` requires `clientAppId`, while `ClientChannelRequest` additionally requires `channelCode`. Do not split business fields into path or query parameters alongside a separate client-context argument. For endpoints without a business body, declare the concrete request-context type and let the shared argument resolver build and validate it from protected headers.
- **INTERFACES-FACADE-BOUNDARY-001** — After header injection and validation, interfaces converts its mutable HTTP request into a complete immutable API command/query record; Facades must not accept interfaces HTTP request types.
- **INTERFACES-RPC-001** — Publish the same Facade Bean directly when the RPC technology supports it; add a technology-specific RPC adapter only for incompatible models, semantics, metadata, or exception handling.
- **INTERFACES-MESSAGE-001** — Message listeners convert inbound contracts and invoke application services without being required to pass through a Facade.
- **CLIENT-FEIGN-001** — OpenFeign clients match Facade method signatures, reuse API contract types and response wrappers, and contain no business model, use case, or conversion rules.
- **BOOT-RESPONSIBILITY-001** — Boot owns startup, runtime configuration, implementation selection, and packaging; absent implementation-module dependencies mean the capability is unavailable at runtime.
- **BOOT-BOUNDARY-001** — Boot contains no domain rules, application orchestration, protocol adapters, or business types.
