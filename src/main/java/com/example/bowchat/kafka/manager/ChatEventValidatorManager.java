package com.example.bowchat.kafka.manager;

import com.example.bowchat.chatmessage.entity.MessageType;
import com.example.bowchat.kafka.ChatEvent;
import com.example.bowchat.kafka.validator.ChatEventValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ChatEventValidatorManager {


    private final Map<MessageType, ChatEventValidator> validatorMap;

    public ChatEventValidatorManager(List<ChatEventValidator> validators) {
        this.validatorMap = validators.stream()
                .collect(java.util.stream.Collectors.toMap(ChatEventValidator::getMessageType, v -> v));
    }

    public void validate(ChatEvent event) {
        ChatEventValidator validator = validatorMap.get(event.type());
        if (validator != null) {
            validator.validate(event); // 예외 발생 시 WebSocket에서 catch
        }
    }
}
