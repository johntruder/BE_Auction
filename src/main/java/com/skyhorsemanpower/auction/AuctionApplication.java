package com.skyhorsemanpower.auction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.TimeZone;


@SpringBootApplication
@EnableMongoRepositories
@RefreshScope
@EnableDiscoveryClient
public class AuctionApplication {

	public static void main(String[] args) {
		// application 전체 timezone을 UTC로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(AuctionApplication.class, args);
	}

}
