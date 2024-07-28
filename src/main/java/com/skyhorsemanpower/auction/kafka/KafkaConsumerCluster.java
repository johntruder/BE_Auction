package com.skyhorsemanpower.auction.kafka;

import com.skyhorsemanpower.auction.config.QuartzJobConfig;
import com.skyhorsemanpower.auction.domain.RoundInfo;
import com.skyhorsemanpower.auction.kafka.data.dto.InitialAuctionDto;
import com.skyhorsemanpower.auction.repository.RoundInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaConsumerCluster {
    private final RoundInfoRepository roundInfoRepository;
    private final QuartzJobConfig quartzJobConfig;

    @KafkaListener(topics = Topics.Constant.INITIAL_AUCTION, groupId = "${spring.kafka.consumer.group-id}")
    public void initialAuction(@Payload LinkedHashMap<String, Object> message,
        @Headers MessageHeaders messageHeaders) {
        log.info("consumer: success >>> message: {}, headers: {}", message.toString(),
                messageHeaders);

        // 경매 마감 시간이 현재보다 과거인 경매는 로직을 하지 않는다.
        Long auctionEndTime = (Long) message.get("auctionEndTime");
        Long currentTime = System.currentTimeMillis();
        if(auctionEndTime < currentTime) {
            log.info("Already Closed Auction Message!");
            return;
        }

        // 경매 마감 시간이 안 넘은 경우에만 아래 로직을 실행
        // round_info 초기 데이터 저장
        InitialAuctionDto initialAuctionDto = InitialAuctionDto.builder()
                .auctionUuid(message.get("auctionUuid").toString())
                .startPrice(new BigDecimal(message.get("startPrice").toString()))
                .numberOfEventParticipants((Integer) message.get("numberOfEventParticipants"))
                .auctionStartTime((Long) message.get("auctionStartTime"))
                .auctionEndTime((Long) message.get("auctionEndTime"))
                .incrementUnit(new BigDecimal(message.get("incrementUnit").toString()))
                .build();
        log.info("InitialAuctionDto >>> {}", initialAuctionDto.toString());

        // 초기 round_info 도큐먼트 저장
        roundInfoRepository.save(RoundInfo.initialRoundInfo(initialAuctionDto));

        // 경매 마감 스케줄러 등록
        try {
            quartzJobConfig.schedulerUpdateAuctionStateJob(initialAuctionDto);
        } catch (Exception e1) {
            log.warn(e1.getMessage());
        }
    }
}
