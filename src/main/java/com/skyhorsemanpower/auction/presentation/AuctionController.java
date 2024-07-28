package com.skyhorsemanpower.auction.presentation;

import com.skyhorsemanpower.auction.application.AuctionService;
import com.skyhorsemanpower.auction.common.SuccessResponse;
import com.skyhorsemanpower.auction.common.exception.CustomException;
import com.skyhorsemanpower.auction.common.exception.ResponseStatus;
import com.skyhorsemanpower.auction.data.dto.OfferBiddingPriceDto;
import com.skyhorsemanpower.auction.data.vo.AuctionResultResponseVo;
import com.skyhorsemanpower.auction.data.vo.OfferBiddingPriceRequestVo;
import com.skyhorsemanpower.auction.data.vo.RoundInfoResponseVo;
import com.skyhorsemanpower.auction.domain.RoundInfo;
import com.skyhorsemanpower.auction.repository.RoundInfoReactiveRepository;
import com.skyhorsemanpower.auction.repository.RoundInfoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "경매 서비스", description = "경매 서비스 API")
@RequestMapping("/api/v1/auction")
@CrossOrigin(value = "*")
public class AuctionController {
    private final AuctionService auctionService;
    private final RoundInfoReactiveRepository roundInfoReactiveRepository;
    private final RoundInfoRepository roundInfoRepository;

    // 경매 입찰가 제시
    @PostMapping("/bidding")
    @Operation(summary = "경매 입찰가 제시", description = "경매 입찰가 제시")
    public Boolean offerBiddingPrice(
            @RequestHeader String uuid,
            @RequestBody OfferBiddingPriceRequestVo offerBiddingPriceRequestVo) {
        return auctionService.offerBiddingPrice(OfferBiddingPriceDto.voToDto(offerBiddingPriceRequestVo, uuid));
    }

    // 경매 페이지 API
    @GetMapping(value = "/auction-page/{auctionUuid}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "경매 페이지 API", description = "경매 페이지에 보여줄 데이터 실시간 조회")
    public Flux<RoundInfoResponseVo> auctionPage(
            @PathVariable("auctionUuid") String auctionUuid) {
        Flux<RoundInfoResponseVo> roundInfoResponseVoFlux = roundInfoReactiveRepository.searchRoundInfo(auctionUuid)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> {
                    if (error instanceof TimeoutException) {
                        log.info("Timeout occurred about SSE!");
                    } else {
                        log.info("SSE error occured!! >>> {}", error.toString());
                    }
                })
                .onErrorResume(error -> {
                            if (error instanceof TimeoutException) {
                                log.info("Connection closed due to timeout");
                                // 에러 발생 시, 빈 Flux 객체를 반환
                                return Flux.empty();
                            }
                            // 다른 에러 발생 시, 빈 Flux 객체를 반환해서 연결 종료
                            return Flux.error(error);
                        }
                );

        // heartbeat 스트림으로 1분 주기로 확인
        Flux<RoundInfoResponseVo> heartbeat = Flux.interval(Duration.ofMinutes(1))
                .map(tick -> new RoundInfoResponseVo());

        // 메시지 및 heartbeat 반환
        return roundInfoResponseVoFlux.mergeWith(heartbeat)
                .doOnSubscribe(sub -> log.info("Subscribed to roundInfoResponseVo and heartbeat streams"))
                .doFinally(signalType -> {
                    // 디버그 용 로그
                    if (signalType == SignalType.ON_COMPLETE) {
                        log.info("Connection completed.");
                    } else if (signalType == SignalType.ON_ERROR) {
                        log.info("Connection terminated due to error.");
                    } else {
                        log.info("Connection terminated by signal type: {}", signalType);
                    }
                    //todo
                    // 자원 해제 메서드 추가 필요
                });
    }

    // 경매 페이지 최초 진입 시 현재 데이터 조회 API
    @GetMapping("/initial-auction-page/{auctionUuid}")
    @Operation(summary = "경매 페이지 입장 시 사용되는 API", description = "경매 페이지 최초 진입 시 현재 데이터 조회")
    public SuccessResponse<RoundInfo> initialAuctionPage(
            @PathVariable("auctionUuid") String auctionUuid) {
        return new SuccessResponse<>(roundInfoRepository.
                findFirstByAuctionUuidOrderByCreatedAtDesc(auctionUuid).orElseThrow(
                () -> new CustomException(ResponseStatus.NO_DATA)));
    }

    // 경매 마감 API 구현
    @GetMapping("/auction-close/{auctionUuid}")
    @Operation(summary = "경매 마감 API", description = "경매 마감 처리 후 결제 서비스에 메시지 전달")
    public SuccessResponse<Object> auctionClose(
            @PathVariable("auctionUuid") String auctionUuid) {
        auctionService.auctionClose(auctionUuid);
        return new SuccessResponse<>(null);
    }

    // 라운드 진행 중에서 대기 중으로 상태 변경
    @PutMapping("/auction-standby-end/{auctionUuid}")
    @Operation(summary = "경매 라운드 진행 API", description = "대기 시간 끝나서 라운드 진행 중으로 상태 변경")
    public SuccessResponse<Object> auctionStateChangeTrue(
            @PathVariable("auctionUuid") String auctionUuid) {
        // round_info의 isActive를 true(진행 중)로 변경
        auctionService.auctionStateChangeTrue(auctionUuid);
        return new SuccessResponse<>(null);
    }

    // 유저에 따른 경매 결과 조회
    @GetMapping("/result/{auctionUuid}")
    @Operation(summary = "경매 결과 조회 API", description = "유저가 경매에 낙찰됐는가 여부 조회")
    public SuccessResponse<AuctionResultResponseVo> auctionResult(
            @RequestHeader String uuid,
            @PathVariable("auctionUuid") String auctionUuid) {
        return new SuccessResponse<>(auctionService.auctionResult(uuid, auctionUuid));
    }
}