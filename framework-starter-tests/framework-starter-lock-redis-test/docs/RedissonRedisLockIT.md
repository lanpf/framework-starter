# RedissonRedisLockIT

## 测试目标

该测试使用两个独立 Spring ApplicationContext 和同一个真实 Redis，验证 Redisson `RLock` 的互斥、watchdog 自动续期、固定租期和资源命名。

测试类：

```text
src/test/java/com/cloud/framework/starter/lock/redis/test/RedissonRedisLockIT.java
```

单独执行：

```bash
mvn verify -pl :framework-starter-lock-redis-test -am \
  -Dit.test=RedissonRedisLockIT
```

## 自动续期场景

`shouldProvideMutualExclusionAndAutomaticRenewal` 使用：

```text
spring.application.name = redisson-auto-renewal-lock-test
provider                = redisson
lease-time              = 1s
auto-renewal            = true
scene                   = create-order
key                     = 1001
```

starter 将 Redisson `lockWatchdogTimeout` 对齐为 `lease-time`。Redis 物理 key 必须为：

```text
redisson-auto-renewal-lock-test:create-order:1001
```

测试执行过程：

1. owner context 在独立线程中取得锁。
2. 连续观察超过 `1.5s`，key 始终存在且 TTL 为正，证明 Redisson watchdog 正在续期。
3. contender context 最多等待 `150ms`，不能取得同一把锁。
4. owner 主动释放后 key 被删除。
5. contender 随后能够取得同一把锁。

## 固定租期场景

`shouldReleaseTheResourceWhenFixedLeaseExpires` 使用：

```text
spring.application.name = redisson-fixed-lease-lock-test
provider                = redisson
lease-time              = 1s
auto-renewal            = false
scene                   = settle-order
key                     = 2001
```

物理 key 为：

```text
redisson-fixed-lease-lock-test:settle-order:2001
```

owner 持续执行但不主动释放，Redis key 应在约 `1s` 后消失。contender 随后能够取得锁；原 owner 再执行 unlock 时会收到非锁持有者异常，避免把已经失效的临界区误判为正常完成。

## 手工观测

### 1. Debug 自动续期

在自动续期用例首次执行 `assertKeyWithPositiveTtl` 后设置断点。断点必须配置为仅挂起当前线程，保证 Redisson watchdog 线程继续运行。

Debug 运行测试，停住后执行：

```bash
docker ps --filter ancestor=redis:8.4.4
docker exec -it <container-id> redis-cli
```

在 `redis-cli` 中重复执行：

```redis
EXISTS redisson-auto-renewal-lock-test:create-order:1001
TYPE redisson-auto-renewal-lock-test:create-order:1001
HGETALL redisson-auto-renewal-lock-test:create-order:1001
PTTL redisson-auto-renewal-lock-test:create-order:1001
```

预期观察到：

- `EXISTS` 始终为 `1`。
- Redisson 锁的 key 类型为 `hash`。
- `PTTL` 在接近零前被 watchdog 刷新，持续保持为正数。
- key 存在时间明显超过配置的 `1s`。

继续运行测试后，owner 释放锁；再次执行 `EXISTS` 应返回 `0`。

### 2. Debug 固定租期

在固定租期用例首次执行 `assertKeyWithPositiveTtl` 后设置仅挂起当前线程的断点，然后重复执行：

```redis
PTTL redisson-fixed-lease-lock-test:settle-order:2001
EXISTS redisson-fixed-lease-lock-test:settle-order:2001
```

预期 `PTTL` 持续下降，不会回升；约 `1s` 后返回 `-2`，`EXISTS` 返回 `0`。继续运行测试后，contender 应成功取得锁。

## 判定结果

测试日志包含实际观测值，例如：

```text
Observed Redisson renewing lock: key=..., ttl=973ms
Observed Redisson fixed lock expiry: key=..., leaseTime=1000ms
```

自动断言和 Redis 手工观测共同证明 Redisson 实现符合约定。
