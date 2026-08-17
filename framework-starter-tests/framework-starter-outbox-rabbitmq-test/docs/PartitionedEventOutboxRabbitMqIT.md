# PartitionedEventOutboxRabbitMqIT

## 测试目标

该测试使用四个分区队列和两个独立 Spring ApplicationContext，验证 selector 路由模式下的：

- partition key 到队列 index 的稳定映射。
- 分区内严格有序消费。
- 四个分区之间并行消费。
- 每个队列注册两个 consumer 时仅有一个 active consumer。

单独执行：

```bash
mvn verify -pl :framework-starter-outbox-rabbitmq-test -am \
  -Dit.test=PartitionedEventOutboxRabbitMqIT
```

## 场景配置

```text
framework.outbox.integration-event.mode=direct
framework.rabbitmq.producer.reliability-mode=publisher-confirm
framework.rabbitmq.partitioned.routing-mode=selector
framework.rabbitmq.partitioned.selector.algorithm=hash
framework.rabbitmq.partitioned.destinations.partitioned-event-outbox-test=4
```

测试为四个分区各构造 10 个事件，共 40 个事件。事件按 sequence 交错排列后一次批量发布：

```text
p0-s0, p1-s0, p2-s0, p3-s0, ... , p0-s9, p1-s9, p2-s9, p3-s9
```

目标队列为：

```text
partitioned_queue.partitioned-event-outbox-test.0
partitioned_queue.partitioned-event-outbox-test.1
partitioned_queue.partitioned-event-outbox-test.2
partitioned_queue.partitioned-event-outbox-test.3
```

## 自动判定

- 实际装配 direct outbox 与 partitioned publisher。
- `MessageQueueSelector` 将字符串 `"0"` 到 `"3"` 分别映射到队列 0 到 3。
- 每个队列在发送前均已注册两个 consumer。
- 每个分区只从对应 queue 收到消息，sequence 严格为 0 到 9。
- 每个分区的消费记录只包含一个 consumerId。
- 四个首消息回调通过 `CountDownLatch(4)` 同时汇合，最大并发回调数至少为 4。

并行性由回调重叠直接观测，不根据总耗时推测。

## 手工观测

在 `shouldRouteToFourOrderedParallelPartitionsWithOneActiveConsumerPerQueue` 中设置两个断点：

1. `awaitFourQueuesWithTwoConsumers(firstContext)` 之后、`outbox.appendAll(events())` 之前。
2. Awaitility 消费断言完成之后。

Debug 运行测试，停在第一个断点后打开 RabbitMQ Management：

```bash
docker ps --filter ancestor=rabbitmq:4.2.8-management
docker port <container-id> 15672/tcp
```

依次进入四个分区队列，预期每个队列均包含 `x-single-active-consumer=true`、注册两个 consumer，且只有一个为 active。

继续到第二个断点，在 IDE Evaluate Expression 中查看：

```java
CONSUMPTION_PROBE.invocationCount()
CONSUMPTION_PROBE.parallelBarrierCompleted()
CONSUMPTION_PROBE.maxConcurrentCallbacks()
CONSUMPTION_PROBE.sequences(0)
CONSUMPTION_PROBE.queues(0)
CONSUMPTION_PROBE.consumerIds(0)
```

将最后三项参数替换为 1、2、3。预期：

- 调用总数为 40。
- 并行屏障完成，最大并发回调数至少为 4。
- 每个 sequence 为 `[0, 1, ..., 9]`。
- 每个分区只出现对应队列和一个 consumerId。

## 当前边界

该测试不覆盖 selector 算法切换、消费者故障接管、消费失败重试、死信以及多 JVM 部署。
