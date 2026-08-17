# ConsistentHashPluginEventOutboxRabbitMqIT

## 测试目标

该测试使用 RabbitMQ `x-consistent-hash` exchange 验证 broker 侧分区路由：

- 容器已启用 `rabbitmq_consistent_hash_exchange` 插件。
- 应用装配 `ConsistentHashPluginPartitionedRabbitTemplate`，不装配应用侧 `MessageQueueSelector`。
- 同一 partition key 的消息始终进入同一个 queue 并按 sequence 消费。
- 四个 queue 都参与分发，并各自只有一个 active consumer。

单独执行：

```bash
mvn verify -pl :framework-starter-outbox-rabbitmq-test -am \
  -Dit.test=ConsistentHashPluginEventOutboxRabbitMqIT
```

## 场景配置

```text
framework.outbox.integration-event.mode=direct
framework.rabbitmq.producer.reliability-mode=publisher-confirm
framework.rabbitmq.partitioned.routing-mode=consistent-hash-plugin
framework.rabbitmq.partitioned.destinations.consistent-hash-event-outbox-test=4
```

测试交错发送 64 个 partition key，每个 key 包含 sequence 0 到 4，共 320 条消息。RabbitMQ 容器通过 `src/test/resources/rabbitmq/enabled_plugins` 启用一致性哈希插件。

## 自动判定

- `rabbitmq-plugins is_enabled rabbitmq_consistent_hash_exchange` 返回成功。
- 四个队列在发送前均注册两个 consumer。
- 320 条消息全部被消费。
- 每个 partition key 只关联一个实际 queue。
- 每个 partition key 的 sequence 严格为 `[0, 1, 2, 3, 4]`。
- 四个 queue 都收到消息，每个 queue 的记录只包含一个 consumerId。

## 手工观测

在 `shouldKeepEachPartitionKeyOnOneOrderedQueueWithConsistentHashPlugin` 中设置两个断点：

1. `awaitFourQueuesWithTwoConsumers(firstContext)` 之后、`outbox.appendAll(events())` 之前。
2. Awaitility 消费断言完成之后。

停在第一个断点时确认插件与拓扑：

```bash
docker ps --filter ancestor=rabbitmq:4.2.8-management
docker exec <container-id> rabbitmq-plugins is_enabled rabbitmq_consistent_hash_exchange
docker port <container-id> 15672/tcp
```

打开 RabbitMQ Management 后，预期看到：

- `partitioned_exchange.consistent-hash-event-outbox-test` 的类型为 `x-consistent-hash`。
- 四个 `partitioned_queue.consistent-hash-event-outbox-test.<index>` 均已声明。
- 每个队列注册两个 consumer，且只有一个 active。

继续到第二个断点，在 IDE Evaluate Expression 中查看：

```java
CONSUMPTION_PROBE.invocationCount()
CONSUMPTION_PROBE.usedQueues()
CONSUMPTION_PROBE.queues("aggregate-0")
CONSUMPTION_PROBE.sequences("aggregate-0")
```

将 partition key 替换为其它 `aggregate-<index>`，预期每个 key 始终只有一个 queue，sequence 均为 0 到 4，`usedQueues()` 包含全部四个队列。

## 当前边界

该测试不评估一致性哈希的负载均衡质量、扩缩容后的 key 迁移比例、消费者故障接管及插件不可用时的降级策略。
