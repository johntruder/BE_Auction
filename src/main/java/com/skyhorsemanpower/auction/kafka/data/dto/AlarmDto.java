package com.skyhorsemanpower.auction.kafka.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class AlarmDto {
    private List<String> receiverUuids;
    private String message;
    private String eventType;
    private String uuid;

    @Builder
    public AlarmDto(List<String> receiverUuids, String message, String eventType, String uuid) {
        this.receiverUuids = receiverUuids;
        this.message = message;
        this.eventType = eventType;
        this.uuid = uuid;
    }
}
