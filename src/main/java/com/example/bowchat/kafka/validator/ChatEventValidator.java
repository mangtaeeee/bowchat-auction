package com.example.bowchat.kafka.validator;

import com.example.bowchat.chatmessage.entity.MessageType;
import com.example.bowchat.kafka.ChatEvent;

public interface ChatEventValidator {
    MessageType getMessageType();
    void validate(ChatEvent chatEvent);
}
