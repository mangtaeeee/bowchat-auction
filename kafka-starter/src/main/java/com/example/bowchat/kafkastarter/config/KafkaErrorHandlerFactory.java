package com.example.bowchat.kafkastarter.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
public class KafkaErrorHandlerFactory {

    public DefaultErrorHandler create(
            KafkaTemplate<Object, Object> kafkaTemplate,
            BowchatKafkaProperties properties
    ) {
        DefaultErrorHandler errorHandler = getDefaultErrorHandler(kafkaTemplate, properties);

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("Kafka 재시도 {}/{}회: key={}, exception={}",
                        attempt,
                        properties.getMaxRetries(),
                        record.key(),
                        ex.getClass().getSimpleName()));

        return errorHandler;
    }

    private static @NonNull DefaultErrorHandler getDefaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate, BowchatKafkaProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    String dltTopic = record.topic() + properties.getDltSuffix();
                    log.error("DLQ 전송: topic={}, key={}, reason={}",
                            dltTopic, record.key(), ex.getClass().getSimpleName());
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        FixedBackOff backOff =
                new FixedBackOff(properties.getBackOffInterval(), properties.getMaxRetries());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        return errorHandler;
    }
}