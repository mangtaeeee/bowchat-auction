package com.example.bowchat.kafkastarter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bowchat.kafka")
@Getter @Setter
public class BowchatKafkaProperties {

    private long backOffInterval = 0L;
    private long maxRetries = 2L;
    private String dltSuffix = ".DLT";
}
