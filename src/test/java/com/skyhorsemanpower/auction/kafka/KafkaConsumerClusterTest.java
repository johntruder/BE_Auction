package com.skyhorsemanpower.auction.kafka;

import com.skyhorsemanpower.auction.config.QuartzJobConfig;
import com.skyhorsemanpower.auction.domain.RoundInfo;
import com.skyhorsemanpower.auction.kafka.data.dto.InitialAuctionDto;
import com.skyhorsemanpower.auction.repository.RoundInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.util.LinkedHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaConsumerClusterTest {

    @Mock
    private RoundInfoRepository roundInfoRepository;

    @Mock
    private QuartzJobConfig quartzJobConfig;

    @InjectMocks
    private KafkaConsumerCluster kafkaConsumerCluster;

    private LinkedHashMap<String, Object> message;
    private GenericMessage<LinkedHashMap<String, Object>> genericMessage;

    @BeforeEach
    void setUp() {
        message = new LinkedHashMap<>();
        message.put("auctionUuid", "test-uuid");
        message.put("startPrice", "1000");
        message.put("numberOfEventParticipants", 10);
        message.put("auctionStartTime", System.currentTimeMillis() + 10000);
        message.put("incrementUnit", "100");

        // GenericMessage 객체 생성
        genericMessage = new GenericMessage<>(message);
    }

    @Test
    @DisplayName("메시지 수신 내용 중 auctionEndTime이 현재보다 미래인 경우(정상)")
    void testInitialAuction_FutureEndTime() throws SchedulerException {
        // Given
        Long auctionEndTime = System.currentTimeMillis() + 20000;
        message.put("auctionEndTime", auctionEndTime);

        // When
        kafkaConsumerCluster.initialAuction(message, genericMessage.getHeaders());

        // Then
        // 저장과 스케줄 등록 로직이 한 번 호출되야 한다.
        verify(roundInfoRepository, times(1)).save(any(RoundInfo.class));
        verify(quartzJobConfig, times(1)).schedulerUpdateAuctionStateJob(any(InitialAuctionDto.class));
    }

    @Test
    @DisplayName("메시지 수신 내용 중 auctionEndTime이 현재보다 과거인 경우(비정상)")
    void testInitialAuction_PastEndTime() throws SchedulerException {
        // Given
        Long auctionEndTime = System.currentTimeMillis() - 20000;
        message.put("auctionEndTime", auctionEndTime);

        // When
        kafkaConsumerCluster.initialAuction(message, genericMessage.getHeaders());

        // Then
        // 저장과 스케줄 등록이 되면 안된다.
        verify(roundInfoRepository, never()).save(any());
        verify(quartzJobConfig, never()).schedulerUpdateAuctionStateJob(any());
    }
}
