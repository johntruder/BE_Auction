package com.skyhorsemanpower.auction.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AuctionUnique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String auctionUuid;

    @Builder
    public AuctionUnique(String auctionUuid) {
        this.auctionUuid = auctionUuid;
    }
}
