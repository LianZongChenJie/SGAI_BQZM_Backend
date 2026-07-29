package org.jeecg.modules.bems.constant;

import java.time.format.DateTimeFormatter;

public class RedisConstant {

    /**
     * 15分钟能耗数据
     */
    public static final String MINUTE_DATA_KEY = "energy:consumption:minute";

    /**
     * 表底值
     */
    public static final String REAL_DATA_KEY = "energy:data:real";

    /**
     * 能耗数据：小时
     */
    public static final String HOUR_DATA_KEY = "energy:data:hour";
    /**
     * 能耗数据：天
     */
    public static final String DAY_DATA_KEY = "energy:data:day";
    /**
     * 能耗数据：月
     */
    public static final String MONTH_DATA_KEY = "energy:data:month";
    /**
     * 能耗数据：年
     */
    public static final String YEAR_DATA_KEY = "energy:data:year";

    public static final int MINUTE_DATE_TTL = 3600;

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
}
