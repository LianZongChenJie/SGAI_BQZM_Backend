package org.jeecg.modules.bems.api;

import org.jeecg.modules.bems.api.fallback.BemsDeviceFallback;
import org.jeecg.modules.bems.entity.DeviceEntity;
import org.jeecg.modules.bems.entity.DeviceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "jeecg-gateway", fallbackFactory = BemsDeviceFallback.class)
public interface BemsDeviceApi {
    /**
     * 获取设备基本信息
     * @return 设备id、设备编码、设备类型
     */
    @GetMapping(value = "/bems/device/api/deviceList")
    List<DeviceEntity> deviceList();

    /**
     * 获取设备详细信息
     * @param deviceIds 设备id
     * @return 设备详细信息
     */
    @GetMapping(value = "/bems/device/api/deviceInfoList")
    List<DeviceInfo> deviceInfoList(@RequestParam String deviceIds);
}
