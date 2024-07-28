package com.skyhorsemanpower.auction.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NextRoundStateEnum {
    NUMBER_1(1);
    private final int number;
}
