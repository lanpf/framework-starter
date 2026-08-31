# framework-starter 内部工程规约

本文只约束 `framework-starter` 工程及其 `framework-starter-*` module 的内部设计；服务工程按需引用 starter。

## 工程职责

`framework-starter` 将 `framework-*` 契约适配到 Spring Boot 和具体技术栈，提供条件化、可替换、最小侵入的自动配置。starter 不定义服务领域模型，也不替服务的 boot module 决定运行时技术组合。

## 模块与依赖

- `framework-starter-*` 生产 module 之间不得相互依赖；每个 starter 只依赖其适配的 `framework-*` 契约和当前技术实现所需的三方依赖。
- 对 `framework-*` 的依赖是 starter 对外能力契约的一部分，使用正常编译依赖；三方技术依赖统一使用 `provided`，由最终引用方决定运行时实现。
- 只有承载具体技术实现时才允许引入对应聚合型 starter，并优先选择能够满足实现的最小依赖。
- `framework-starter-bom` 只管理 starter 版本，不承载运行时代码。
- `framework-starter-tests` 是测试聚合模块，可以按完整场景组合多个 framework 和 starter，不受生产 starter 互不依赖约束。

## 自动配置组织

- 自动配置根类聚合该能力的公共 Bean；由配置项切换的互斥实现按语义拆分为独立或嵌套的 `*Configuration`。
- 模式、类型和算法等分支条件声明在对应配置类上，不将同一分支的条件散落到多个 Bean 方法；相互独立的能力使用独立自动配置类。
- Spring Boot 已自动配置的 `ObjectMapper`、消息模板、任务执行器、事务管理器和数据源等基础设施 Bean 通过注入复用，不在 starter 中重复声明。
- 所有自定义线程池复用 Spring Boot `TaskExecutionProperties.Pool` 属性模型，并优先使用对应 builder；线程名前缀、拒绝策略和调度策略等技术特有参数单独配置。
- 配置开关使用 `boolean`；可选模式使用可空枚举或条件属性，不为“未配置”额外定义 `none` 枚举值。
- Bean 的 condition 只覆盖该 Bean 成立所需的最小条件，并按能力聚合，避免宽泛条件造成部分装配。

## 模块职责

### `framework-starter-bom`

- 提供 `framework-starter` 工程内除自身外的 starter module 版本约束。

### `framework-starter-autoconfigure`

- 提供跨技术的 Spring Boot 通用增强，包括 Jackson、JSR-310 Conversion 和 namespace 解析。
- namespace 未显式配置时使用 `spring.application.name`，仍不可用时使用 `application`。

### `framework-starter-id-cosid`

- 提供基于 CosId 的 `LongIdGenerator` 适配装配。
- CosId 的 JDBC、Zookeeper 等运行时能力由引用方显式选择，starter 不强绑定具体存储。

### `framework-starter-lock-redis`

- 提供 Redis 分布式锁装配，可通过 `framework.lock.redis.provider` 选择 `spring-integration` 或 `redisson`，默认使用 `spring-integration`。
- `LockContext` 只声明 `scene` 和 `key`：`scene` 隔离应用内不同业务动作，`key` 标识具体资源；namespace 用于跨应用隔离，不进入业务调用参数。
- namespace 优先使用 `framework.lock.redis.namespace`，未配置时回退到 `spring.application.name`，仍不可用时使用 `application`。
- 两种 provider 统一生成 `namespace:scene:key`：Spring Integration 将 namespace 作为 `RedisLockRegistry.registryKey` 并透传 `scene:key`，Redisson 显式组合 namespace 与 `scene:key`。
- 两种 provider 的锁数据结构和协议不同，运行期间不得混用或直接切换。
- `framework.lock.redis.lease-time` 统一表示首次或固定 TTL。开启 `auto-renewal` 时，Redisson 使用 watchdog、Spring Integration 使用 renewal scheduler 续期至主动解锁；关闭时两者均使用固定 TTL，到期释放。
- Spring Integration renewal scheduler 使用 `framework.lock.redis.renewal.pool` 配置线程池，线程名前缀和取消任务清理策略分别使用 `renewal.thread-name-prefix`、`renewal.remove-on-cancel-policy`。
- Redisson 通过 `RedissonAutoConfigurationCustomizer` 将 `lockWatchdogTimeout` 对齐为 `lease-time`，并在创建 `LockProvider` 时校验该约束。
- Spring Integration Redis 与 Redisson 是可选技术实现，引用方只引入实际选择的实现及其运行时依赖。

### `framework-starter-persistence-*`

- 将 `framework-persistence` 的通用能力适配到具体持久化技术，例如 `jpa`、`mybatis`、`mybatis-plus`。
- 每种技术自行处理默认表名转换与配置 suffix 的差异，不把技术转换逻辑放入公共契约。

### `framework-starter-domain-eventstore`

- 提供技术中立的 `DomainEventEnvelope`、持久化 repository 契约和默认编排；具体持久化技术在各自实现 module 中定义 DO 和转换 Mapper。
- 原始 `DomainEvent` 不携带事件记录 ID；`DefaultDomainEventStore` 在构造 `DomainEventEnvelope` 时通过 `LongIdGenerator` 的 `domain_event` ID 空间分配趋势递增的 Long ID。
- 领域事件发生时间使用 `Instant` 持久化。
- 默认使用 `DefaultDomainEventStore` 持久化，可通过配置切换为只记录日志的 `NoopDomainEventStore`。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供。

### `framework-starter-message`

- 提供通用消息能力的 Spring Boot 自动配置和默认 `MessageConverter` 装配。

### `framework-starter-message-rabbitmq`

- 提供 RabbitMQ 基础增强，复用 Spring Boot 自动配置的 `RabbitTemplate`、`RabbitMessagingTemplate` 和 listener container factory。
- 生产者可靠性可选择 `transaction` 或 `publisher-confirm`；未配置时保持 Spring Rabbit 原生行为。
- 消费者可靠性仅在显式选择 `transaction` 时增强；未配置时保持 Spring Rabbit 原生行为。
- 分区消息通过 `routing-mode` 区分应用侧 `selector` 与 RabbitMQ `consistent-hash-plugin` 路由；selector 算法独立配置。
- `@PartitionedRabbitListener` 为每个分区 queue 注册 listener endpoint；每个 queue 使用 Single Active Consumer，listener prefetch 由 `framework.rabbitmq.partitioned.listener.prefetch` 配置，默认为 `1`。
- 分区并行度通过增加 queue 数扩展，不通过提高单 queue 的 consumer 并发数破坏分区内顺序。
- 延迟消息使用二进制分层 TTL + DLX，不依赖 RabbitMQ 延迟插件，也不与分区消息组合；配置使用 `framework.rabbitmq.delayed` 前缀。
- `tick-duration` 表示最小延迟精度，实际延迟按 tick 向上取整；`levels` 决定最大 tick 数 `2^levels - 1`。
- 延迟层级队列默认使用 quorum queue 和 at-least-once dead lettering；开启 at-least-once 时必须使用 quorum queue，业务消费者保持幂等。
- 分区与延迟拓扑分别提供 registry 和 declarer，统一由 `RabbitTopologyInitializer` 在单例初始化完成后声明。

### `framework-starter-outbox`

- 提供 integration event outbox 的通用 DO、持久化 repository 契约和 `direct`、`reliable` 两种模式。
- 事件发生时间和 outbox 的创建、发布、失败等状态时间统一使用 `Instant`；状态时间通过可注入的 `Clock` 获取，未提供时使用 UTC 系统时钟。
- 配置统一使用 `framework.outbox.integration-event` 前缀；reliable 模式的 signal、即时重试和 fallback 均以 `batchId` 为单位。
- `direct` 为默认模式，直接调用 publisher，不存储状态；`reliable` 先持久化再异步发布。
- 同一 batch 必须属于同一聚合，入库时记录 `batchId` 和 `batchSequence` 保持批内顺序，数据库状态整批从 `PENDING` 推进到 `PUBLISHING`、`PUBLISHED`。
- 优先使用 `PartitionedOperations`，以 `aggregateType` 作为 destination、`aggregateId` 作为 partition argument；无真实消息端口时使用日志型 no-op publisher。
- reliable 模式的异步线程池复用 `TaskExecutionProperties.Pool` 并独立配置线程名前缀。
- 即时重试配置遵循 Spring Retry 的 `@Retryable` 和 `@Backoff` 语义，支持 retry/no-retry 异常类型和 fixed、uniform-random、exponential、exponential-random 退避。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供，starter 不绑定 JPA、MyBatis 等持久化技术。

### `framework-starter-scheduler-*`

- 提供分布式定时任务运行时装配，例如 `xxljob`。
- namespace 复用公共解析能力；定时任务只承载兜底、补偿和批处理等异步辅助能力，不作为核心实时链路的前置依赖。

### `framework-starter-webmvc`

- 提供 Spring MVC 的 CORS 和统一异常处理自动配置。
- Web 层异常响应复用 `framework-core` 的响应与错误契约，不定义服务业务异常。
- `ClientRequestBodyAdvice` 负责有请求体场景，`ClientRequestArgumentResolver` 负责无请求体的框架具体上下文参数；两者都通过同一个 binder 覆盖受保护 Header。
- binder 始终写入 Client 上下文，并按 `ChannelRequestContext`、`AuthenticatedSessionRequestContext` 能力分别写入渠道和认证会话，因此一个请求可以组合两种上下文。Starter 只完成绑定；参数上的 `@Valid`/`@Validated` 负责校验必填契约。

## 集成测试工程

- `framework-starter-tests` 按能力或完整场景建立独立测试子模块，例如 `framework-starter-outbox-rabbitmq-test`。
- 测试子模块只声明 `test` scope 依赖，不加入 `framework-starter-bom`，不安装或发布测试制品。
- 每个 `*IT` 遵守通用测试规约的自包含、同名文档和手工可观测要求。
- `framework-starter-tests` 不随每次常规 `verify` 重复执行；仅在测试子模块的直接或传递依赖发生变更时执行受影响场景。
