# BinaryDelayedRabbitMqIT

## 测试目标

本模块验证 `framework-starter-message-rabbitmq` 不依赖 RabbitMQ delayed message exchange 插件，仅使用二进制分层 TTL 队列和 DLX 完成延迟投递：

- `DelayedOperations` 使用真实 `RabbitTemplate` 发送消息。
- RabbitMQ 生产者使用 `publisher-confirm` 模式。
- `700ms` 延迟被拆分到 `400ms`、`200ms`、`100ms` 三个 TTL 层级。
- 消息在延迟完成前不能从业务投递队列取出，完成后能够取得目标消息。
- RabbitMQ 生成的 `timestamp_in_ms` 用于观测 Broker 内实际延迟，`x-death` 用于验证消息经过的 TTL 队列。

测试入口为：

```text
src/test/
├── java/com/cloud/framework/starter/message/rabbitmq/delayed/test/
│   └── BinaryDelayedRabbitMqIT.java
└── resources/rabbitmq/
    └── rabbitmq.conf
```

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- Docker Desktop、OrbStack 或其他兼容 Docker API 的容器运行环境。
- Docker daemon 已启动，当前用户能够执行 `docker info`。

测试使用 Testcontainers 创建并销毁 `rabbitmq:4.2.8-management`，不要求本机安装 RabbitMQ。测试通过 RabbitMQ 内置 incoming message interceptor 生成毫秒级 `timestamp_in_ms`：

```properties
message_interceptors.incoming.set_header_timestamp.overwrite = true
```

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-message-rabbitmq-delayed-test -am
```

其中：

- `verify` 触发 Maven Failsafe 执行 `*IT` 集成测试。
- `-pl` 选择当前测试子模块。
- `-am` 同时构建测试依赖的 framework starter 模块。

测试报告位于：

```text
framework-starter-tests/framework-starter-message-rabbitmq-delayed-test/
└── target/failsafe-reports/
    ├── com.cloud.framework.starter.message.rabbitmq.delayed.test.BinaryDelayedRabbitMqIT.txt
    └── TEST-com.cloud.framework.starter.message.rabbitmq.delayed.test.BinaryDelayedRabbitMqIT.xml
```

预期结果为：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

`mvn test` 只执行 Surefire 管理的单元测试，不会执行当前 `*IT`。

## 场景配置

集成测试使用以下关键配置：

| 配置 | 值 | 作用 |
| --- | --- | --- |
| `framework.rabbitmq.producer.reliability-mode` | `publisher-confirm` | 等待 RabbitMQ 对初始发布进行确认 |
| `framework.rabbitmq.delayed.levels` | `3` | 建立三个二进制延迟层级 |
| `framework.rabbitmq.delayed.tick-duration` | `100ms` | 一个 tick 表示 `100ms` |
| `framework.rabbitmq.delayed.destinations[0]` | `delayed-orders-test` | 声明业务 destination 及其投递队列 |
| `framework.rabbitmq.delayed.dead-letter.enabled` | `false` | 本场景不验证业务投递失败后的死信队列 |
| `message_interceptors.incoming.set_header_timestamp.overwrite` | `true` | 由 RabbitMQ 覆盖并写入 Broker 接收时间 |

三个层级最多表达 `2^3 - 1 = 7` 个 tick，即 `700ms`。测试消息的延迟正好为 `700ms`，换算结果为七个 tick，二进制表示为 `111`。

## 拓扑与消息路径

应用启动后应声明以下公共拓扑：

| Exchange | Queue | TTL | Queue 到期后的 DLX |
| --- | --- | --- | --- |
| `delayed.level.x.2` | `delayed.level.q.2` | `400ms` | `delayed.level.x.1` |
| `delayed.level.x.1` | `delayed.level.q.1` | `200ms` | `delayed.level.x.0` |
| `delayed.level.x.0` | `delayed.level.q.0` | `100ms` | `delayed.delivery.x` |

业务 destination 还会声明投递队列：

```text
delayed.delivery.q.delayed-orders-test
```

`700ms` 消息从最高层 Exchange 开始，routing key 为：

```text
1.1.1.delayed-orders-test
```

每一位为 `1` 时进入对应 TTL 队列等待，为 `0` 时直接路由到下一层。本用例三位均为 `1`，因此实际路径为：

```text
delayed.level.x.2
  -> delayed.level.q.2 (400ms)
  -> delayed.level.x.1
  -> delayed.level.q.1 (200ms)
  -> delayed.level.x.0
  -> delayed.level.q.0 (100ms)
  -> delayed.delivery.x
  -> delayed.delivery.q.delayed-orders-test
```

本场景关闭了业务投递死信能力，因此不应声明 `delayed.dlx` 和 `delayed.dlq.delayed-orders-test`。层级队列自身的 DLX 是延迟算法的一部分，不受该开关影响。

## 手工观测

### 1. 设置断点

在 `BinaryDelayedRabbitMqIT.shouldDeliverOnlyAfterTraversingTheBinaryDelayLevels` 中设置两个断点：

1. Spring ApplicationContext 启动完成后、调用 `delayedOperations.convertAndSend` 之前。
2. 方法末尾打印观测结果的 `log.info` 语句处。

从 IDE 以 JUnit Debug 方式运行 `BinaryDelayedRabbitMqIT`。

### 2. 查看 RabbitMQ 拓扑

测试停在第一个断点时执行：

```bash
docker ps --filter ancestor=rabbitmq:4.2.8-management
docker port <container-id> 15672/tcp
```

使用映射端口打开 RabbitMQ Management：

```text
http://localhost:<mapped-port>
```

默认用户名和密码均为 `guest`。在 Exchanges 和 Queues 页面核对上一节列出的拓扑，并重点观察：

- 三个层级队列的 TTL 分别为 `400`、`200`、`100` 毫秒。
- 每个层级队列的 dead letter exchange 指向下一层 Exchange。
- 业务投递队列绑定到 `delayed.delivery.x`。
- 没有 delayed message exchange 插件类型的 Exchange。
- 没有业务投递 DLX 和 DLQ。

### 3. 手工验证 TTL + DLX 路径

保持测试停在第一个断点，在 RabbitMQ Management 的 `delayed.level.x.2` Exchange 页面手工发布一条消息：

```text
Routing key: 1.1.1.delayed-orders-test
Payload: manual-order
```

发布后刷新 Queues 页面。约 `700ms` 后，`delayed.delivery.q.delayed-orders-test` 的 Ready 数量应增加 `1`；从该队列执行 Get Message 时选择自动 ACK，应取得 `manual-order`，并观察到：

- `timestamp_in_ms` 为 RabbitMQ 首次收到该消息的毫秒级时间。
- `x-death` 包含 `delayed.level.q.0`、`delayed.level.q.1`、`delayed.level.q.2` 三项。
- 三项的 `reason` 均为 `expired`，`count` 均为 `1`。
- `x-death` 按最近发生时间排序，因此界面中的顺序为 `q.0 -> q.1 -> q.2`，实际经过顺序为 `q.2 -> q.1 -> q.0`。

再次获取时不应再出现同一消息。

该操作直接验证 RabbitMQ 中的二进制 TTL 与 DLX 拓扑。获取并清除手工消息后再继续自动测试，避免它先于测试消息被 `RabbitTemplate` 取走。

### 4. 观察自动测试探针

获取并清除手工消息后继续运行到第二个断点，在 IDE Evaluate Expression 中查看：

```java
earlyMessage
payload
brokerTimestamp
brokerDelay
queues
reasons
counts
```

预期结果为：

```text
earlyMessage = null
payload = "order-1"
brokerTimestamp 为 Number
700ms <= brokerDelay <= 2700ms
queues = [delayed.level.q.0, delayed.level.q.1, delayed.level.q.2]
reasons = [expired, expired, expired]
counts = [1, 1, 1]
```

RabbitMQ Management 中 `delayed.delivery.q.delayed-orders-test` 最终应没有 Ready 或 Unacked 消息。

第一个断点位于测试消息发送前，第二个断点位于全部时间数据采集完成后，因此在这两个位置暂停不会增加本次 `brokerDelay`。精确结果仍以不设置断点的自动执行为准。

## 自动用例判定

测试只通过公共抽象调用一次：

```java
delayedOperations.convertAndSend(
        "delayed-orders-test",
        "order-1",
        payload -> Duration.ofMillis(700)
);
```

随后从真实 RabbitMQ 的业务投递队列进行两次拉取消费，并检查最终消息的 Broker 元数据：

1. 第一次最多等待 `300ms`，必须返回 `null`，排除消息被立即投递或提前投递。
2. 第二次最多等待 `10s`，必须取得 payload `order-1`。
3. 消费时间减去 RabbitMQ 写入的 `timestamp_in_ms` 必须位于 `700ms` 到 `2700ms` 之间；前者验证不会提前投递，额外 `2s` 用于容纳容器、网络和线程调度延迟。
4. `x-death` 必须按 RabbitMQ 的最近优先顺序包含 `q.0`、`q.1`、`q.2`，每一项的 `reason=expired` 且 `count=1`，证明实际经过了全部三个 TTL 层级。

`publisher-confirm` 只确认初始消息已被入口 Exchange 接收；测试最终从投递队列取得消息，才证明消息确实完成了全部 TTL 与 DLX 路径。
