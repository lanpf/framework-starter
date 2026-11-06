# framework-starter

本文是 `framework-starter` 工程的项目文档入口，维护对外能力、协作契约、保证与限制；服务工程按需引用 starter。修改任何 `framework-starter-*` module 前先阅读本文，涉及内部依赖、配置绑定或装配机制时继续阅读 [DESIGN.md](DESIGN.md)。

## 工程职责

`framework-starter` 将 `framework-*` 契约适配到 Spring Boot 和具体技术栈，提供条件化、可替换、最小侵入的自动配置。

## 不负责

- 服务领域模型与业务规则：由各业务服务工程负责。
- 运行时技术组合选择：由服务工程的 boot module 装配负责，starter 只提供条件化能力。
- 具体持久化技术的 DO、DO 转换 mapper 与 repository 实现：由服务或独立技术适配 module 提供。

## 协作契约

- 本工程消费 `framework-*` 契约完成适配：契约语义、稳定性与变更节奏以 framework 工程为准，需要新契约或契约变更时在两侧同步更新；对应内容见 [framework 职责文档](../../framework/docs/RESPONSIBILITIES.md)，此处不复制。

## 模块能力

### `framework-starter-bom`

- 管理工程内除自身外的全部 starter module 版本约束，不承载运行时代码。

### `framework-starter-dependencies`

- 聚合导入 `framework-starter-bom`，作为服务工程统一引入的版本管理入口。

### `framework-starter-autoconfigure`

- 提供日期时间格式转换、`spring.jackson.module.java-time.*` 格式配置与 namespace 解析。
- namespace 未显式配置时使用 `spring.application.name`，仍不可用时使用 `application`。
- 提供统一资源 Key 解析能力，使用方传入业务键段，由命名解析能力确定最终资源名。

### `framework-starter-id-cosid`

- 提供 CosId 发号适配；使用方须提供发号配置及 provider，缺失 generator name 时快速失败。JDBC、Zookeeper 等运行时能力由引用方显式选择，starter 不强绑定具体存储。

### `framework-starter-lock-redis`

- Redis 分布式锁装配，`framework.lock.redis.provider` 选择 `spring-integration`（默认）或 `redisson`。
- 使用方提供等待时间和业务锁键段；两种 provider 均规范化键段并只注入一次 namespace。namespace 优先 `framework.lock.redis.namespace`，回退 `spring.application.name`、`application`。
- `lease-time` 统一表示首次或固定 TTL；开启 `auto-renewal` 时两种 provider 均支持续期，关闭时均使用固定 TTL 到期释放。
- Redisson watchdog 超时须与 `lease-time` 一致，启动时校验；两种 provider 锁数据结构不同，运行期不得混用。
- `LockExecutor` 只管理锁内执行边界：锁保护数据库工作时，由调用方先获取锁、再开启事务，事务提交或回滚完成后才释放锁；方法返回不代表事务完成，存在外层事务或代理时需自行验证真实的事务边界。

### `framework-starter-domain-eventstore`

- 提供技术中立的 `DomainEventEnvelope`、持久化 repository/mapper 契约和默认编排。
- `domain-event.store.mode` 选择 `default`（默认，`DefaultDomainEventStore` 持久化）或 `noop`（只记录日志）。
- default 模式在写入时通过 `domain_event` ID 空间分配全局唯一、趋势递增的持久化记录标识；事件发生时间保留绝对时间语义。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供。
- noop 模式面向开发联调与无存储降级场景，逐事件 INFO 是其可观测性行为；生产高频场景应选择 default 实现或自行替换 Store。

### `framework-starter-message`

- 提供 ByteArray、String、Jackson 与可选 FastJSON 消息转换能力，沿用使用方的 Jackson 配置。
- 可选 FastJSON 注册失败仅记录 WARN，不阻断启动。

### `framework-starter-message-rabbitmq`

- 提供 RabbitMQ 消息发送、消费、分区与延迟能力，沿用使用方的连接和基础设施配置。
- 生产者可靠性 `framework.rabbitmq.producer.reliability-mode` 选择 `transaction` 或 `publisher-confirm`，未配置时保持 Spring Rabbit 原生行为；publisher-confirm 支持单条与批量等待确认，`confirm-timeout` 控制等待上限。
- 消费者可靠性仅在显式选择 `transaction` 时增强，未配置保持原生行为。
- 分区消息通过 `routing-mode` 区分应用侧 `selector`（`hash` 默认 / `consistent-hash`，后者需要 Guava，缺失即启动失败）与 RabbitMQ `consistent-hash-plugin` 路由。
- 分区消费每个 queue 使用 Single Active Consumer；单 queue 消费者并发固定为 1，prefetch 由 `framework.rabbitmq.partitioned.listener.prefetch` 配置；支持 `simple` 与 `direct` container，dead-letter 开启时被拒消息不重回队列。
- 延迟消息使用二进制分层 TTL + DLX，不依赖延迟插件，也不与分区消息组合；`tick-duration` 为最小延迟精度，`levels` 决定最大 tick 数；默认 quorum queue + at-least-once dead lettering，业务消费者保持幂等。
- 分区与延迟拓扑在启动完成初始化时声明。

### `framework-starter-outbox`

- 提供技术中立的 `IntegrationEventOutboxEnvelope`、持久化 repository/mapper 契约和 `direct`、`reliable` 两种模式。
- `framework.outbox.integration-event.mode`：`direct` 为默认，直接调用 publisher 不存储状态；`reliable` 先持久化再异步发布。
- reliable 模式：业务状态与 outbox 记录的原子性由调用方在同一本地事务中保证；存在事务同步时提交后发出发布 signal，没有事务同步时立即发出 signal，不保证与另行提交的业务写入原子一致。即时重试支持 retry/no-retry 异常类型及 fixed、uniform-random、exponential、exponential-random 退避，fallback 扫描兜底；signal、即时重试和 fallback 均以 `batchId` 为单位。
- 同一 batch 必须属于同一聚合，入库时记录 `batchId` 和 `batchSequence` 保持批内顺序，数据库状态整批从 `PENDING` 推进到 `PUBLISHING`、`PUBLISHED`。
- 发布端口优先使用 `PartitionedOperations`，以 `aggregateType` 作为 destination、`aggregateId` 作为 partition argument；无真实消息端口时使用日志型 no-op publisher。
- 事件发生时间和 outbox 的创建、发布、失败等状态时间均使用绝对时间；状态时间支持替换时钟，默认 UTC 系统时钟。
- 具体数据库 repository 实现由服务或独立技术适配 module 提供，starter 不绑定 JPA、MyBatis 等持久化技术。
- 消费侧约定：消费者完成消费的去重记录必须与本地业务写入在同一事务内原子提交；非事务的外部副作用需配套可恢复的幂等协议。starter 侧重放（signal 丢失、PUBLISHING 超时回收）按 at-least-once 投递，安全性依赖上述幂等约定。

### `framework-starter-persistence-jpa`

- 为 JPA 表名应用 `framework.persistence.naming` 的前缀/后缀。

### `framework-starter-persistence-mybatisplus`

- 分页按 `framework.persistence.database` 显式指定数据库类型，不支持的类型启动即失败；表名前缀/后缀语义与 JPA 一致。
- 支持使用方追加自定义拦截器，执行顺序须保留分页和命名约束。

### `framework-starter-scheduler-xxljob`

- 提供 XXL-JOB executor 适配，`scheduler.xxljob.enabled` 控制开关，开启时要求 `admin-addresses`；executor 的应用标识遵循 namespace 解析约定。
- 定时任务只承载兜底、补偿和批处理等异步辅助能力，不作为核心实时链路的前置依赖。

### `framework-starter-webmvc`

- 提供 Spring MVC 的 CORS（`framework.webmvc.cors`）和统一异常处理自动配置。
- 支持标准请求体和无请求体的框架具体上下文请求参数，统一覆盖受保护 Header。AppRequest 的 appId/appPlatform/appVersion 来自 APP_ID/APP_PLATFORM/APP_VERSION，ClientContext.clientId 只来自 CLIENT_ID（X-Client-Id），AuthenticatedSessionContext 来自 USER_ID/SESSION_ID；不读取 X-Channel-Code。缺失 Header 也覆盖为 null。starter 只完成绑定；参数上的 `@Valid`/`@Validated` 负责校验必填契约。
- MVC 统一返回无数据的标准错误响应。请求校验、请求体格式和其他协议拒绝使用对应 Web 入站错误；返回值约束异常与未分类异常返回 `INTERNAL_ERROR/500`。业务 `BaseException` 保留既有 200 与稳定错误码；`FrameworkException` 返回 500 和稳定枚举消息，不透出调用点诊断文本。
- MVC 的 ErrorResponseException 和实现 ErrorResponse 的标准 Servlet 协议异常保留原 HTTP 状态及响应 Header（如 405 的 Allow、415 的 Accept）；Controller 异常翻译不接管 Servlet Filter/Security 链。

### `framework-starter-webflux`

- 提供 reactive 应用的 Controller 请求上下文绑定与 REST 异常翻译自动配置，不依赖 webmvc、gateway 或 Servlet。
- 标准请求体及 reactive 请求体元素均先绑定受保护 Header，再执行入口声明的校验；保留原媒体类型、解码行为和标准请求体使用方式。
- 支持无请求体的四个框架具体上下文请求类型，Header 字段与覆盖语义与 WebMVC 相同；版本声明由入口清洗，starter 不重复 gateway 规则。
- 只在 reactive Web 应用生效，允许替换默认适配能力，并沿用应用已有的消息读取和校验配置；运行时技术由最终服务选择。
- WebFlux 的统一错误响应遵循与 MVC 相同的状态映射；仅处理 Controller 边界：外部输入失败使用 Web 入站错误，返回值约束失败使用 `INTERNAL_ERROR/500`；业务 `BaseException` 保留错误码及既有 200 响应约定，`FrameworkException` 和未分类异常返回 500，ErrorResponseException（包括 ResponseStatusException）保留 HTTP 状态及响应 Header。不接管 WebFilter/Security 拒绝或代理响应。
- 本模块不提供 CORS 自动配置；服务按部署策略配置，不覆盖 Spring Security 或 SCG 的 CORS 行为。

## 集成测试工程

- `framework-starter-tests` 按能力或完整场景建立独立测试子模块，例如 `framework-starter-outbox-rabbitmq-test`。
- 场景覆盖与手工观测入口见各测试子模块的同名文档；测试组织见 [DESIGN.md](DESIGN.md)。
- `framework-starter-tests` 不随每次常规 `verify` 重复执行；仅在测试子模块的直接或传递依赖发生变更时执行受影响场景。
