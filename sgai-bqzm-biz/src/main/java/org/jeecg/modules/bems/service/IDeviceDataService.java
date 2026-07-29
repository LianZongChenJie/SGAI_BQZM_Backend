package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bems.dto.DeviceDataFindDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.vo.DeviceDataVo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IDeviceDataService {

    IPage<DeviceDataVo> findList(DeviceDataFindDto params);

    IPage<DeviceDataVo> findList1(DeviceDataFindDto params);

    IPage<DeviceDataVo> deviceStatusList(DeviceDataFindDto params);

    /**
     * 计算设备能耗
     * @param device 设备di
     * @param time 时间
     * @param value 表底值
     */
    void calculateValue(Device device, LocalDateTime time, BigDecimal value);

    /**
     * 计算设备能耗
     * @param deviceCode 设备编码
     * @param time 时间
     * @param value 表底值
     */
    void calculateValue(String deviceCode,LocalDateTime time,BigDecimal value);

    /**
     * 计算设备能耗-日
     * @param deviceCode 设备编码
     * @param time 时间
     * @param value 表底值
     */
    void calculateValueDay(String deviceCode,LocalDateTime time,BigDecimal value);

    /**
     * 能耗数据修正
     *  修改小时数据值，同步修改日数据值，同步修改月数据值，同步修改年数据值
     * @param id 小时数据id，data_hour 主键
     * @param value 最终值
     */
    void dataAmend(Long id,BigDecimal value);
}
