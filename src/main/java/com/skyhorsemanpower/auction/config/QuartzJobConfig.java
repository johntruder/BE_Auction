package com.skyhorsemanpower.auction.config;

import com.skyhorsemanpower.auction.kafka.data.dto.InitialAuctionDto;
import com.skyhorsemanpower.auction.quartz.AuctionClose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Date;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class QuartzJobConfig {
    private final Scheduler scheduler;

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        return scheduler;
    }

    // 경매 시작과 경매 마감의 상태 변경 스케줄링
    public void schedulerUpdateAuctionStateJob(InitialAuctionDto initialAuctionDto) throws SchedulerException {
        // JobDataMap 생성 및 auctionUuid 설정
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("auctionUuid", initialAuctionDto.getAuctionUuid());

        // Job 생성
        JobDetail auctionCloseJob = JobBuilder
                .newJob(AuctionClose.class)
                .withIdentity("AuctionCloseJob_" + initialAuctionDto.getAuctionUuid(),
                        "AuctionCloseGroup")
                .usingJobData(jobDataMap)
                .withDescription("경매 마감 Job")
                .build();


        //todo
        // 테스트를 위한경매 마감 시간을 경매 시작 시간으로부터 1분 뒤로 설정
//        Date auctionEndDate = Date.from(Instant.ofEpochMilli(initialAuctionDto.getAuctionStartTime()).plusSeconds(60));

        // 배포에선 아래 코드 사용해야 함
        Date auctionEndDate = Date.from(Instant.ofEpochMilli(initialAuctionDto.getAuctionEndTime()));
        log.info("Auction Close Job Will Start At >>> {}", auctionEndDate);

        // Trigger 생성
        Trigger auctionCloseTrigger = TriggerBuilder
                .newTrigger()
                .withIdentity("AuctionCloseTrigger_" + initialAuctionDto.getAuctionUuid(),
                        "AuctionCloseGroup")
                .withDescription("경매 마감 Trigger")
                .startAt(auctionEndDate)
                .build();

        // 스케줄러 생성 및 Job, Trigger 등록
        scheduler.scheduleJob(auctionCloseJob, auctionCloseTrigger);
    }
}
