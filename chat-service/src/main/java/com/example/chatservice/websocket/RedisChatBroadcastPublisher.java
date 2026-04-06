package com.example.chatservice.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatBroadcastPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic chatBroadcastTopic;

    public void publish(String payload) {
        stringRedisTemplate.convertAndSend(chatBroadcastTopic.getTopic(), payload);
    }
}
