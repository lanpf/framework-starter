package com.cloud.framework.starter.message.rabbitmq.partitioned.listener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PartitionedRabbitListener {

    String destination();

    String containerFactory() default "";
}
