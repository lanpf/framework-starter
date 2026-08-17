# RabbitMQ Delayed 集成测试

本模块使用真实 RabbitMQ 验证二进制分层 TTL 与 DLX 延迟消息能力。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- 可用的 Docker 运行环境。

测试由 Testcontainers 创建并销毁 `rabbitmq:4.2.8-management`。

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-message-rabbitmq-delayed-test -am
```

预期结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

测试报告位于 `target/failsafe-reports/`。

## 测试文档

- [BinaryDelayedRabbitMqIT](docs/BinaryDelayedRabbitMqIT.md)
