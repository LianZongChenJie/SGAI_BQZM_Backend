package org.jeecg.modules.bems.mq.util;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 计量点位更新工具类
 */
@Service
@AllArgsConstructor
public class MeteringPointUtil {

    private final RedisTemplate<String,Object> redisTemplate;

    /**
     * 计量点位数据更新校验消息是否重复
     * @param pointId 计量点位id
     * @param hour 小时数据
     * @return true:消息不存在，可以发送
     */
    public boolean checkMessageExists(Long pointId, LocalDateTime hour){
        String key = getQueueMeteringPointValueUpdateBigMapKey(hour);
        long msgOffset = getMsgOffset(pointId, hour);
        // 判断key在redis中是否存在
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key,msgOffset))) {
            return false;
        }
        redisTemplate.opsForValue().setBit(key,getMsgOffset(pointId,hour),true);
        // 设置过期时间，防止永久占用
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        return true;
    }

    /**
     * 清除消息标记
     * @param pointId 计量点位id
     * @param hour 小时
     */
    public void clearMessageMark(Long pointId, LocalDateTime hour){
        String key = getQueueMeteringPointValueUpdateBigMapKey(hour);
        redisTemplate.opsForValue().setBit(key,getMsgOffset(pointId,hour),false);
    }

    /**
     * 计量点位数据更新校验消息是否重复
     * @param hour 小时数据
     * @return bigmap key
     */
    private String getQueueMeteringPointValueUpdateBigMapKey(LocalDateTime hour){
        return "msg_point_value_update:" + hour.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 获取消息id
     * @param pointId 计量点位id
     * @param hour 小时
     * @return 消息偏移量
     */
    private long getMsgOffset(Long pointId,LocalDateTime hour){
        // 获取hour当天的秒数
        return hour.toLocalTime().toSecondOfDay() + pointId * 100000;
    }

}
