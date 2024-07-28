package com.skyhorsemanpower.auction.data.dto;

import com.skyhorsemanpower.auction.data.vo.OfferBiddingPriceRequestVo;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
@NoArgsConstructor
public class OfferBiddingPriceDto {
    private String auctionUuid;
    private String biddingUuid;
    private BigDecimal biddingPrice;
    private int round;

    @Builder
    public OfferBiddingPriceDto(String auctionUuid, String biddingUuid, BigDecimal biddingPrice, int round) {
        this.auctionUuid = auctionUuid;
        this.biddingUuid = biddingUuid;
        this.biddingPrice = biddingPrice;
        this.round = round;
    }

    public static OfferBiddingPriceDto voToDto(OfferBiddingPriceRequestVo offerBiddingPriceRequestVo, String uuid) {
        return OfferBiddingPriceDto.builder()
                .auctionUuid(offerBiddingPriceRequestVo.getAuctionUuid())
                .biddingUuid(uuid)
                .biddingPrice(offerBiddingPriceRequestVo.getBiddingPrice())
                .round(offerBiddingPriceRequestVo.getRound())
                .build();
    }
}
