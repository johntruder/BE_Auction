package com.skyhorsemanpower.auction.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServerPathEnum {
    MEMBER_SERVER("http://52.79.127.196:8000/member-service"),

    GET_HANDLE("/api/v1/non-authorization/users/datarequest/with-uuid"),
    GET_UUID("/api/v1/non-authorization/users/datarequest/with-handle"),
    GET_ISSUBSCRIBED("/api/v1/authorization/subscription/auction/is-subscribed");

    private final String server;
}
