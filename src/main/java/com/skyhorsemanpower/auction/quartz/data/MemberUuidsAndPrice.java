package com.skyhorsemanpower.auction.quartz.data;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Builder
public class MemberUuidsAndPrice {
    private Set<String> memberUuids;
    private BigDecimal price;
}
