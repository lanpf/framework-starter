package com.cloud.framework.starter.scheduler.xxljob;

import com.cloud.framework.core.naming.NamespaceResolver;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(name = "com.cloud.framework.starter.autoconfigure.naming.NamespaceAutoConfiguration")
@ConditionalOnClass(XxlJobSpringExecutor.class)
@Import(XxlJobSchedulerAutoConfiguration.EnabledXxlJobConfiguration.class)
public class XxlJobSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "scheduler.xxljob")
    public XxlJobSchedulerProperties xxlJobSchedulerProperties() {
        return new XxlJobSchedulerProperties();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "scheduler.xxljob", name = "enabled", havingValue = "true")
    static class EnabledXxlJobConfiguration {

        @Bean
        @ConditionalOnMissingBean
        XxlJobSpringExecutor xxlJobSpringExecutor(
                XxlJobSchedulerProperties properties,
                NamespaceResolver namespaceResolver
        ) {
            XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
            executor.setEnabled(properties.isEnabled());
            executor.setAdminAddresses(properties.getAdminAddresses());
            executor.setAccessToken(properties.getAccessToken());
            executor.setTimeout(properties.getTimeout());
            executor.setAppname(namespaceResolver.resolve(properties));
            executor.setAddress(properties.getAddress());
            executor.setIp(properties.getIp());
            executor.setPort(properties.getPort());
            executor.setLogPath(properties.getLogPath());
            executor.setLogRetentionDays(properties.getLogRetentionDays());
            return executor;
        }
    }
}
