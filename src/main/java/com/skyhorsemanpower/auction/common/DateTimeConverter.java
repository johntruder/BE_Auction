package com.skyhorsemanpower.auction.common;

import com.skyhorsemanpower.auction.status.TimeZoneChangeEnum;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateTimeConverter {

    public static LocalDateTime instantToLocalDateTime(long longOfInstant) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(longOfInstant), ZoneId.systemDefault());
    }

    public static long localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long kstLocalDateTimeToInstant(LocalDateTime kstLocalDateTime) {
        return kstLocalDateTime.minusHours(TimeZoneChangeEnum.KOREA.getTimeDiff())
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

}
