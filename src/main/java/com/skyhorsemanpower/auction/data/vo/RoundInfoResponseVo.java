package com.skyhorsemanpower.auction.data.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
public class RoundInfoResponseVo {
    private Integer round;
    private LocalDateTime roundStartTime;
    private LocalDateTime roundEndTime;
    private BigDecimal incrementUnit;
    private BigDecimal price;
    private Boolean isActive;
    private Long numberOfParticipants;
    private Long leftNumberOfParticipants;
    private Boolean endStatus;

    @Builder

    public RoundInfoResponseVo(Integer round, LocalDateTime roundStartTime, LocalDateTime roundEndTime,
                               BigDecimal incrementUnit, BigDecimal price, Boolean isActive,
                               Long numberOfParticipants, Long leftNumberOfParticipants, Boolean endStatus) {
        this.round = round;
        this.roundStartTime = roundStartTime;
        this.roundEndTime = roundEndTime;
        this.incrementUnit = incrementUnit;
        this.price = price;
        this.isActive = isActive;
        this.numberOfParticipants = numberOfParticipants;
        this.leftNumberOfParticipants = leftNumberOfParticipants;
        this.endStatus = endStatus;
    }
}
