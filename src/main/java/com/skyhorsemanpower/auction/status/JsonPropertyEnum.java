package com.skyhorsemanpower.auction.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JsonPropertyEnum {
    HANDLE(Constant.HANDLE),
    PROFILE(Constant.PROFILE),
    IS_SUBSCRIBED(Constant.IS_SUBSCRIBED),
    UUID(Constant.UUID);

    public static class Constant {
        public static final String HANDLE = "handle";
        public static final String PROFILE = "profileImage";
        public static final String IS_SUBSCRIBED = "isSubscribed";
        public static final String UUID = "uuid";
    }

    private final String property;
}
