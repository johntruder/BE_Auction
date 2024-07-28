package com.skyhorsemanpower.auction.data.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyhorsemanpower.auction.status.JsonPropertyEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MemberUuidResponseVo {

    @JsonProperty(value = JsonPropertyEnum.Constant.UUID)
    private String uuid;
}
