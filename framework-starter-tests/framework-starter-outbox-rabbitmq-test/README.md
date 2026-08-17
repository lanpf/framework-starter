# Event Outbox RabbitMQ 集成测试

本模块使用真实 RabbitMQ 验证 direct integration-event outbox 的 publisher confirm、分区路由、顺序与并行消费以及 Single Active Consumer。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- 可用的 Docker 运行环境。

测试由 Testcontainers 创建并销毁 `rabbitmq:4.2.8-management`；一致性哈希场景会在容器内启用 `rabbitmq_consistent_hash_exchange` 插件。

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-outbox-rabbitmq-test -am
```

预期结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

测试报告位于 `target/failsafe-reports/`。

## 测试文档

- [EventOutboxRabbitMqIT](docs/EventOutboxRabbitMqIT.md)
- [PartitionedEventOutboxRabbitMqIT](docs/PartitionedEventOutboxRabbitMqIT.md)
- [ConsistentHashPluginEventOutboxRabbitMqIT](docs/ConsistentHashPluginEventOutboxRabbitMqIT.md)
