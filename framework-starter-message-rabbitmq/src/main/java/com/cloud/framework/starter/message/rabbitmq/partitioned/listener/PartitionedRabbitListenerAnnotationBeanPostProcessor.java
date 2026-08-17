package com.cloud.framework.starter.message.rabbitmq.partitioned.listener;

import com.cloud.framework.starter.message.rabbitmq.partitioned.PartitionedRabbitTopologyRegistry;
import com.cloud.framework.starter.message.rabbitmq.partitioned.support.PartitionedRabbitMessageSupport;
import com.cloud.framework.starter.message.rabbitmq.topology.RabbitTopologyInitializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.adapter.AmqpMessageHandlerMethodFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
public class PartitionedRabbitListenerAnnotationBeanPostProcessor
        implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

    public static final String PARTITIONED_CONTAINER_FACTORY_BEAN_NAME = "partitionedRabbitListenerContainerFactory";

    private final Lock endpointRegistrarLock = new ReentrantLock();

    private final ObjectProvider<RabbitListenerEndpointRegistry> endpointRegistry;

    private final ObjectProvider<MessageConverter> messageConverter;

    private final ObjectProvider<ConversionService> conversionService;

    private final ObjectProvider<Validator> validator;

    private final ObjectProvider<PartitionedRabbitTopologyRegistry> topologyRegistry;

    private final ObjectProvider<RabbitTopologyInitializer> topologyInitializer;


    private final ConcurrentMap<Class<?>, Map<Method, PartitionedRabbitListener>> annotatedMethodsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ListenerMetadata> metadataCache = new ConcurrentHashMap<>();

    private volatile boolean registerImmediately;

    private volatile MessageHandlerMethodFactory messageHandlerMethodFactory;

    @Setter
    private BeanFactory beanFactory;

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Map<Method, PartitionedRabbitListener> annotatedMethods = this.annotatedMethodsCache.computeIfAbsent(targetClass, this::buildListenerMethods);
        annotatedMethods.forEach((method, annotation) -> processPartitionedRabbitListener(annotation, method, bean, beanName));
        log.debug("Processed {} @PartitionedRabbitListener methods on bean '{}'", annotatedMethods.size(), beanName);
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.topologyInitializer.ifAvailable(RabbitTopologyInitializer::initialize);
        this.metadataCache.values().forEach(this::registerListenerContainers);
        this.registerImmediately = true;
        this.annotatedMethodsCache.clear();
    }

    private Map<Method, PartitionedRabbitListener> buildListenerMethods(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(
                targetClass,
                (MethodIntrospector.MetadataLookup<PartitionedRabbitListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, PartitionedRabbitListener.class)
        );
    }

    private void processPartitionedRabbitListener(
            PartitionedRabbitListener annotation,
            Method method,
            Object bean,
            String beanName
    ) {
        validate(annotation, method);
        ListenerMetadata metadata = new ListenerMetadata(
                annotation.destination(),
                resolveContainerFactoryName(annotation.containerFactory()),
                beanName,
                bean,
                AopUtils.selectInvocableMethod(method, bean.getClass())
        );

        ListenerMetadata existsMetadata = this.metadataCache.putIfAbsent(metadata.getDestination(), metadata);
        if (existsMetadata == null) {
            if (this.registerImmediately) {
                registerListenerContainers(metadata);
            }
            return;
        }

        log.warn(
                "Ignored @PartitionedRabbitListener {}#{}: same destination '{}' with {}#{}, only the first one will be registered",
                metadata.getBeanName(),
                metadata.getMethod().getName(),
                metadata.getDestination(),
                existsMetadata.getBeanName(),
                existsMetadata.getMethod().getName());
    }

    private void registerListenerContainers(ListenerMetadata metadata) {
        for (int index = 0; index < this.topologyRegistry.getObject().partitions(metadata.getDestination()); index++) {
            registerListenerContainer(metadata, index);
        }
    }

    private void registerListenerContainer(ListenerMetadata metadata, Integer index) {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        endpoint.setMethod(metadata.getMethod());
        endpoint.setBean(metadata.getBean());
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
        endpoint.setId(getEndpointId(metadata, index));
        endpoint.setBeanFactory(this.beanFactory);
        endpoint.setQueueNames(PartitionedRabbitMessageSupport.queue(metadata.getDestination(), index));

        this.endpointRegistrarLock.lock();
        try {
            this.endpointRegistry.getObject().registerListenerContainer(
                    endpoint,
                    resolveContainerFactory(metadata.getContainerFactoryBeanName()),
                    true
            );
        } finally {
            this.endpointRegistrarLock.unlock();
        }
    }

    private void validate(PartitionedRabbitListener annotation, Method method) {
        if (!StringUtils.hasText(annotation.destination())) {
            throw new IllegalStateException("@PartitionedRabbitListener destination must not be blank on " + method);
        }
    }

    private String resolveContainerFactoryName(String containerFactoryBeanName) {
        if (StringUtils.hasText(containerFactoryBeanName)) {
            return containerFactoryBeanName;
        }
        return PARTITIONED_CONTAINER_FACTORY_BEAN_NAME;
    }


    private RabbitListenerContainerFactory<?> resolveContainerFactory(String containerFactoryBeanName) {
        if (this.beanFactory.containsBean(containerFactoryBeanName)) {
            return this.beanFactory.getBean(containerFactoryBeanName, RabbitListenerContainerFactory.class);
        }

        throw new IllegalStateException("No partitioned RabbitListenerContainerFactory available for " + containerFactoryBeanName);
    }

    private MessageHandlerMethodFactory messageHandlerMethodFactory() {
        MessageHandlerMethodFactory factory = this.messageHandlerMethodFactory;
        if (factory != null) {
            return factory;
        }
        synchronized (this) {
            if (this.messageHandlerMethodFactory == null) {
                AmqpMessageHandlerMethodFactory defaultFactory = new AmqpMessageHandlerMethodFactory();
                defaultFactory.setBeanFactory(this.beanFactory);
                this.messageConverter.ifAvailable(defaultFactory::setMessageConverter);
                this.conversionService.ifUnique(defaultFactory::setConversionService);
                this.validator.ifUnique(defaultFactory::setValidator);
                defaultFactory.afterPropertiesSet();
                this.messageHandlerMethodFactory = defaultFactory;
            }
            return this.messageHandlerMethodFactory;
        }
    }

    private String getEndpointId(ListenerMetadata metadata, Integer index) {
        return metadata.getBeanName() + "#" + metadata.getMethod().getName() + ":" + metadata.getDestination() + "." + index;
    }

    @Getter
    @RequiredArgsConstructor
    private static class ListenerMetadata {
        private final String destination;

        private final String containerFactoryBeanName;

        private final String beanName;

        private final Object bean;

        private final Method method;
    }
}
