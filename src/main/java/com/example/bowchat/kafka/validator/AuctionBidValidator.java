package com.example.bowchat.kafka.validator;

import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.entity.AuctionErrorCode;
import com.example.bowchat.auction.entity.AuctionException;
import com.example.bowchat.auction.repository.AuctionRepository;
import com.example.bowchat.chatmessage.entity.MessageType;
import com.example.bowchat.kafka.ChatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AuctionBidValidator implements ChatEventValidator {

    private final AuctionRepository auctionRepository;

    @Override
    public MessageType getMessageType() {
        return MessageType.AUCTION_BID;
    }

    @Override
    public void validate(ChatEvent chatEvent) {
        Long auctionId = chatEvent.roomId();
        Long userId    = chatEvent.senderId();
        Long amount    = Long.valueOf(chatEvent.content());

        Auction auction = auctionRepository.findWithProductAndSellerById(auctionId)
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (amount <= 0) {
            throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
        }

        auction.validateBid(userId, amount);
    }
}
