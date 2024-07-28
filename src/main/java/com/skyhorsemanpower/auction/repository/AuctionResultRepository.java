package com.skyhorsemanpower.auction.repository;

import com.skyhorsemanpower.auction.domain.AuctionResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuctionResultRepository extends MongoRepository<AuctionResult, String> {
    Optional<AuctionResult> findByAuctionUuidAndMemberUuidsContains(String auctionUuid, String memberUuid);
}
