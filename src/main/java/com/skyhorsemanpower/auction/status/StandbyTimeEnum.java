package com.skyhorsemanpower.auction.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StandbyTimeEnum {
    SECONDS_15(15),
    SECONDS_30(30);
    private final int second;
}
