package com.cloud.framework.starter.id.cosid;

import com.cloud.framework.id.LongIdGenerator;
import com.cloud.framework.starter.id.cosid.adapter.CosIdLongIdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "me.ahoo.cosid.spring.boot.starter.CosIdAutoConfiguration")
@ConditionalOnClass(IdGeneratorProvider.class)
public class CosIdIdGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnBean(IdGeneratorProvider.class)
    @ConditionalOnMissingBean(LongIdGenerator.class)
    public LongIdGenerator longIdGenerator(IdGeneratorProvider idGeneratorProvider) {
        return new CosIdLongIdGenerator(idGeneratorProvider);
    }
}
