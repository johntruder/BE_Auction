package com.skyhorsemanpower.auction.repository;

import com.skyhorsemanpower.auction.data.vo.RoundInfoResponseVo;
import com.skyhorsemanpower.auction.domain.RoundInfo;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RoundInfoReactiveRepository extends ReactiveMongoRepository<RoundInfo, String> {

    @Tailable
    @Query("{'auctionUuid' : ?0}")
    Flux<RoundInfoResponseVo> searchRoundInfo(String auctionUuid);
}
