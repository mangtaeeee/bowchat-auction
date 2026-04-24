package com.example.chatservice.chatroom.service.manager;

import com.example.chatservice.chatroom.client.ProductServiceClient;
import com.example.chatservice.chatroom.client.dto.ProductInfo;
import com.example.chatservice.chatroom.dto.request.ProductChatRoomEnterRequest;
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

        ProductInfo product;
        try {
            product = productServiceClient.getProduct(productId);
        } catch (FeignException.NotFound e) {
            throw new ChatException(ChatErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            throw new ChatException(ChatErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }

        if (product.sellerId().equals(buyerId)) {
            throw new ChatException(ChatErrorCode.OWN_PRODUCT_CHAT_NOT_ALLOWED);
        }

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
