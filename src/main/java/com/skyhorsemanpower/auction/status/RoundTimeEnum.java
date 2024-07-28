package com.skyhorsemanpower.auction.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoundTimeEnum {
    SECONDS_15(15),
    SECONDS_30(30),
    SECONDS_45(45),
    SECONDS_60(60);
    private final int second;
}
