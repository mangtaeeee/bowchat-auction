package com.example.chatservice.refact;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {


    @PostMapping("/enter")
    public ResponseEntity<String>(@RequestBody ChatRoomEnterRequest request){
        if (request instanceof AuctionChatRoomEnterRequest) {

        }

    }
}
