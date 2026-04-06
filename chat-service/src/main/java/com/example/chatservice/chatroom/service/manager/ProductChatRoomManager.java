package com.example.chatservice.chatroom.service.manager;

import com.example.chatservice.chatroom.client.ProductServiceClient;
import com.example.chatservice.chatroom.client.dto.ProductInfo;
import com.example.chatservice.chatroom.dto.request.ProductChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import com.example.chatservice.chatroom.service.ChatRoomManager;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductChatRoomManager implements ChatRoomManager<ProductChatRoomEnterRequest> {

    private final ChatRoomRepository chatRoomRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    public ChatRoomType supportType() {
        return ChatRoomType.DIRECT;
    }

    @Override
    public Class<ProductChatRoomEnterRequest> requestType() {
        return ProductChatRoomEnterRequest.class;
    }

    @Override
    @Transactional
    public EnterChatResponse enterChatRoom(ProductChatRoomEnterRequest request, Long buyerId) {
        Long productId = request.getProductId();

        // 상품 정보 조회 (존재 여부 + 판매자 확인)
        ProductInfo product;
        try {
            product = productServiceClient.getProduct(productId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다.");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "상품 서비스에 접근할 수 없습니다.");
        }

        // 판매자가 자신의 상품에 채팅방 생성 방지
        if (product.sellerId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 상품에는 채팅방을 생성할 수 없습니다.");
        }

        // 기존 채팅방 있으면 입장, 없으면 생성
        ChatRoom chatRoom = chatRoomRepository
                .findByTypeAndProductAndUserIdWithParticipants(ChatRoomType.DIRECT, productId, buyerId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .name(product.name())
                            .type(ChatRoomType.DIRECT)
                            .product(productId)
                            .owner(buyerId)
                            .build();
                    newRoom.registerSeller(product.sellerId());
                    newRoom.addOrActivateMember(buyerId);
                    return chatRoomRepository.save(newRoom);
                });

        log.info("상품 채팅방 입장: roomId={}, buyerId={}", chatRoom.getId(), buyerId);
        return new EnterChatResponse(chatRoom.getId(), ChatRoomType.DIRECT, chatRoom.getName());
    }
}
