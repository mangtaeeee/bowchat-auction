package com.example.bowchat.kafkastarter.config;

import com.example.bowchat.kafkastarter.producer.EventProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

@AutoConfiguration
@EnableConfigurationProperties(BowchatKafkaProperties.class)
public class BowchatKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaErrorHandlerFactory kafkaErrorHandlerFactory() {
        return new KafkaErrorHandlerFactory();
    }

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    public DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            KafkaErrorHandlerFactory factory,
            BowchatKafkaProperties properties
    ) {
        return factory.create(kafkaTemplate, properties);
    }

    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler defaultErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventProducer eventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new EventProducer(kafkaTemplate);
    }
}