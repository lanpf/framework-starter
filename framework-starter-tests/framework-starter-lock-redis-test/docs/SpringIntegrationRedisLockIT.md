# SpringIntegrationRedisLockIT

## 测试目标

该测试使用两个独立 Spring ApplicationContext 和同一个真实 Redis，验证 Spring Integration `RedisLockRegistry` 的互斥、自动续期、固定租期和资源命名。

测试类：

```text
src/test/java/com/cloud/framework/starter/lock/redis/test/SpringIntegrationRedisLockIT.java
```

单独执行：

```bash
mvn verify -pl :framework-starter-lock-redis-test -am \
  -Dit.test=SpringIntegrationRedisLockIT
```

## 自动续期场景

`shouldProvideMutualExclusionAndAutomaticRenewal` 使用：

```text
spring.application.name = spring-integration-auto-renewal-lock-test
provider                = spring-integration
lease-time              = 1s
auto-renewal            = true
scene                   = create-order
key                     = 1001
```

Redis 物理 key 必须为：

```text
spring-integration-auto-renewal-lock-test:create-order:1001
```

测试执行过程：

1. owner context 在独立线程中取得锁。
2. 连续观察超过 `1.5s`，key 始终存在且 TTL 为正，证明持锁时间超过一个租期后仍在续期。
3. contender context 最多等待 `150ms`，不能取得同一把锁。
4. owner 主动释放后 key 被删除。
5. contender 随后能够取得同一把锁。

## 固定租期场景

`shouldReleaseTheResourceWhenFixedLeaseExpires` 使用：

```text
spring.application.name = spring-integration-fixed-lease-lock-test
provider                = spring-integration
lease-time              = 1s
auto-renewal            = false
scene                   = settle-order
key                     = 2001
```

物理 key 为：

```text
spring-integration-fixed-lease-lock-test:settle-order:2001
```

owner 持续执行但不主动释放，Redis key 应在约 `1s` 后消失。contender 随后能够取得锁；原 owner 再执行 unlock 时会收到锁已过期异常，表明临界资源保护已经失效，而不是静默成功。

## 手工观测

### 1. Debug 自动续期

在自动续期用例首次执行 `assertKeyWithPositiveTtl` 后设置断点。断点必须配置为仅挂起当前线程，不要挂起全部线程，否则续期调度线程也会停止。

Debug 运行测试，停住后执行：

```bash
docker ps --filter ancestor=redis:8.4.4
docker exec -it <container-id> redis-cli
```

在 `redis-cli` 中重复执行：

```redis
EXISTS spring-integration-auto-renewal-lock-test:create-order:1001
TYPE spring-integration-auto-renewal-lock-test:create-order:1001
GET spring-integration-auto-renewal-lock-test:create-order:1001
PTTL spring-integration-auto-renewal-lock-test:create-order:1001
```

预期观察到：

- `EXISTS` 始终为 `1`。
- key 类型为 `string`。
- `PTTL` 在接近零前被刷新，持续保持为正数。
- key 存在时间明显超过配置的 `1s`。

继续运行测试后，owner 释放锁；再次执行 `EXISTS` 应返回 `0`。

### 2. Debug 固定租期

在固定租期用例首次执行 `assertKeyWithPositiveTtl` 后设置仅挂起当前线程的断点，然后重复执行：

```redis
PTTL spring-integration-fixed-lease-lock-test:settle-order:2001
EXISTS spring-integration-fixed-lease-lock-test:settle-order:2001
```

预期 `PTTL` 持续下降，不会回升；约 `1s` 后返回 `-2`，`EXISTS` 返回 `0`。继续运行测试后，contender 应成功取得锁。

## 判定结果

测试日志包含实际观测值，例如：

```text
Observed Spring Integration renewing lock: key=..., ttl=833ms
Observed Spring Integration fixed lock expiry: key=..., leaseTime=1000ms
```

自动断言和 Redis 手工观测共同证明 Spring Integration 实现符合约定。
