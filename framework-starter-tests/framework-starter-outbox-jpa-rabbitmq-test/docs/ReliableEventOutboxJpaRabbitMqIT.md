# ReliableEventOutboxJpaRabbitMqIT

## 测试目标

本模块验证 integration event 在以下组合下完成可靠存储、发布和分区消费：

- `IntegrationEventOutbox` 使用 `reliable` 模式。
- outbox persistence port 使用 Spring Data JPA 实现，测试数据库为 H2。
- `IntegrationEventPublisher` 使用 `PartitionedIntegrationEventPublisher`。
- RabbitMQ 生产者使用 `publisher-confirm` 模式。
- 同一分区队列注册两个消费者时，仅有一个 active consumer 消费该分区的消息。

测试入口为：

```text
src/test/java/com/cloud/framework/starter/message/outbox/jpa/rabbitmq/test/
└── ReliableEventOutboxJpaRabbitMqIT.java
```

## 测试结构

`framework-starter-outbox` 定义通用 outbox DO 和 persistence port，不提供绑定具体数据库技术的 repository 实现。本测试在测试模块内提供最小 JPA 适配：

```text
persistence/
├── IntegrationEventOutboxJpaRepository.java
└── IntegrationEventOutboxJpaPersistenceRepository.java
```

其中：

- `IntegrationEventOutboxJpaRepository` 继承 Spring Data JPA 的 `JpaRepository`。
- `IntegrationEventOutboxJpaPersistenceRepository` 实现框架的 `IntegrationEventOutboxEnvelopePersistenceRepository`。
- `JpaOutboxTestConfiguration` 负责实体扫描、JPA repository 扫描和 persistence port 装配。
- 每个 Spring 上下文使用独立的 H2 内存数据库，避免两个测试消费者共享应用内数据库状态。
- RabbitMQ 由 Testcontainers 创建，两个 Spring 上下文连接同一个真实 broker。

测试还提供 `PausableAsyncTaskExecutor`。它先接收但不立即执行 outbox signal 任务，使测试可以稳定观察“事务已提交、记录仍为 `PENDING`、消息尚未发送”的中间状态；调用 `releaseAll()` 后才继续执行 publisher。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- Docker Desktop、OrbStack 或其它兼容 Docker API 的容器运行环境。
- Docker daemon 已启动，并且当前用户能够执行 `docker info`。

H2 随测试依赖启动，不需要本机安装数据库；RabbitMQ 由 Testcontainers 自动创建和销毁。

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-outbox-jpa-rabbitmq-test -am
```

其中：

- `verify` 触发 Maven Failsafe 执行 `*IT`。
- `-pl` 选择当前测试子模块。
- `-am` 同时构建测试依赖的 framework starter 模块。

测试报告位于：

```text
framework-starter-tests/framework-starter-outbox-jpa-rabbitmq-test/
└── target/failsafe-reports/
    ├── com.cloud.framework.starter.outbox.jpa.rabbitmq.test.ReliableEventOutboxJpaRabbitMqIT.txt
    └── TEST-com.cloud.framework.starter.outbox.jpa.rabbitmq.test.ReliableEventOutboxJpaRabbitMqIT.xml
```

预期结果为：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

`mvn test` 不执行当前 `*IT`，完整验证必须使用 `mvn verify`。

## 手动操作

当前场景由测试代码统一管理 H2、RabbitMQ、生产者和两个消费者。人工验证建议 Debug `ReliableEventOutboxJpaRabbitMqIT`，直接观察同一条自动测试链路。

### 1. 设置断点

在测试方法 `shouldPersistReliableOutboxWithJpaAndPublishToOneActiveConsumerWithConfirm` 中设置两个断点：

1. `assertPendingBatch(jpaRepository, events)` 执行完成之后、`taskExecutor.releaseAll()` 执行之前。
2. 消费和 `PUBLISHED` 状态的 Awaitility 断言执行完成之后。

### 2. Debug 测试

从 IDE 以 JUnit Debug 方式运行 `ReliableEventOutboxJpaRabbitMqIT`。测试会依次：

1. 启动 RabbitMQ 容器。
2. 启动带有独立 H2 数据库的 `consumer-1` Spring 上下文。
3. 启动带有另一个独立 H2 数据库的 `consumer-2` Spring 上下文。
4. 等待相同分区队列上注册两个 RabbitMQ consumer。
5. 在 JPA 事务中向 reliable outbox 追加 20 个事件。
6. 提交事务，并将 after-commit signal 暂存在 `PausableAsyncTaskExecutor`。

### 3. 观察 PENDING 状态

测试停在第一个断点时，在 IDE Evaluate Expression 中查看：

```java
jpaRepository.findByBatchIdOrderByBatchSequenceAsc("event-0")
taskExecutor.pendingTaskCount()
CONSUMPTION_PROBE.invocationCount()
```

预期观察到：

- JPA 查询返回 20 条 outbox 记录。
- 所有记录的 `batchId` 都是 `event-0`。
- `batchSequence` 依次为 `0` 到 `19`。
- 所有记录状态为 `PENDING`。
- `retryCount` 为 `0`，`publishedAt` 为 `null`。
- 每条记录均保存了序列化后的 payload。
- `taskExecutor.pendingTaskCount()` 为 `1`，表示 after-commit signal 已产生但尚未执行。
- `CONSUMPTION_PROBE.invocationCount()` 为 `0`，表示消费者尚未收到消息。
- RabbitMQ 分区队列的 message count 为 `0`，表示 publisher 尚未运行。

这些观察共同证明 outbox 记录先在事务中持久化并提交，MQ 发布没有先于数据库提交发生。

### 4. 查看 RabbitMQ 消费者

仍停在第一个断点时执行：

```bash
docker ps --filter ancestor=rabbitmq:4.2.8-management
docker port <container-id> 15672/tcp
```

使用映射端口打开 RabbitMQ Management：

```text
http://localhost:<mapped-port>
```

默认用户名和密码均为 `guest`。进入队列：

```text
partitioned_queue.reliable-event-outbox-test.0
```

应观察到：

- 队列参数包含 `x-single-active-consumer = true`。
- consumer 数量为 `2`。
- consumer 明细中一个处于 active 状态，另一个等待接管。
- 对应 exchange 为 `partitioned_exchange.reliable-event-outbox-test`。

这一步用于证明两个消费者均已连接，并排除第二个消费者未成功启动造成的假阳性。

### 5. 释放 signal 并观察结果

继续执行 `taskExecutor.releaseAll()`。该操作会让异步任务执行：

```text
IntegrationEventOutboxSignalListener
  -> IntegrationEventOutboxPublisher.publish(batchId)
  -> JPA claim: PENDING -> PUBLISHING
  -> IntegrationEventOutboxPersistenceMapper 反序列化事件
  -> PartitionedIntegrationEventPublisher.publishAll
  -> PartitionedOperations.convertAndSendBatch
  -> RabbitTemplate
  -> RabbitMQ publisher confirm
  -> JPA update: PUBLISHING -> PUBLISHED
```

测试停在第二个断点时，查看：

```java
jpaRepository.findByBatchIdOrderByBatchSequenceAsc("event-0")
CONSUMPTION_PROBE.invocationCount()
CONSUMPTION_PROBE.eventIds()
CONSUMPTION_PROBE.consumerIds()
CONSUMPTION_PROBE.countByConsumer()
```

预期观察到：

- 20 条 JPA 记录全部变为 `PUBLISHED`。
- 20 条记录的 `publishedAt` 均已赋值。
- 消费调用次数为 `20`。
- 消费记录包含 `event-0` 到 `event-19`，没有观察到重复或遗漏。
- `consumerIds` 只包含 `consumer-1` 或 `consumer-2` 中的一个。
- 唯一消费者的消费数量为 `20`。

继续运行后，两个 Spring 上下文、H2 数据源和 RabbitMQ 容器会被自动关闭。

## 用例判定依据

### 自动配置与持久化实现

测试首先断言实际 Bean 类型：

```text
IntegrationEventOutbox -> ReliableIntegrationEventOutbox
IntegrationEventPublisher -> PartitionedIntegrationEventPublisher
IntegrationEventOutboxPersistenceRepository -> IntegrationEventOutboxJpaPersistenceRepository
```

这证明配置选择的是 reliable outbox、分区 publisher 和 JPA persistence adapter，而不是 direct、no-op 或其它存储实现。

### 先存储、后发布

`appendAll` 在 `TransactionTemplate` 管理的 JPA 事务中执行。Reliable outbox 只有在事务提交后才发布 signal；可控执行器暂停 signal 任务后，测试观察到：

```text
数据库：20 条 PENDING
signal：1 个待执行任务
RabbitMQ：0 条待消费消息
消费者：0 次调用
```

因此可以判定 outbox 数据已提交，而 MQ 发布尚未发生。

### Publisher Confirm 与状态完成

释放 signal 后，批量分区发送只允许在 `publisher-confirm` 模式下执行，并等待所有消息的 broker confirm。NACK、returned message 或 confirm 超时都会抛出异常，使事务无法把整批记录更新为 `PUBLISHED`。

最终同时观察到 20 条记录全部为 `PUBLISHED`、`publishedAt` 非空且消费者收到全部事件，可以判定本次正向发布获得 broker ACK 并完成 outbox 状态更新。

### Single Active Consumer

发送前 RabbitMQ 报告同一个队列存在两个 consumer；发送后探针只记录到一个 consumerId，且该 consumerId 对应 20 次调用。第二个 Spring 上下文仍处于 active 状态。

因此可以判定不是“仅启动了一个消费者”，而是 RabbitMQ 的 Single Active Consumer 机制只让一个消费者处理该分区的全部消息。

## 当前边界

本用例不覆盖：

- active consumer 下线后的 standby consumer 接管。
- publisher-confirm 的 NACK、returned message 和超时失败路径。
- signal 重试耗尽和 fallback job。
- `FAILED` 状态及 retry count 推进。
- 消费异常、重试、死信和毒消息处理。
- 消息的严格消费顺序。
- 服务业务数据和 outbox 数据处于同一事务的具体服务场景；本测试只建立 outbox 自身的 JPA 事务。
- 多 JVM 或多容器部署；当前两个消费者来自同一 JVM 中的两个独立 Spring 上下文。
