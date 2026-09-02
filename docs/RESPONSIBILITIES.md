# framework-starter

本文是 `framework-starter` 工程的项目文档入口，按 module 描述当前能力、装配方式与依赖边界；服务工程按需引用 starter。修改任何 `framework-starter-*` module 前先阅读本文。

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

## 持久化信封模式

`domain-eventstore` 与 `outbox` 采用同构的技术中立持久化结构，两个 starter 均不携带持久化技术注解：

- starter 内提供 `*Envelope` 持久化模型（record）、`*EnvelopePersistenceRepository`、`*EnvelopePersistenceMapper` 契约和默认编排；`OutboxStatus` 等状态枚举保持技术中立。
- 转换契约声明在 `persistence` 包；MapStruct 实现放在契约包的 `mapstruct` 子包，命名 `*MapStructMapper`，通过 `uses` 引入同子包的 `*PayloadConverter` 完成 payload 序列化与反序列化。
- `persistence/*EnvelopeConfiguration` 统一声明 converter 与 mapper Bean（`Mappers.getMapper` 实例化生成实现，`@ConditionalOnMissingBean` 允许替换），由能力配置类 `@Import`。
- payload 序列化/反序列化失败抛 `FrameworkException(FrameworkError.SERIALIZATION_FAILED)`；`ObjectMapper` 复用 Spring Boot 自动配置的实例。
- 技术注解 DO、DO↔Envelope 转换 mapper 与 repository 实现由服务或独立技术适配 module 提供（参考 `framework-starter-outbox-jpa-rabbitmq-test` 的 JPA 实现）。

## 模块能力

### `framework-starter-bom`

- 管理工程内除自身外的全部 starter module 版本约束，不承载运行时代码。

### `framework-starter-dependencies`

- 聚合导入 `framework-starter-bom`，作为服务工程统一引入的版本管理入口。

### `framework-starter-autoconfigure`

- 提供跨技术的 Spring Boot 通用增强：自适应多格式日期时间反序列化与 `spring.jackson.module.java-time.*` 格式配置、JSR-310 Conversion 注册、namespace 解析。
- `NamespaceAutoConfiguration` 装配 `NamespaceResolver`：namespace 未显式配置时使用 `spring.application.name`，仍不可用时使用 `application`。
- 提供 `AbstractKeyResolver` 基类，业务场景资源 Key 解析统一委托注入的 `ResourceNameResolver`。

### `framework-starter-id-cosid`

- 存在 CosId `IdGeneratorProvider` 时装配 `LongIdGenerator`（`CosIdLongIdGenerator`）；CosId 的 JDBC、Zookeeper 等运行时能力由引用方显式选择，starter 不强绑定具体存储。

### `framework-starter-lock-redis`

- Redis 分布式锁装配，`framework.lock.redis.provider` 选择 `spring-integration`（默认）或 `redisson`。
- `LockContext` 只声明 `scene` 和 `key`；两种 provider 统一生成 `namespace:scene:key`，namespace 优先 `framework.lock.redis.namespace`，回退 `spring.application.name`、`application`。
- `lease-time` 统一表示首次或固定 TTL；开启 `auto-renewal` 时 Redisson 使用 watchdog、Spring Integration 使用 renewal scheduler（`renewal.pool`、线程名前缀、remove-on-cancel 独立配置）；关闭时两者均使用固定 TTL 到期释放。
- Redisson 通过 `RedissonAutoConfigurationCustomizer` 将 `lockWatchdogTimeout` 对齐 `lease-time` 并在装配时校验；两种 provider 锁数据结构不同，运行期不得混用。

### `framework-starter-domain-eventstore`

- 提供技术中立的 `DomainEventEnvelope`、持久化 repository/mapper 契约和默认编排。
- `domain-event.store.mode` 选择 `default`（默认，`DefaultDomainEventStore` 持久化）或 `noop`（只记录日志）。
- default 编排在构造信封时通过 `LongIdGenerator` 的 `domain_event` ID 空间分配趋势递增的 Long ID；事件发生时间使用 `Instant` 持久化。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供。

### `framework-starter-message`

- 装配名为 `MessageConverterNames.DEFAULT` 的复合 `MessageConverter`（ByteArray、String、Jackson、可选 FastJSON），Jackson 转换器复用容器 `ObjectMapper`。
- 通过 `BeanPostProcessor` 将默认 converter 注入 Spring 的 `DefaultMessageHandlerMethodFactory`，可选 FastJSON 注册失败仅记录 WARN 不阻断启动。

### `framework-starter-message-rabbitmq`

- RabbitMQ 基础增强，复用 Spring Boot 自动配置的 `RabbitTemplate`、`RabbitMessagingTemplate` 和 listener container factory。
- 生产者可靠性 `framework.rabbitmq.producer.reliability-mode` 选择 `transaction` 或 `publisher-confirm`，未配置时保持 Spring Rabbit 原生行为；publisher-confirm 支持单条与批量等待确认，`confirm-timeout` 控制等待上限。
- 消费者可靠性仅在显式选择 `transaction` 时增强，未配置保持原生行为。
- 分区消息通过 `routing-mode` 区分应用侧 `selector`（`hash` 默认 / `consistent-hash`，后者需要 Guava，缺失即启动失败）与 RabbitMQ `consistent-hash-plugin` 路由。
- `@PartitionedRabbitListener` 为每个分区 queue 注册 listener endpoint，每个 queue 使用 Single Active Consumer；单 queue 消费者并发固定为 1，prefetch 由 `framework.rabbitmq.partitioned.listener.prefetch` 配置；`simple` 与 `direct` container 均支持；dead-letter 开启时被拒消息不重回队列。
- 延迟消息使用二进制分层 TTL + DLX，不依赖延迟插件，也不与分区消息组合；`tick-duration` 为最小延迟精度，`levels` 决定最大 tick 数；默认 quorum queue + at-least-once dead lettering，业务消费者保持幂等。
- 分区与延迟拓扑分别提供 registry 和 declarer，统一由 `RabbitTopologyInitializer` 在单例初始化完成后声明。

### `framework-starter-outbox`

- 提供技术中立的 `IntegrationEventOutboxEnvelope`、持久化 repository/mapper 契约和 `direct`、`reliable` 两种模式。
- `framework.outbox.integration-event.mode`：`direct` 为默认，直接调用 publisher 不存储状态；`reliable` 先持久化再异步发布。
- reliable 模式：本地事务提交后发布 signal，由独立线程池 + Spring Retry 即时重试（支持 retry/no-retry 异常类型与 fixed、uniform-random、exponential、exponential-random 退避），fallback 扫描兜底；signal、即时重试和 fallback 均以 `batchId` 为单位。
- 同一 batch 必须属于同一聚合，入库时记录 `batchId` 和 `batchSequence` 保持批内顺序，数据库状态整批从 `PENDING` 推进到 `PUBLISHING`、`PUBLISHED`。
- 发布端口优先使用 `PartitionedOperations`，以 `aggregateType` 作为 destination、`aggregateId` 作为 partition argument；无真实消息端口时使用日志型 no-op publisher。
- 事件发生时间和 outbox 的创建、发布、失败等状态时间统一使用 `Instant`，通过可注入的 `Clock` 获取，未提供时使用 UTC 系统时钟；异步线程池复用 `TaskExecutionProperties.Pool` 并独立配置线程名前缀。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供，starter 不绑定 JPA、MyBatis 等持久化技术。

### `framework-starter-persistence-jpa`

- 通过 Hibernate `PersistencePhysicalNamingStrategy` 应用 `framework.persistence.naming` 的表名前缀/后缀。

### `framework-starter-persistence-mybatisplus`

- 装配 `MybatisPlusInterceptor`：分页拦截器按 `framework.persistence.database` 显式指定数据库类型，不支持的类型启动即失败；动态表名拦截器应用与 JPA 一致的表名前缀/后缀。
- `InnerInterceptor` Bean 按顺序注入拦截器链，应用可追加自定义拦截器。

### `framework-starter-scheduler-xxljob`

- 装配 XXL-JOB executor，`scheduler.xxljob.enabled` 控制开关，开启时校验 `admin-addresses` 必填；executor 的 appname 复用 namespace 公共解析能力。
- 定时任务只承载兜底、补偿和批处理等异步辅助能力，不作为核心实时链路的前置依赖。

### `framework-starter-webmvc`

- 提供 Spring MVC 的 CORS（`framework.webmvc.cors`）和统一异常处理自动配置。
- `ClientRequestBodyAdvice` 负责有请求体场景，`ClientRequestArgumentResolver` 负责无请求体的框架具体上下文参数；两者通过同一个 binder 覆盖受保护 Header，按 `ChannelContext`、`AuthenticatedSessionContext` 能力组合写入渠道与认证会话。starter 只完成绑定；参数上的 `@Valid`/`@Validated` 负责校验必填契约。
- Web 层异常响应复用 `framework-core` 的响应与错误契约：`BaseException` 按错误码返回，未分类异常记录 ERROR 并返回框架标准错误码，不透出内部异常消息。

## 集成测试工程

- `framework-starter-tests` 按能力或完整场景建立独立测试子模块，例如 `framework-starter-outbox-rabbitmq-test`。
- 测试子模块只声明 `test` scope 依赖，不加入 `framework-starter-bom`，不安装或发布测试制品。
- 每个 `*IT` 遵守通用测试规约的自包含、同名文档和手工可观测要求。
- `framework-starter-tests` 不随每次常规 `verify` 重复执行；仅在测试子模块的直接或传递依赖发生变更时执行受影响场景。
