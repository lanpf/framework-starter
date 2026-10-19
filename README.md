# framework-starter

`framework-starter` 将 `framework-*` 契约适配到 Spring Boot 和具体技术栈，提供条件化、可替换、最小侵入的自动配置。能力交付状态见 [docs/RESPONSIBILITIES.md](docs/RESPONSIBILITIES.md)。

## 构建与验证

- JDK 17，Maven 多 module 工程。
- 构建并安装全部 module：`mvn install`
- 只运行单元测试：`mvn test`
- 集成测试位于 `framework-starter-tests` 下的测试子模块，不随常规 `verify` 重复执行；按需运行受影响场景，例如：

```bash
mvn -pl framework-starter-tests/framework-starter-outbox-rabbitmq-test -am verify
```

## 模块地图

| module | 职责 |
| --- | --- |
| `framework-starter-dependencies` | 服务工程统一引入的版本管理入口，聚合导入 `framework-starter-bom` |
| `framework-starter-bom` | starter module 版本约束，不承载运行时代码 |
| `framework-starter-autoconfigure` | 跨技术通用增强：日期时间反序列化、JSR-310、namespace 解析、`AbstractKeyResolver` |
| `framework-starter-id-cosid` | 存在 CosId `IdGeneratorProvider` 时装配 `LongIdGenerator` |
| `framework-starter-lock-redis` | Redis 分布式锁装配，spring-integration / redisson 两种 provider |
| `framework-starter-domain-eventstore` | 技术中立的领域事件存储信封、repository 契约与 default/noop 编排 |
| `framework-starter-message` | 复合 `MessageConverter` 装配与注入 |
| `framework-starter-message-rabbitmq` | RabbitMQ 生产/消费可靠性、分区、延迟拓扑 |
| `framework-starter-outbox` | 集成事件 outbox 信封、direct/reliable 模式与重试编排 |
| `framework-starter-persistence-jpa` | Hibernate 表名前缀/后缀命名策略 |
| `framework-starter-persistence-mybatisplus` | 分页与动态表名拦截器装配 |
| `framework-starter-scheduler-xxljob` | XXL-JOB executor 装配与校验 |
| `framework-starter-webmvc` | CORS、统一异常处理、App/Client/会话 Header 请求绑定 |
| `framework-starter-webflux` | reactive Controller 的标准 RequestBody 上下文绑定、校验与异常翻译 |
| `framework-starter-tests` | 按能力或场景组织的集成测试子模块（lock-redis、message-rabbitmq-delayed、outbox-rabbitmq、outbox-jpa-rabbitmq） |

各 module 的能力、配置契约与限制见 [docs/RESPONSIBILITIES.md](docs/RESPONSIBILITIES.md)，内部装配设计见 [docs/DESIGN.md](docs/DESIGN.md)。

## 文档路由

- 工程标准与任务路由：`AGENTS.md`（由 engineering-guidance-publisher 托管）。
- 工程职责、模块能力、对外保证与限制：[docs/RESPONSIBILITIES.md](docs/RESPONSIBILITIES.md)。
- 内部依赖、自动配置组织和持久化信封设计：[docs/DESIGN.md](docs/DESIGN.md)。
- 各集成测试场景文档位于对应测试子模块的 `docs/<测试类名>.md`。
- 纯技术适配工程，无 `docs/DOMAIN.md`。
