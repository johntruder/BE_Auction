package com.skyhorsemanpower.auction.quartz;

import com.skyhorsemanpower.auction.common.exception.CustomException;
import com.skyhorsemanpower.auction.common.exception.ResponseStatus;
import com.skyhorsemanpower.auction.domain.AuctionHistory;
import com.skyhorsemanpower.auction.domain.AuctionResult;
import com.skyhorsemanpower.auction.domain.AuctionUnique;
import com.skyhorsemanpower.auction.domain.RoundInfo;
import com.skyhorsemanpower.auction.kafka.KafkaProducerCluster;
import com.skyhorsemanpower.auction.kafka.Topics;
import com.skyhorsemanpower.auction.kafka.data.MessageEnum;
import com.skyhorsemanpower.auction.kafka.data.dto.AlarmDto;
import com.skyhorsemanpower.auction.kafka.data.dto.AuctionCloseDto;
import com.skyhorsemanpower.auction.quartz.data.MemberUuidsAndPrice;
import com.skyhorsemanpower.auction.repository.AuctionHistoryRepository;
import com.skyhorsemanpower.auction.repository.AuctionResultRepository;
import com.skyhorsemanpower.auction.repository.AuctionUniqueRepository;
import com.skyhorsemanpower.auction.repository.RoundInfoRepository;
import com.skyhorsemanpower.auction.status.AuctionStateEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@RequiredArgsConstructor
public class AuctionClose implements Job {
    private final KafkaProducerCluster producer;
    private final AuctionHistoryRepository auctionHistoryRepository;
    private final RoundInfoRepository roundInfoRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final AuctionUniqueRepository auctionUniqueRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // JobDataMap에서 auctionUuid 추출
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String auctionUuid = jobDataMap.getString("auctionUuid");

        // 이미 마감된 경매는 진행하지 않습니다.
        try{
            // 저장에 성공하면 마감이 진행되지 않았다는 의미, 바로 마감 진행
            auctionUniqueRepository.save(AuctionUnique.builder().auctionUuid(auctionUuid).build());
            log.info("Scheduled Auction Close Job Start!");
        } catch (Exception e) {
            log.info("Auction Already Close!");
            return;
        }

        // auction_history 도큐먼트를 조회하여 경매 상태를 변경
        if (auctionHistoryRepository.findFirstByAuctionUuidOrderByBiddingTimeDesc(auctionUuid).isEmpty()) {
            log.info("auction_history is not exist! No one bid the auction!");

            // 아무도 참여하지 않은 경우에는 auctionUuid와 auctionState(AUCTION_NO_PARTICIPANTS) 전송
            AuctionCloseDto noParticipantsAuctionCloseDto = AuctionCloseDto.builder()
                    .auctionUuid(auctionUuid)
                    .auctionState(AuctionStateEnum.AUCTION_NO_PARTICIPANTS)
                    .build();
            log.info("No one bid the auction message >>> {}", noParticipantsAuctionCloseDto.toString());
            producer.sendMessage(Topics.Constant.AUCTION_CLOSE, noParticipantsAuctionCloseDto);

            return;
        }

        log.info("auction_history is exist!");

        // 경매 마감 로직
        // 마지막 라운드 수, 낙찰 가능 인원 수 조회
        RoundInfo lastRoundInfo = roundInfoRepository.findFirstByAuctionUuidOrderByCreatedAtDesc(auctionUuid)
                .orElseThrow(() -> new CustomException(ResponseStatus.NO_DATA)
                );
        log.info("Last Round Info >>> {}", lastRoundInfo.toString());

        int round = lastRoundInfo.getRound();
        long numberOfParticipants = lastRoundInfo.getNumberOfParticipants();

        // 마감 로직
        MemberUuidsAndPrice memberUuidsAndPrice = getMemberUuidsAndPrice(
                round, auctionUuid, numberOfParticipants);

        // 낙찰가와 낙찰자 획득
        Set<String> memberUuids = memberUuidsAndPrice.getMemberUuids();
        BigDecimal price = memberUuidsAndPrice.getPrice();

        // 카프카로 경매 서비스 메시지 전달
        AuctionCloseDto auctionCloseDto = AuctionCloseDto.builder()
                .auctionUuid(auctionUuid)
                .memberUuids(memberUuids.stream().toList())
                .price(price)
                .auctionState(AuctionStateEnum.AUCTION_NORMAL_CLOSING)
                .build();
        log.info("Kafka Message To Payment Service >>> {}", auctionCloseDto.toString());

        // 경매글 마감 처리 메시지와 결제 서비스 메시지 동일 토픽으로 진행
        producer.sendMessage(Topics.Constant.AUCTION_CLOSE, auctionCloseDto);

        // 알람 서비스로 메시지 전달
        AlarmDto alarmDto = AlarmDto.builder().receiverUuids(memberUuids.stream().toList())
                .message(MessageEnum.Constant.AUCTION_CLOSE_MESSAGE)
                .eventType("경매")
                .uuid(auctionUuid)
                .build();
        log.info("Auction Close Message To Alarm Service >>> {}", alarmDto.toString());

        producer.sendMessage(Topics.Constant.ALARM, alarmDto);

        // 경매 결과 저장
        auctionResultRepository.save(AuctionResult.builder()
                .auctionUuid(auctionUuid)
                .memberUuids(memberUuids.stream().toList())
                .price(price)
                .build());
        log.info("Auction Result Save!");
    }

    private MemberUuidsAndPrice getMemberUuidsAndPrice(int round, String auctionUuid, long numberOfParticipants) {
        Set<String> memberUuids = new HashSet<>();
        BigDecimal price;

        // 마지막 라운드 입찰 이력
        List<AuctionHistory> lastRoundAuctionHistory = auctionHistoryRepository.
                findByAuctionUuidAndRoundOrderByBiddingTime(auctionUuid, round);
        log.info("Last Round Auction History >>> {}", lastRoundAuctionHistory.toString());

        // 1라운드에서 경매가 마감된 경우
        if (round == 1) {
            log.info("One Round Close");
            // 마지막 라운드 입찰자를 낙찰자로 고정
            for (AuctionHistory auctionHistory : lastRoundAuctionHistory) {
                memberUuids.add(auctionHistory.getBiddingUuid());
            }

            log.info("memberUuids >>> {}", memberUuids.toString());

            // 낙찰가는 마지막 라운드에서 biddingPrice로 결정
            price = lastRoundAuctionHistory.get(0).getBiddingPrice();
            log.info("price >>> {}", price);
        }

        // 1라운드 제외한 라운드에서 경매가 마감된 경우
        else {
            log.info("{} Round Close", round);

            // 마지막 - 1 라운드 입찰 이력
            List<AuctionHistory> lastMinusOneRoundAuctionHistory = auctionHistoryRepository.
                    findByAuctionUuidAndRoundOrderByBiddingTime(auctionUuid, round - 1);
            log.info("Before Last Round Auction History >>> {}", lastMinusOneRoundAuctionHistory.toString());

            // 마지막 라운드 입찰자를 낙찰자로 고정
            for (AuctionHistory auctionHistory : lastRoundAuctionHistory) {
                memberUuids.add(auctionHistory.getBiddingUuid());
            }

            // 마지막 직전 라운드 입찰자 중 낙찰자 추가
            for (AuctionHistory auctionHistory : lastMinusOneRoundAuctionHistory) {
                // 동일 입찰자 제외하고 추가
                memberUuids.add(auctionHistory.getBiddingUuid());

                // 낙찰 가능 인원 수 만큼 리스트 추가
                if (memberUuids.size() == numberOfParticipants) break;
            }

            log.info("memberUuids >>> {}", memberUuids.toString());

            // 낙찰가는 마지막 이전 라운드에서 biddingPrice로 결정
            price = lastMinusOneRoundAuctionHistory.get(0).getBiddingPrice();
            log.info("price >>> {}", price);
        }

        return MemberUuidsAndPrice.builder().memberUuids(memberUuids).price(price).build();
    }
}
