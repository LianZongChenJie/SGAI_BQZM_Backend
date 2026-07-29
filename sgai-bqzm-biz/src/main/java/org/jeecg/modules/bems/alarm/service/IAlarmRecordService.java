package org.jeecg.modules.bems.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.alarm.dto.AlarmRecordDto;
import org.jeecg.modules.bems.alarm.dto.TransferEventDto;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.vo.AlarmRecordStatisticsVo;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;

import java.time.LocalDateTime;
import java.util.List;

public interface IAlarmRecordService extends IService<AlarmRecord> {
    IPage<AlarmRecord> listPage(AlarmRecordDto params);

    void elimination(Long id);

    List<AlarmRecordStatisticsVo> levelStatistics(AlarmRecordDto params);

    /**
     * 查询时间范围内的告警记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 告警记录
     */
    List<AlarmRecord> listByAlarmTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询时间范围内的告警记录数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 告警记录数量
     */
    Long countByAlarmTimeRange(LocalDateTime startTime,LocalDateTime endTime);

    /**
     * 设备告警检测
     * @param deviceId 设备id
     * @param pointId 点位id
     * @param value 点位值
     */
    void alarmDetection(Long deviceId, Long pointId, String value);

    /**
     * 设备告警检测
     * @param deviceId 设备id
     * @param deviceAttributes 设备点位
     */
    void alarmDetection(Long deviceId, List<DeviceAttribute> deviceAttributes);

    /**
     * 设备告警检测
     * @param deviceId 设备id
     * @param timeGranularity 时间粒度
     * @param value 值
     */
    void alarmDetection(Long deviceId,String timeGranularity,String value);

    /**
     * 设备告警检测
     * @param deviceId 设备id
     * @param hour 小时
     */
    void alarmDetection(Long deviceId,LocalDateTime hour);

    /**
     * 计量规则点位告警检测
     * @param meteringPointId 计量规则点位id
     * @param hour 小时
     */
    void alarmDetectionForMeteringPoint(Long meteringPointId,LocalDateTime hour);

    /**
     * 转事件工单
     * @param data
     */
    void transferEvent(TransferEventDto data);

    /**
     * 已处理
     * @param eventId 事件id
     */
    void completed(String eventId);
}
