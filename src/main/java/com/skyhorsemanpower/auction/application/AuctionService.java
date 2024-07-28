package com.skyhorsemanpower.auction.application;

import com.skyhorsemanpower.auction.data.dto.*;
import com.skyhorsemanpower.auction.data.vo.AuctionResultResponseVo;

public interface AuctionService {
    Boolean offerBiddingPrice(OfferBiddingPriceDto offerBiddingPriceDto);

    void auctionClose(String auctionUuid);

    void auctionStateChangeTrue(String auctionUuid);

    AuctionResultResponseVo auctionResult(String uuid, String auctionUuid);
}
