package com.cloud.framework.starter.persistence.jpa;

import com.cloud.framework.persistence.PersistenceProperties;
import com.cloud.framework.starter.persistence.jpa.naming.PersistencePhysicalNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(HibernatePropertiesCustomizer.class)
public class PersistenceJpaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "framework.persistence")
    public PersistenceProperties persistenceProperties() {
        return new PersistenceProperties();
    }

    @Bean
    public HibernatePropertiesCustomizer persistenceHibernatePropertiesCustomizer(
            PersistenceProperties properties
    ) {
        return hibernateProperties -> hibernateProperties.put(
                AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                new PersistencePhysicalNamingStrategy(properties.getNaming().getTablePrefix(), properties.getNaming().getTableSuffix())
        );
    }
}
