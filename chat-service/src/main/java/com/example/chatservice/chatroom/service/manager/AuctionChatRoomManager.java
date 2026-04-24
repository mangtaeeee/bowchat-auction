package com.example.chatservice.chatroom.service.manager;

import com.example.chatservice.chatroom.client.AuctionServiceClient;
import com.example.chatservice.chatroom.client.dto.AuctionInfo;
import com.example.chatservice.chatroom.dto.request.AuctionChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import com.example.chatservice.chatroom.service.ChatRoomManager;
import com.example.chatservice.exception.ChatErrorCode;
import com.example.chatservice.exception.ChatException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionChatRoomManager implements ChatRoomManager<AuctionChatRoomEnterRequest> {

    private final ChatRoomRepository chatRoomRepository;
    private final AuctionServiceClient auctionServiceClient;

    @Override
    public ChatRoomType supportType() {
        return ChatRoomType.AUCTION;
    }

    @Override
    public Class<AuctionChatRoomEnterRequest> requestType() {
        return AuctionChatRoomEnterRequest.class;
    }

    @Override
    @Transactional
    public EnterChatResponse enterChatRoom(AuctionChatRoomEnterRequest request, Long userId) {
        Long productId = request.getProductId();

        AuctionInfo auction;
        try {
            auction = auctionServiceClient.getAuctionByProductId(productId);
        } catch (FeignException.NotFound e) {
            throw new ChatException(ChatErrorCode.AUCTION_NOT_FOUND);
        } catch (FeignException e) {
            throw new ChatException(ChatErrorCode.AUCTION_SERVICE_UNAVAILABLE);
        }

        if (auction.closed()) {
            throw new ChatException(ChatErrorCode.AUCTION_CLOSED);
        }

        ChatRoom chatRoom = chatRoomRepository.findByTypeAndProductWithParticipants(ChatRoomType.AUCTION, productId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .name(productId + "번 상품 경매 채팅방")
                            .type(ChatRoomType.AUCTION)
                            .product(productId)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        chatRoom.addOrActivateMember(userId);

        log.info("경매 채팅방 입장: roomId={}, userId={}", chatRoom.getId(), userId);
        return new EnterChatResponse(chatRoom.getId(), ChatRoomType.AUCTION, chatRoom.getName());
    }
}
