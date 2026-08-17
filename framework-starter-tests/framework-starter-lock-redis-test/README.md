# Redis Lock 集成测试

本模块使用真实 Redis 验证 `framework-starter-lock-redis` 的两种实现：

- `SpringIntegrationRedisLockIT`：Spring Integration `RedisLockRegistry`。
- `RedissonRedisLockIT`：Redisson `RLock`。

每种实现都验证：

- 两个独立 Spring ApplicationContext 竞争相同资源时保持互斥。
- `auto-renewal=true` 时，持锁时间超过 `lease-time` 后锁仍然有效。
- 主动释放后，竞争者能够取得相同资源。
- `auto-renewal=false` 时，锁在固定租期结束后自动释放。
- Redis 中的物理 key 为 `namespace:scene:key`。

## 环境要求

- JDK 17 或更高版本。
- Maven 3.9 或更高版本。
- Docker Desktop、OrbStack 或其他兼容 Docker API 的容器运行环境。
- Docker daemon 已启动，当前用户能够执行 `docker info`。

测试使用 Testcontainers 创建并销毁 `redis:8.4.4`，不要求本机安装 Redis。

## 自动执行

在 `framework-starter` 根目录执行：

```bash
mvn verify -pl :framework-starter-lock-redis-test -am
```

预期结果为：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

测试报告位于：

```text
framework-starter-tests/framework-starter-lock-redis-test/
└── target/failsafe-reports/
    ├── com.cloud.framework.starter.lock.redis.test.SpringIntegrationRedisLockIT.txt
    ├── TEST-com.cloud.framework.starter.lock.redis.test.SpringIntegrationRedisLockIT.xml
    ├── com.cloud.framework.starter.lock.redis.test.RedissonRedisLockIT.txt
    └── TEST-com.cloud.framework.starter.lock.redis.test.RedissonRedisLockIT.xml
```

## 测试文档

- [SpringIntegrationRedisLockIT](docs/SpringIntegrationRedisLockIT.md)
- [RedissonRedisLockIT](docs/RedissonRedisLockIT.md)
