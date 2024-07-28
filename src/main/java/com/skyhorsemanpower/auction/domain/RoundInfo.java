package com.skyhorsemanpower.auction.domain;

import com.skyhorsemanpower.auction.common.DateTimeConverter;
import com.skyhorsemanpower.auction.kafka.data.dto.InitialAuctionDto;
import com.skyhorsemanpower.auction.status.AuctionTimeEnum;
import com.skyhorsemanpower.auction.status.RoundTimeEnum;
import com.skyhorsemanpower.auction.status.StandbyTimeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@ToString
@Slf4j
@Document(collection = "round_info")
public class RoundInfo {
    @Id
    private String roundInfoId;

    private String auctionUuid;
    private Integer round;
    private LocalDateTime roundStartTime;
    private LocalDateTime roundEndTime;
    private BigDecimal incrementUnit;
    private BigDecimal price;
    private Boolean isActive;
    private int numberOfParticipants;
    private int leftNumberOfParticipants;
    private LocalDateTime createdAt;
    private LocalDateTime auctionEndTime;
    private Boolean isLastRound;
    private Boolean endStatus;

    @Builder
    public RoundInfo(String auctionUuid, Integer round, LocalDateTime roundStartTime, LocalDateTime roundEndTime,
                     BigDecimal incrementUnit, BigDecimal price, Boolean isActive, int numberOfParticipants,
                     int leftNumberOfParticipants, LocalDateTime createdAt,
                     LocalDateTime auctionEndTime, Boolean isLastRound, Boolean endStatus) {
        this.auctionUuid = auctionUuid;
        this.round = round;
        this.roundStartTime = roundStartTime;
        this.roundEndTime = roundEndTime;
        this.incrementUnit = incrementUnit;
        this.price = price;
        this.isActive = isActive;
        this.numberOfParticipants = numberOfParticipants;
        this.leftNumberOfParticipants = leftNumberOfParticipants;
        this.createdAt = LocalDateTime.now();
        this.auctionEndTime = auctionEndTime;
        this.isLastRound = isLastRound;
        this.endStatus = endStatus;
    }

    public static RoundInfo nextRoundUpdate(RoundInfo roundInfo) {
        Integer nextRound = roundInfo.getRound() + 1;
        LocalDateTime nextRoundStartTime = LocalDateTime.now().plusSeconds(StandbyTimeEnum.SECONDS_15.getSecond());
        LocalDateTime nextRoundEndTime = nextRoundStartTime.plusSeconds(RoundTimeEnum.SECONDS_60.getSecond());
        BigDecimal nextPrice = roundInfo.getPrice().add(roundInfo.getIncrementUnit());
        LocalDateTime auctionEndTime = roundInfo.getAuctionEndTime();

        // nextRoundStartTime <= auctionEndTime <= nextRoundEndTime 인 경우 다음 라운드가 마지막 라운드
        boolean isLastRound = nextRoundStartTime.isBefore(auctionEndTime) && auctionEndTime.isBefore(nextRoundEndTime);

        return RoundInfo.builder()
                .auctionUuid(roundInfo.getAuctionUuid())
                .round(nextRound)
                .roundStartTime(nextRoundStartTime)
                .roundEndTime(nextRoundEndTime)
                .incrementUnit(roundInfo.getIncrementUnit())
                .price(nextPrice)
                .isActive(false)        // 대기 상태로 변경
                .numberOfParticipants(roundInfo.getNumberOfParticipants())
                .leftNumberOfParticipants(roundInfo.getNumberOfParticipants())
                .auctionEndTime(roundInfo.getAuctionEndTime())
                .isLastRound(isLastRound)
                .endStatus(false)
                .build();
    }

    public static RoundInfo currentRoundUpdate(RoundInfo roundInfo) {
        int nextNumberOfParticipants = roundInfo.getLeftNumberOfParticipants() - 1;

        return RoundInfo.builder()
                .auctionUuid(roundInfo.getAuctionUuid())
                .round(roundInfo.getRound())
                .roundStartTime(roundInfo.getRoundStartTime())
                .roundEndTime(roundInfo.getRoundEndTime())
                .incrementUnit(roundInfo.getIncrementUnit())
                .price(roundInfo.getPrice())
                .isActive(true)
                .numberOfParticipants(roundInfo.getNumberOfParticipants())
                .leftNumberOfParticipants(nextNumberOfParticipants)
                .auctionEndTime(roundInfo.getAuctionEndTime())
                .isLastRound(roundInfo.getIsLastRound())
                .endStatus(false)
                .build();
    }

    public static RoundInfo setIsActiveTrue(RoundInfo roundInfo) {
        return RoundInfo.builder()
                .auctionUuid(roundInfo.getAuctionUuid())
                .round(roundInfo.getRound())
                .roundStartTime(roundInfo.getRoundStartTime())
                .roundEndTime(roundInfo.getRoundEndTime())
                .incrementUnit(roundInfo.getIncrementUnit())
                .price(roundInfo.getPrice())
                .isActive(true)
                .numberOfParticipants(roundInfo.getNumberOfParticipants())
                .leftNumberOfParticipants(roundInfo.getLeftNumberOfParticipants())
                .auctionEndTime(roundInfo.getAuctionEndTime())
                .isLastRound(roundInfo.getIsLastRound())
                .createdAt(LocalDateTime.now())
                .endStatus(false)
                .build();
    }

    // initialRoundInfo 저장
    public static RoundInfo initialRoundInfo(InitialAuctionDto initialAuctionDto) {
        // Instant 타입을 LocalDateTime 변환
        LocalDateTime roundStartTime = DateTimeConverter.
                instantToLocalDateTime(initialAuctionDto.getAuctionStartTime());
        LocalDateTime auctionEndTime = roundStartTime.plusMinutes(AuctionTimeEnum.MINUTES_120.getMinute());

        RoundInfo roundinfo = RoundInfo.builder()
                .auctionUuid(initialAuctionDto.getAuctionUuid())
                .round(1)
                .roundStartTime(roundStartTime)
                .roundEndTime(roundStartTime.plusSeconds(RoundTimeEnum.SECONDS_60.getSecond()))
                .incrementUnit(initialAuctionDto.getIncrementUnit())
                .price(initialAuctionDto.getStartPrice())
                .isActive(true)
                .numberOfParticipants(initialAuctionDto.getNumberOfEventParticipants())
                .leftNumberOfParticipants(initialAuctionDto.getNumberOfEventParticipants())
                .createdAt(LocalDateTime.now())
                .auctionEndTime(auctionEndTime)
                .isLastRound(false)
                .endStatus(false)
                .build();

        log.info("Initial round_info >>> {}", roundinfo);

        return roundinfo;
    }
}
