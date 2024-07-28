package com.skyhorsemanpower.auction.kafka.data.dto;

import com.skyhorsemanpower.auction.status.AuctionStateEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class AuctionStartDto {
    private String auctionUuid;
    private AuctionStateEnum auctionState;

    @Builder
    public AuctionStartDto(String auctionUuid) {
        this.auctionUuid = auctionUuid;
        this.auctionState = AuctionStateEnum.AUCTION_IS_IN_PROGRESS;
    }
}
