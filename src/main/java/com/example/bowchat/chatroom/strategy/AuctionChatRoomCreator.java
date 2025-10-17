package com.example.bowchat.chatroom.strategy;

import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.repository.AuctionRepository;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuctionChatRoomCreator implements ChatRoomCreator<Long> {
    private final ChatRoomRepository repo;
    private final AuctionRepository auctionRepository;

    @Override
    public ChatRoomType roomType() {
        return ChatRoomType.AUCTION;
    }


    @Override
    @Transactional
    public ChatRoomResponse createOrGet(Long productId, User user) {
        Auction auction = auctionRepository.findByProductId(productId)
                .orElseThrow( ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

        // 이미 만들어진 채팅방이 있으면 리턴
        return repo.findByTypeAndProduct_Id(roomType(), auction.getProduct().getId())
                .map(ChatRoomResponse::from)
                .orElseGet(() -> {
                    // 새 방 생성
                    ChatRoom room = ChatRoom.builder()
                            .name(auction.getProduct().getName())
                            .type(roomType())
                            .product(auction.getProduct())
                            .owner(auction.getProduct().getSeller())
                            .build();
                    room.registerSeller(auction.getProduct().getSeller());
                    return ChatRoomResponse.from(repo.save(room));
                });
    }
}