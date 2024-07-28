package com.skyhorsemanpower.auction.data.vo;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
public class OfferBiddingPriceRequestVo {
    private String auctionUuid;
    private BigDecimal biddingPrice;
    private int round;
}
