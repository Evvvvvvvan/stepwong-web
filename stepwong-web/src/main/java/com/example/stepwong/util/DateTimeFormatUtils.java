package com.example.stepwong.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具，统一页面和接口的本地时间展示。
 */
public final class DateTimeFormatUtils {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeFormatUtils() {
    }

    public static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_TIME_FORMATTER.format(instant.atZone(DEFAULT_ZONE_ID));
    }
}
