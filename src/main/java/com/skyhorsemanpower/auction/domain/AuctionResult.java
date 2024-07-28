package com.skyhorsemanpower.auction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@ToString
@Document(collection = "auction_result")
public class AuctionResult {
    @Id
    private String auctionResultId;

    private String auctionUuid;
    private List<String> memberUuids;
    private BigDecimal price;

    @Builder
    public AuctionResult(String auctionUuid, List<String> memberUuids, BigDecimal price) {
        this.auctionUuid = auctionUuid;
        this.memberUuids = memberUuids;
        this.price = price;
    }
}
