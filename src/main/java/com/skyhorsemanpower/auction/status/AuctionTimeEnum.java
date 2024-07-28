package com.skyhorsemanpower.auction.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuctionTimeEnum {
    MINUTES_01(1),
    MINUTES_30(30),
    MINUTES_60(60),
    MINUTES_120(120);

    private final int minute;
}
