package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.mdm.dto.DeviceDto;
import org.jeecg.modules.bems.mdm.dto.DeviceRunStateStatisticsDto;
import org.jeecg.modules.bems.mdm.dto.DeviceStatusDto;
import org.jeecg.modules.bems.mdm.entity.Device;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 设备基础信息
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
public interface IDeviceService extends IService<Device> {

    IPage<Device> listPage(DeviceDto params);
    List<Device> list(DeviceDto params);
    void addDevice(Device device);

    boolean updateById(Device device);

    void updateAutomaticAlgorithm(Long id,String automaticAlgorithm);

    List<Device> findCodeAndName();

    IPage<Device> findMeasuring(Device device);

    IPage<Device> find(Device device);

    List<Device> findByDeviceIds(Collection<Long> deviceIds);

    List<Device> findMeasurementBySpaceIdAndCategoryId(String deviceName,String deviceCode,List<Long> spaceIds,List<Long> categoryIds);

    Device getByDeviceCode(String deviceCode);

    void updateStatus(DeviceStatusDto data);

    void updateStatus(String deviceCode,String status);

    Device getDetail(Long id);

    List<Device> findByType(String type);

    /**
     * 计量仪表运行状态统计
     * @return 统计结果
     */
    DeviceRunStateStatisticsDto statisticsRunState();

    DeviceRunStateStatisticsDto statisticsRunState(Long categoryId);

    /**
     * 更新设备最后采集时间
     * @param deviceCode 设备编号
     * @param time 采集时间
     */
    void updateLastGatherTime(String deviceCode, LocalDateTime time);

    List<Device> findByCategoryIds(Collection<Long> categoryIds);

    IPage<Device> findDeviceAndAttribute(DeviceDto params);

    List<Device> listDeviceAndAttribute(DeviceDto params);

}
