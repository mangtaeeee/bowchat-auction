package com.example.bowchat.chatmessage.controller;

import com.example.bowchat.chatmessage.dto.ChatMessageDTO;
import com.example.bowchat.chatmessage.service.ChatProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatSendController {

    private final ChatProducer chatProducer;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO message) {
        chatProducer.sendMessage(message);
    }

}
