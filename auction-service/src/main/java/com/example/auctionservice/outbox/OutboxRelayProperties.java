package com.example.auctionservice.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "outbox.relay")
@Getter
@Setter
public class OutboxRelayProperties {

    private int batchSize = 50;
    private long fixedDelayMs = 2000;
    private long retryDelaySeconds = 5;
    private long staleThresholdSeconds = 30;
}
