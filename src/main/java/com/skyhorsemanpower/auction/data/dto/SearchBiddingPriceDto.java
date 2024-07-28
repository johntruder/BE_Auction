package com.skyhorsemanpower.auction.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SearchBiddingPriceDto {
    private String auctionUuid;

    @Builder
    public SearchBiddingPriceDto(String auctionUuid) {
        this.auctionUuid = auctionUuid;
    }
}
