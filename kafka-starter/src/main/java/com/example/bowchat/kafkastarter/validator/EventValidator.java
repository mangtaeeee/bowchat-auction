package com.example.bowchat.kafkastarter.validator;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;

public interface EventValidator {
    MessageType getMessageType();
    void validate(EventMessage chatEvent);
}
