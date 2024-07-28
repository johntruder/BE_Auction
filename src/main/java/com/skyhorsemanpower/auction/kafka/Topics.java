package com.skyhorsemanpower.auction.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Topics {
    NOTIFICATION_SERVICE(Constant.NOTIFICATION_SERVICE),
    CHAT_SERVICE(Constant.CHAT_SERVICE),
    AUCTION_POST_SERVICE(Constant.AUCTION_POST_SERVICE),
    PAYMENT_SERVICE(Constant.PAYMENT_SERVICE),
    SUCCESSFUL_BID_ALARM(Constant.SUCCESSFUL_BID_ALARM),
    INITIAL_AUCTION(Constant.INITIAL_AUCTION),
    AUCTION_CLOSE(Constant.AUCTION_CLOSE),
    ALARM(Constant.ALARM)
    ;

    public static class Constant {
        public static final String NOTIFICATION_SERVICE = "alarm-topic";
        public static final String CHAT_SERVICE = "chat-topic";
        public static final String AUCTION_POST_SERVICE = "new-auction-post-topic";
        public static final String PAYMENT_SERVICE = "event-preview-topic";
        public static final String SUCCESSFUL_BID_ALARM = "successful-bid-alarm-topic";
        public static final String INITIAL_AUCTION = "initial-auction-topic";
        public static final String AUCTION_CLOSE = "auction-close-topic";
        public static final String ALARM ="alarm-topic";

    }

    private final String topic;
}
