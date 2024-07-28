package com.skyhorsemanpower.auction.domain;

import com.skyhorsemanpower.auction.data.dto.OfferBiddingPriceDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@ToString
@Document(collection = "auction_history")
public class AuctionHistory {
    @Id
    private String auctionHistoryId;

    private String auctionUuid;
    private String biddingUuid;
    private BigDecimal biddingPrice;
    private LocalDateTime biddingTime;
    private Integer round;

    @Builder
    public AuctionHistory(String auctionUuid, String biddingUuid, BigDecimal biddingPrice,
                          LocalDateTime biddingTime, Integer round) {
        this.auctionUuid = auctionUuid;
        this.biddingUuid = biddingUuid;
        this.biddingPrice = biddingPrice;
        this.biddingTime = biddingTime;
        this.round = round;
    }

    public static AuctionHistory converter(OfferBiddingPriceDto offerBiddingPriceDto) {
        return AuctionHistory.builder()
                .auctionUuid(offerBiddingPriceDto.getAuctionUuid())
                .biddingUuid(offerBiddingPriceDto.getBiddingUuid())
                .biddingPrice(offerBiddingPriceDto.getBiddingPrice())
                .biddingTime(LocalDateTime.now())
                .round(offerBiddingPriceDto.getRound())
                .build();
    }
}
