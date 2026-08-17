# Reliable Outbox JPA RabbitMQ 集成测试

本模块使用 H2 和真实 RabbitMQ，验证 reliable integration-event outbox 的 JPA 持久化、异步发布、publisher confirm 与 Single Active Consumer。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- 可用的 Docker 运行环境。

H2 随测试依赖启动，RabbitMQ 由 Testcontainers 创建并销毁。

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-outbox-jpa-rabbitmq-test -am
```

预期结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

测试报告位于 `target/failsafe-reports/`。

## 测试文档

- [ReliableEventOutboxJpaRabbitMqIT](docs/ReliableEventOutboxJpaRabbitMqIT.md)
