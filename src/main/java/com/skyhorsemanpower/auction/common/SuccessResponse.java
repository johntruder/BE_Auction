package com.skyhorsemanpower.auction.common;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SuccessResponse<T> extends ResponseEntity<T> {

    @Builder
    public SuccessResponse(T body) {
        super(body, HttpStatus.OK);
    }
}
