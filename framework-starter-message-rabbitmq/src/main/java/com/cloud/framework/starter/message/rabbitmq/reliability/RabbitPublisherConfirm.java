package com.cloud.framework.starter.message.rabbitmq.reliability;

import com.cloud.framework.kernel.error.FrameworkException;
import com.cloud.framework.message.error.MessageError;
import com.cloud.framework.starter.message.rabbitmq.RabbitMessageProperties;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;

@Validated
@RequiredArgsConstructor
public class RabbitPublisherConfirm {
    private final RabbitMessageProperties properties;

    public void send(
            @NotNull Runnable nativeSender,
            @NotNull Consumer<CorrelationData> confirmSender
    ) {
        if (this.properties.getProducer().getReliabilityMode()
                != RabbitMessageProperties.ProducerReliabilityMode.PUBLISHER_CONFIRM) {
            nativeSender.run();
            return;
        }

        CorrelationData correlationData = new CorrelationData();
        confirmSender.accept(correlationData);
        await(correlationData, this.properties.getProducer().getConfirmTimeout());
    }

    public void sendBatch(
            @NotNull RabbitTemplate rabbitTemplate,
            @NotNull Function<RabbitOperations, List<CorrelationData>> confirmSender
    ) {
        if (this.properties.getProducer().getReliabilityMode()
                != RabbitMessageProperties.ProducerReliabilityMode.PUBLISHER_CONFIRM) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_REQUIRED,
                    "Rabbit batch sending requires publisher-confirm reliability mode."
            );
        }

        rabbitTemplate.invoke(operations -> {
            List<CorrelationData> correlationDataList = confirmSender.apply(operations);
            awaitAll(correlationDataList, this.properties.getProducer().getConfirmTimeout());
            return null;
        });
    }

    private void await(CorrelationData correlationData, Duration timeout) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            verify(correlationData, confirm);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_INTERRUPTED,
                    "Interrupted while waiting for Rabbit publisher confirm.",
                    ex
            );
        } catch (TimeoutException ex) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_TIMEOUT,
                    "Rabbit publisher confirm timed out after " + timeout + ".",
                    ex
            );
        } catch (ExecutionException ex) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_FAILED,
                    "Failed to wait for Rabbit publisher confirm.",
                    ex
            );
        }
    }

    private void awaitAll(List<CorrelationData> correlationDataList, Duration timeout) {
        CompletableFuture<?>[] futures = correlationDataList.stream()
                .map(CorrelationData::getFuture)
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(futures).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            correlationDataList.forEach(correlationData ->
                    verify(correlationData, correlationData.getFuture().join())
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_INTERRUPTED,
                    "Interrupted while waiting for Rabbit batch publisher confirms.",
                    ex
            );
        } catch (TimeoutException ex) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_TIMEOUT,
                    "Rabbit batch publisher confirms timed out after " + timeout + ".",
                    ex
            );
        } catch (ExecutionException ex) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_FAILED,
                    "Failed to wait for Rabbit batch publisher confirms.",
                    ex
            );
        }
    }

    private void verify(CorrelationData correlationData, CorrelationData.Confirm confirm) {
        ReturnedMessage returnedMessage = correlationData.getReturned();
        if (returnedMessage != null) {
            throw new FrameworkException(
                    MessageError.MESSAGE_RETURNED,
                    "Rabbit message was returned. exchange=" + returnedMessage.getExchange()
                            + ", routingKey=" + returnedMessage.getRoutingKey()
                            + ", replyCode=" + returnedMessage.getReplyCode()
                            + ", replyText=" + returnedMessage.getReplyText()
            );
        }
        if (!confirm.isAck()) {
            throw new FrameworkException(
                    MessageError.PUBLISHER_CONFIRM_FAILED,
                    "Rabbit publisher confirm failed. reason=" + confirm.getReason()
            );
        }
    }
}
