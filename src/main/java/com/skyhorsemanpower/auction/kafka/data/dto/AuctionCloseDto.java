package com.skyhorsemanpower.auction.kafka.data.dto;

import com.skyhorsemanpower.auction.status.AuctionStateEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class AuctionCloseDto {
    private String auctionUuid;
    private AuctionStateEnum auctionState;
    private List<String> memberUuids;
    private BigDecimal price;

    @Builder
    public AuctionCloseDto(String auctionUuid, AuctionStateEnum auctionState,
                           List<String> memberUuids, BigDecimal price) {
        this.auctionUuid = auctionUuid;
        this.auctionState = auctionState;
        this.memberUuids = memberUuids;
        this.price = price;
    }
}
