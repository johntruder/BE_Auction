package com.skyhorsemanpower.auction.kafka.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@ToString
public class InitialAuctionDto {
    private String auctionUuid;
    private BigDecimal startPrice;
    private int numberOfEventParticipants;
    private long auctionStartTime;
    private long auctionEndTime;
    private BigDecimal incrementUnit;

    @Builder
    public InitialAuctionDto(String auctionUuid, BigDecimal startPrice, int numberOfEventParticipants,
                             long auctionStartTime, long auctionEndTime, BigDecimal incrementUnit) {
        this.auctionUuid = auctionUuid;
        this.startPrice = startPrice;
        this.numberOfEventParticipants = numberOfEventParticipants;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
        this.incrementUnit = incrementUnit;
    }
}
