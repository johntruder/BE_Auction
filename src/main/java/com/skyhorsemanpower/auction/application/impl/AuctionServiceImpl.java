package com.skyhorsemanpower.auction.application.impl;

import com.skyhorsemanpower.auction.application.AuctionService;
import com.skyhorsemanpower.auction.common.exception.CustomException;
import com.skyhorsemanpower.auction.data.vo.AuctionResultResponseVo;
import com.skyhorsemanpower.auction.domain.*;
import com.skyhorsemanpower.auction.kafka.KafkaProducerCluster;
import com.skyhorsemanpower.auction.kafka.Topics;
import com.skyhorsemanpower.auction.kafka.data.MessageEnum;
import com.skyhorsemanpower.auction.kafka.data.dto.AlarmDto;
import com.skyhorsemanpower.auction.kafka.data.dto.AuctionCloseDto;
import com.skyhorsemanpower.auction.quartz.data.MemberUuidsAndPrice;
import com.skyhorsemanpower.auction.repository.*;
import com.skyhorsemanpower.auction.common.exception.ResponseStatus;
import com.skyhorsemanpower.auction.data.dto.*;
import com.skyhorsemanpower.auction.status.AuctionStateEnum;
import com.skyhorsemanpower.auction.status.NextRoundStateEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private final AuctionHistoryRepository auctionHistoryRepository;
    private final RoundInfoRepository roundInfoRepository;
    private final KafkaProducerCluster producer;
    private final AuctionResultRepository auctionResultRepository;
    private final AuctionUniqueRepository auctionUniqueRepository;

    @Override
    @Transactional
    public Boolean offerBiddingPrice(OfferBiddingPriceDto offerBiddingPriceDto) {

        // 현재 경매의 라운드 정보 추출
        RoundInfo roundInfo = roundInfoRepository.
                findFirstByAuctionUuidOrderByCreatedAtDesc(offerBiddingPriceDto.getAuctionUuid()).orElseThrow(
                        () -> new CustomException(ResponseStatus.NO_DATA));

        // 입찰 가능 확인
        // 입찰이 안되면 아래 메서드 내에서 예외를 던진다.
        // isUpdateRoundInfo boolean 데이터는 round_info 도큐먼트를 갱신 트리거
        Boolean isBiddingPossible = isBiddingPossible(offerBiddingPriceDto, roundInfo);

        // 입찰 가능할 때만 아래 로직 진행
        if (isBiddingPossible) {
            // 입찰 정보 저장
            AuctionHistory auctionHistory = AuctionHistory.converter(offerBiddingPriceDto);
            log.info("Saved Auction History Information >>> {}", auctionHistory.toString());

            try {
                auctionHistoryRepository.save(auctionHistory);
            } catch (Exception e) {
                throw new CustomException(ResponseStatus.MONGODB_ERROR);
            }

            // 입찰 후, round_info 도큐먼트 갱신
            updateRoundInfo(roundInfo);
        }

        log.info("isBidding >>> {}", isBiddingPossible);
        return isBiddingPossible;
    }

    @Override
    public void auctionClose(String auctionUuid) {
        try{
            // 저장에 성공하면 마감이 진행되지 않았다는 의미, 바로 마감 진행
             AuctionUnique auctionUnique = auctionUniqueRepository.save(AuctionUnique.builder().auctionUuid(auctionUuid).build());
            log.info("Auction Close Start!");
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

        // round_info 마감됐음을 endStatus에 저장
        RoundInfo currenctRoundInfo = roundInfoRepository.
                findFirstByAuctionUuidOrderByCreatedAtDesc(auctionUuid).orElseThrow(
                        () -> new CustomException(ResponseStatus.NO_DATA)
                );

        roundInfoRepository.save(RoundInfo.builder()
                        .auctionUuid(auctionUuid)
                        .round(currenctRoundInfo.getRound())
                        .roundStartTime(currenctRoundInfo.getRoundStartTime())
                        .roundEndTime(currenctRoundInfo.getRoundEndTime())
                        .incrementUnit(currenctRoundInfo.getIncrementUnit())
                        .price(currenctRoundInfo.getPrice())
                        .isActive(true)
                        .numberOfParticipants(currenctRoundInfo.getNumberOfParticipants())
                        .leftNumberOfParticipants(currenctRoundInfo.getLeftNumberOfParticipants())
                        .createdAt(LocalDateTime.now())
                        .auctionEndTime(currenctRoundInfo.getAuctionEndTime())
                        .isLastRound(currenctRoundInfo.getIsLastRound())
                        .endStatus(true)
                .build());
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

    @Override
    public void auctionStateChangeTrue(String auctionUuid) {
        RoundInfo roundInfo = roundInfoRepository.findFirstByAuctionUuidOrderByCreatedAtDesc(auctionUuid).orElseThrow(
                () -> new CustomException(ResponseStatus.NO_DATA)
        );

        try {
            RoundInfo standbyAuction = RoundInfo.setIsActiveTrue(roundInfo);
            log.info("Auction Change isActive >>> {}", standbyAuction.toString());
            roundInfoRepository.save(standbyAuction);
        } catch (Exception e) {
            throw new CustomException(ResponseStatus.MONGODB_ERROR);
        }
    }

    @Override
    public AuctionResultResponseVo auctionResult(String uuid, String auctionUuid) {
        Optional<AuctionResult> auctionResult = auctionResultRepository.
                findByAuctionUuidAndMemberUuidsContains(auctionUuid, uuid);

        // 낙찰자에 포함되지 않는 경우
        if (auctionResult.isEmpty()) {
            log.info("Auction Result is not exist. not bidder");
            return AuctionResultResponseVo.notBidder();
        }

        // 낙찰자에 포함된 경우
        log.info("Auction Result >>> {}", auctionResult.toString());
        return AuctionResultResponseVo.builder()
                .isBidder(true)
                .price(auctionResult.get().getPrice())
                .build();
    }

    private void updateRoundInfo(RoundInfo roundInfo) {
        RoundInfo updatedRoundInfo;

        // 다음 라운드로 round_info 도큐먼트 갱신
        // isActive 대기 상태로 변경
        if (roundInfo.getLeftNumberOfParticipants() == NextRoundStateEnum.NUMBER_1.getNumber()) {
            updatedRoundInfo = RoundInfo.nextRoundUpdate(roundInfo);
        }

        // 동일 라운드에서 round_info 도큐먼트 갱신
        else {
            updatedRoundInfo = RoundInfo.currentRoundUpdate(roundInfo);
        }

        log.info("Updated round_info Document >>> {}", updatedRoundInfo.toString());

        try {
            roundInfoRepository.save(updatedRoundInfo);
        } catch (Exception e) {
            throw new CustomException(ResponseStatus.MONGODB_ERROR);
        }
    }

    private Boolean isBiddingPossible(OfferBiddingPriceDto offerBiddingPriceDto, RoundInfo roundInfo) {
        // 조건1. 입찰 시간 확인
        // 조건2. 해당 라운드에 참여 여부
        // 조건3. 남은 인원이 1 이상
        // 조건4. round 입찰가와 입력한 입찰가 확인

        return checkBiddingTime(roundInfo.getRoundStartTime(), roundInfo.getRoundEndTime()) &&
                checkBiddingRound(offerBiddingPriceDto.getAuctionUuid(), offerBiddingPriceDto.getBiddingUuid(),
                        offerBiddingPriceDto.getRound()) &&
                checkLeftNumberOfParticipant(roundInfo.getLeftNumberOfParticipants()) &&
                checkRoundAndBiddingPrice(offerBiddingPriceDto, roundInfo)
                ;
    }

    private Boolean checkBiddingRound(String auctionUuid, String biddingUuid, int round) {
        return !(auctionHistoryRepository.findByAuctionUuidAndBiddingUuidAndRound(
                auctionUuid, biddingUuid, round).isPresent());
    }

    private Boolean checkLeftNumberOfParticipant(int leftNumberOfParticipants) {
        log.info("leftNumberOfParticipants >>> {}", leftNumberOfParticipants);
//        if (leftNumberOfParticipants < 1L) throw new CustomException(ResponseStatus.FULL_PARTICIPANTS);
        log.info("!(leftNumberOfParticipants < 1L) >>> {}", !(leftNumberOfParticipants < 1L));
        return !(leftNumberOfParticipants < 1L);
    }

    private Boolean checkRoundAndBiddingPrice(OfferBiddingPriceDto offerBiddingPriceDto, RoundInfo roundInfo) {
        log.info("input round >>> {}, document round >>> {}, input price >>> {}, document price >>> {}",
                offerBiddingPriceDto.getRound(), roundInfo.getRound(),
                offerBiddingPriceDto.getBiddingPrice(), roundInfo.getPrice());

        log.info("inputRound == documentRound >>> {}", offerBiddingPriceDto.getRound() == roundInfo.getRound());
        log.info("inputPrice.compareTo(documentPrice) == 0 >>> {}",
                offerBiddingPriceDto.getBiddingPrice().compareTo(roundInfo.getPrice()) == 0);

        return !((!(offerBiddingPriceDto.getBiddingPrice().compareTo(roundInfo.getPrice()) == 0) ||
                !(offerBiddingPriceDto.getRound() == roundInfo.getRound())));
    }

    private Boolean checkBiddingTime(LocalDateTime roundStartTime, LocalDateTime roundEndTime) {
        log.info("roundStartTime >>> {}, now >>> {}, roundEndTime >>> {}",
                roundStartTime, LocalDateTime.now(), roundEndTime);
        log.info("roundStartTime.isBefore(LocalDateTime.now()) >>> {}, roundEndTime.isAfter(LocalDateTime.now()) >>> {}"
                , roundStartTime.isBefore(LocalDateTime.now()), roundEndTime.isAfter(LocalDateTime.now()));
        // roundStartTime <= 입찰 시간 <= roundEndTime
        return (roundStartTime.isBefore(LocalDateTime.now()) && roundEndTime.isAfter(LocalDateTime.now()));
    }

}
