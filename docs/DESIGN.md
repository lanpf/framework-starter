# framework-starter 技术设计

本文维护项目专属的依赖、装配与适配设计。对外能力、使用前提、保证与限制以 [RESPONSIBILITIES.md](RESPONSIBILITIES.md) 为准；修改 starter 前按该入口阅读相关设计。

共享实现约定由工程规范维护：

- [依赖规约](../.agents/skills/develop-service/references/dependency-rules.md)（源：`bp_dependencies.md`）。
- [架构规约](../.agents/skills/develop-service/references/architecture.md)（源：`bp_layered_service.md`）。
- [基础设施规约](../.agents/skills/develop-service/references/infrastructure-interfaces-boot.md)（源：`bp_layered_service.md`）。
- [MapStruct 规约](../.agents/skills/develop-service/references/mapstruct.md)（源：`bp_common_tools.md#MapStruct`）。
- [Java 集合所有权](../.agents/skills/develop-service/references/java-language.md)与[校验规约](../.agents/skills/develop-service/references/service-validation.md)（源：`bp_java.md`、`bp_validation.md`）。

## 模块与依赖

- `framework-starter-*` 生产 module 之间不得相互依赖；每个 starter 只依赖其适配的 `framework-*` 契约和当前技术实现所需的三方依赖。
- 对 `framework-*` 的依赖是 starter 对外能力契约的一部分，使用正常编译依赖；三方技术依赖统一使用 `provided`，由最终引用方决定运行时实现。
- 只有承载具体技术实现时才允许引入对应聚合型 starter，并优先选择能够满足实现的最小依赖。
- `framework-starter-bom` 只管理 starter 版本，不承载运行时代码。
- `framework-starter-tests` 是测试聚合模块，可以按完整场景组合多个 framework 和 starter，不受生产 starter 互不依赖约束。

## 自动配置组织

共享的配置归属、条件装配、Bean 复用与绑定约定遵循[架构规约](../.agents/skills/develop-service/references/architecture.md)和[基础设施规约](../.agents/skills/develop-service/references/infrastructure-interfaces-boot.md)。本工程的具体组织如下：

- 能力根配置聚合公共 Bean；由配置项切换的互斥实现使用独立或嵌套的 `*AutoConfiguration`，同一分支的条件集中在配置类上。
- 锁续期与 outbox 异步线程池复用 `TaskExecutionProperties.Pool` 和对应 builder；线程名前缀、拒绝策略和调度策略等技术特有参数单独配置。
- 模式与算法通过可空枚举或条件属性选择；开关使用 `boolean`，不增加表示未配置的 `none` 枚举值。

## 持久化信封模式

`domain-eventstore` 与 `outbox` 采用同构的技术中立持久化结构，两个 starter 均不携带持久化技术注解：

- starter 内提供 `*Envelope` 持久化模型（record）、`*EnvelopePersistenceRepository`、`*EnvelopePersistenceMapper` 契约和默认编排；`OutboxStatus` 等状态枚举保持技术中立。
- 转换契约声明在 `persistence` 包；MapStruct 实现放在契约包的 `mapstruct` 子包，命名 `*MapStructMapper`，通过 `uses` 引入同子包的 `*PayloadConverter` 完成 payload 序列化与反序列化。
- `persistence/*EnvelopeAutoConfiguration` 统一声明 converter 与 mapper Bean（`Mappers.getMapper` 实例化生成实现，`@ConditionalOnMissingBean` 允许替换），由能力配置类 `@Import`。
- payload 序列化/反序列化失败抛 `FrameworkException`，错误枚举由 event-store 或 outbox 能力各自持有；`ObjectMapper` 复用 Spring Boot 自动配置的实例。
- 技术注解 DO、DO↔Envelope 转换 mapper 与 repository 实现由服务或独立技术适配 module 提供（参考 `framework-starter-outbox-jpa-rabbitmq-test` 的 JPA 实现）。

## 模块适配细节

- `NamespaceAutoConfiguration` 装配 `NamespaceResolver`，并提供委托 `ResourceNameResolver` 的 `AbstractKeyResolver` 基类。
- `CosIdIdGeneratorAutoConfiguration` 只在存在 `IdGeneratorProvider` 时提供 `CosIdLongIdGenerator`。
- Spring Integration 锁通过 `RedisLockRegistry` 与 renewal scheduler 适配；续期线程池和 remove-on-cancel 单独配置。Redisson 使用 watchdog，并通过 `RedissonAutoConfigurationCustomizer` 对齐租期配置。
- 消息 converter 通过 `BeanPostProcessor` 注入 `DefaultMessageHandlerMethodFactory`。
- 分区与延迟拓扑分别使用 registry 和 declarer，由 `RabbitTopologyInitializer` 在单例初始化完成后声明。
- outbox 状态时间使用 `Instant`，通过可注入的 `Clock` 获取；异步执行复用 Boot 的 pool 属性模型并独立配置线程名前缀。
- MyBatis-Plus 的 `InnerInterceptor` Bean 经 `orderedStream()` 加入拦截器链。

## RabbitMQ Header 保存边界

分区与延迟消息的 Header 工厂会返回可重复调用的 `MessagePostProcessor`。工厂在返回回调前保存独立、不可修改的 Map 容器快照，template 和 publisher 不重复复制。后续修改输入 Map 或前一条消息的 Header 容器不会影响该回调。

Header 参数允许 null，按空 Map 处理；值仍按原协议透传，允许 null 值，可变值对象保持共享。这里不使用拒绝 null 值的 `Map.copyOf`，也不为复制新增 framework-core 依赖。独立 `LinkedHashMap` 副本的不可修改包装仅保证容器隔离，不承诺值对象的深度不可变。分区键仍由回调最后写入，不改变原 Map。

## framework 协作位置

- 错误码基础、异常与 Require 引用 `framework-kernel`；请求模型引用 `framework-core.request`，响应信封引用 `framework-core.response`，HTTP Header 与 Web 入站错误引用 `framework-core.http`。


## WebFlux 请求绑定

- `AppRequestWebFluxAutoConfiguration` 在 RequestMappingHandlerAdapter 初始化前装饰其现有 HttpMessageReader；保留消息读取器的媒体类型、服务端提示和解码行为，在解码后绑定 Header，再由 Spring 标准 `@RequestBody` 解析器执行校验。不替换默认参数解析链，不增加请求体注解；普通对象、Mono/Flux 元素均使用同一流程。
## 集成测试组织

`framework-starter-tests` 按能力或完整场景建立测试子模块。子模块只声明 test scope 依赖，不加入 `framework-starter-bom`，不安装或发布测试制品。场景结构、同名文档、手工观测和 Failsafe 执行遵循[集成测试规约](../.agents/skills/test-integration/references/integration-testing.md)，仅在冒烟阶段执行受影响场景。

## Web Controller 适配

- MVC 的 `AppRequestBodyAdvice` 处理请求体，`AppRequestArgumentResolver` 处理无请求体上下文参数，两者复用 `AppRequestHeaderBinder`。
- `WebMvcExceptionHandler` 与 `WebFluxExceptionHandler` 的 `failure`、`validationFailure`、`httpFailure` 统一返回 `ResponseEntity<Result<Void>>`；对外状态与错误语义见职责文档。
