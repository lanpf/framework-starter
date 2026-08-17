# EventOutboxRabbitMqIT

## 测试目标

该测试使用两个独立 Spring ApplicationContext 和同一个真实 RabbitMQ，验证：

- `IntegrationEventOutbox` 装配为 `DirectIntegrationEventOutbox`。
- `IntegrationEventPublisher` 装配为 `PartitionedIntegrationEventPublisher`。
- 生产者使用 `publisher-confirm` 批量发送 20 个事件。
- 同一分区队列注册两个消费者时，只有一个 active consumer 消费全部消息。

单独执行：

```bash
mvn verify -pl :framework-starter-outbox-rabbitmq-test -am \
  -Dit.test=EventOutboxRabbitMqIT
```

## 场景配置

```text
framework.outbox.integration-event.mode=direct
framework.rabbitmq.producer.reliability-mode=publisher-confirm
framework.rabbitmq.partitioned.routing-mode=selector
framework.rabbitmq.partitioned.selector.algorithm=hash
framework.rabbitmq.partitioned.destinations.event-outbox-test=1
```

测试构造 20 个不同 `eventId` 的事件，使用相同的 `aggregateType=event-outbox-test` 和 `aggregateId=aggregate-1`，通过一次 `outbox.appendAll(events)` 发布。

## 自动判定

测试在发送前确认目标队列已经注册两个 consumer。消费完成后断言：

- listener 总调用次数为 20。
- 收到 20 个不同的 `eventId`。
- 消费记录只包含一个 consumerId。
- 唯一 active consumer 消费了全部 20 条消息。
- 第二个消费者所在的 Spring ApplicationContext 仍处于 active 状态。

这组断言排除了“第二个消费者未启动”造成的假阳性。

## 手工观测

在 `shouldPublishDirectOutboxWithConfirmAndConsumeByOneActiveConsumer` 中设置两个断点：

1. `awaitTwoRegisteredConsumers(firstContext)` 之后、`outbox.appendAll(events())` 之前。
2. Awaitility 消费断言完成之后。

从 IDE Debug 运行测试。停在第一个断点时执行：

```bash
docker ps --filter ancestor=rabbitmq:4.2.8-management
docker port <container-id> 15672/tcp
```

打开 `http://localhost:<mapped-port>`，使用 `guest/guest` 登录 RabbitMQ Management，进入：

```text
partitioned_queue.event-outbox-test.0
```

预期观察到：

- 队列参数包含 `x-single-active-consumer=true`。
- consumer 数量为 2。
- 一个 consumer 为 active，另一个等待接管。
- 队列绑定到 `partitioned_exchange.event-outbox-test`。

继续到第二个断点，在 IDE Evaluate Expression 中查看：

```java
CONSUMPTION_PROBE.invocationCount()
CONSUMPTION_PROBE.eventIds()
CONSUMPTION_PROBE.consumerIds()
CONSUMPTION_PROBE.countByConsumer()
```

预期调用次数和 eventId 数量均为 20，`consumerIds()` 只包含一个值，RabbitMQ 队列最终没有 Ready 或 Unacked 消息。

## 当前边界

该测试不覆盖 active consumer 下线后的接管、publisher-confirm 失败路径、消费重试和死信处理。
