package com.skyhorsemanpower.auction.repository;

import com.skyhorsemanpower.auction.data.projection.CheckBiddingPriceProjection;
import com.skyhorsemanpower.auction.domain.AuctionHistory;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionHistoryRepository extends MongoRepository<AuctionHistory, String> {

    // 상위 한개의 도큐먼트를 반환하도록 설정
    @Aggregation(pipeline = {
            "{ '$match': { 'auctionUuid': ?0 } }",
            "{ '$sort': { 'biddingPrice': -1 } }",
            "{ '$limit': 1 }",
            "{ '$project': { 'biddingPrice': 1 } }"
    })
    Optional<CheckBiddingPriceProjection> findMaxBiddingPriceByAuctionUuid(String auctionUuid);

    List<AuctionHistory> findByAuctionUuidAndRoundOrderByBiddingTime(String auctionUuid, int round);

    Optional<AuctionHistory> findByAuctionUuidAndBiddingUuidAndRound(String auctionUuid, String biddingUuid, int round);

    Optional<AuctionHistory> findFirstByAuctionUuidOrderByBiddingTimeDesc(String auctionUuid);
}
