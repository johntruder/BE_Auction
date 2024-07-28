package com.skyhorsemanpower.auction.data.projection;

import lombok.*;

@Getter
@NoArgsConstructor
@ToString
public class CheckBiddingPriceProjection {
    private int biddingPrice;

    @Builder
    public CheckBiddingPriceProjection(int biddingPrice) {
        this.biddingPrice = biddingPrice;
    }
}
