package com.example.bowchat.chatroom.strategy;

import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.entity.SaleType;
import com.example.bowchat.product.service.ProductService;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ProductChatRoomCreator implements ChatRoomCreator<Long> {
    private final ChatRoomRepository chatRoomRepository;
    private final ProductService productService;

    @Override
    public ChatRoomType roomType() {
        return ChatRoomType.DIRECT;
    }

    @Override
    @Transactional
    public ChatRoomResponse createOrGet(Long productId, User buyer) {
        Product product = productService.getProduct(productId);

        if (product.getSaleType() == SaleType.AUCTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경매 상품에 대해 1:1 채팅방을 생성할 수 없습니다.");
        }

        return chatRoomRepository.findByTypeAndProduct_IdAndParticipants_User_Id(roomType(), productId, buyer.getId())
                .map(ChatRoomResponse::from)
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .name(product.getName())
                            .type(roomType())
                            .product(product)
                            .owner(product.getSeller())
                            .build();
                    room.registerOwner(product.getSeller());
                    room.addOrActivateMember(buyer);
                    return ChatRoomResponse.from(chatRoomRepository.save(room));
                });
    }
}