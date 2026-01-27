package com.example.bowchat.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.server.ResponseStatusException;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

        // DLQ로 메시지 전송하는 Recoverer
        DefaultErrorHandler errorHandler = getDefaultErrorHandler(kafkaTemplate);

        // 재시도하지 않을 예외 지정
        errorHandler.addNotRetryableExceptions(
                ResponseStatusException.class,  // 비즈니스 검증 실패
                IllegalArgumentException.class  // 잘못된 파라미터
        );

        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("Kafka 재시도 {}/2회: key={}, exception={}",
                        attempt, record.key(), ex.getClass().getSimpleName()));

        return errorHandler;
    }

    private static DefaultErrorHandler getDefaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.error("DLQ 전송: topic={}, key={}, reason={}",
                            dltTopic, record.key(), ex.getClass().getSimpleName());
                    return new TopicPartition(dltTopic, 0);
                }
        );

        // 재시도 설정: 0ms 간격으로 2번 재시도
        FixedBackOff backOff = new FixedBackOff(0L, 2);
        return new DefaultErrorHandler(recoverer, backOff);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> cf,
            DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

}
