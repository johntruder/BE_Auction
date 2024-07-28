package com.skyhorsemanpower.auction.kafka.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessageEnum {
    AUCTION_CLOSE_MESSAGE(Constant.AUCTION_CLOSE_MESSAGE);

    public static class Constant {
        public static final String AUCTION_CLOSE_MESSAGE = "경매 낙찰되었습니다.";
    }

    private final String message;
}
