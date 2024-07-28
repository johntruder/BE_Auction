package com.skyhorsemanpower.auction.data.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AuctionResultResponseVo {
    private boolean isBidder;
    private BigDecimal price;

    @Builder
    public AuctionResultResponseVo(boolean isBidder, BigDecimal price) {
        this.isBidder = isBidder;
        this.price = price;
    }

    public static AuctionResultResponseVo notBidder() {
        return AuctionResultResponseVo.builder()
                .isBidder(false)
                .price(BigDecimal.ZERO)
                .build();
    }
}
