package com.cloud.framework.starter.lock.redis.springintegration;

import com.cloud.framework.core.naming.NamespaceResolver;
import com.cloud.framework.core.naming.PassthroughResourceNameResolver;
import com.cloud.framework.core.naming.ResourceNameResolver;
import com.cloud.framework.lock.LockProvider;
import com.cloud.framework.starter.lock.redis.RedisLockProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static com.cloud.framework.starter.lock.redis.RedisLockAutoConfiguration.LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisLockRegistry.class)
@ConditionalOnProperty(
        prefix = "framework.lock.redis",
        name = "provider",
        havingValue = "spring-integration",
        matchIfMissing = true
)
public class SpringIntegrationLockAutoConfiguration {

    public static final String RENEWAL_TASK_SCHEDULER_BEAN_NAME = "redisLockRenewalTaskScheduler";

    @Bean(name = LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(name = LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME)
    public ResourceNameResolver lockResourceNameResolver() {
        return new PassthroughResourceNameResolver();
    }

    @Bean(name = RENEWAL_TASK_SCHEDULER_BEAN_NAME, defaultCandidate = false)
    @ConditionalOnMissingBean(name = RENEWAL_TASK_SCHEDULER_BEAN_NAME)
    @ConditionalOnProperty(
            prefix = "framework.lock.redis",
            name = "auto-renewal",
            havingValue = "true",
            matchIfMissing = true
    )
    public ThreadPoolTaskScheduler redisLockRenewalTaskScheduler(
            RedisLockProperties properties,
            ObjectProvider<ThreadPoolTaskSchedulerBuilder> taskSchedulerBuilder
    ) {
        RedisLockProperties.Renewal renewal = properties.getRenewal();
        TaskExecutionProperties.Pool pool = renewal.getPool();
        return taskSchedulerBuilder.getIfUnique(ThreadPoolTaskSchedulerBuilder::new)
                .poolSize(pool.getCoreSize())
                .threadNamePrefix(renewal.getThreadNamePrefix())
                .additionalCustomizers(taskScheduler -> {
                    taskScheduler.setAcceptTasksAfterContextClose(
                            pool.getShutdown().isAcceptTasksAfterContextClose()
                    );
                    taskScheduler.setRemoveOnCancelPolicy(renewal.isRemoveOnCancelPolicy());
                })
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisLockRegistry redisLockRegistry(
            RedisConnectionFactory connectionFactory,
            RedisLockProperties properties,
            @Qualifier(RENEWAL_TASK_SCHEDULER_BEAN_NAME) ObjectProvider<TaskScheduler> renewalTaskScheduler,
            NamespaceResolver namespaceResolver
    ) {
        String namespace = namespaceResolver.resolve(properties);
        RedisLockRegistry lockRegistry = new RedisLockRegistry(
                connectionFactory,
                namespace,
                properties.getLeaseTime().toMillis()
        );
        if (properties.isAutoRenewal()) {
            lockRegistry.setRenewalTaskScheduler(renewalTaskScheduler.getObject());
        }
        return lockRegistry;
    }

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider springIntegrationLockProvider(
            RedisLockRegistry lockRegistry,
            @Qualifier(LOCK_RESOURCE_NAME_RESOLVER_BEAN_NAME) ResourceNameResolver resourceNameResolver
    ) {
        return new SpringIntegrationLockProviderAdapter(lockRegistry, resourceNameResolver);
    }
}
