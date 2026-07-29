package org.jeecg.modules.bems.mdm.controller;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.bems.entity.DeviceEntity;
import org.jeecg.modules.bems.entity.DeviceInfo;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bems/device/api")
@AllArgsConstructor
public class DeviceApiController {

    private final IDeviceService service;
    private final IEquipmentCategoryService categoryService;
    private final ISpaceService spaceService;

    @IgnoreAuth
    @GetMapping("/deviceList")
    public List<DeviceEntity> deviceList(){
        List<DeviceEntity> result = new ArrayList<>();
        service.list().forEach(item -> {
            result.add(new DeviceEntity(item.getId(),item.getDeviceCode(),item.getCategoryId()));
        });
        return result;
    }

    @IgnoreAuth
    @GetMapping("/deviceInfoList")
    public List<DeviceInfo> deviceInfoList(@RequestParam String deviceIds){
        List<Device> devices = service.listByIds(Arrays.asList(deviceIds.split(",")));
        if (CollectionUtil.isEmpty(devices)) {
            return Collections.emptyList();
        }

        // 收集所有类别ID和空间ID
        List<Long> categoryIds = devices.stream()
                .map(Device::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        List<Long> spaceIds = devices.stream()
                .map(Device::getSpaceId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询类别和空间
        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
                categoryService.findByIds(categoryIds).stream()
                        .collect(Collectors.toMap(EquipmentCategory::getId, EquipmentCategory::getFullName));

        Map<Long, String> spaceMap = spaceIds.isEmpty() ? Collections.emptyMap() :
                spaceService.listByIds(spaceIds).stream()
                        .collect(Collectors.toMap(Space::getId, Space::getFullName));

        // 构建返回结果
        return devices.stream()
                .map(device -> new DeviceInfo(
                        device.getId(),
                        device.getDeviceCode(),
                        device.getDeviceName(),
                        categoryMap.getOrDefault(device.getCategoryId(), ""),
                        spaceMap.getOrDefault(device.getSpaceId(), "")
                ))
                .collect(Collectors.toList());
    }
}
