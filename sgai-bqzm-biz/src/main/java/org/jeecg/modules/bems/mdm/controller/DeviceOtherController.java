package org.jeecg.modules.bems.mdm.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.mdm.dto.DeviceDto;
import org.jeecg.modules.bems.mdm.dto.SpaceDeviceDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bems/device/other")
@AllArgsConstructor
public class DeviceOtherController {

    private final IDeviceService deviceService;

    private final ISpaceService spaceService;

    /**
     * 获取所有空间下设备
     * @param categoryId 设备类别id
     */
    @GetMapping("/findSpaceDeviceByCategoryId")
    public Result<List<SpaceDeviceDto>> findSpaceDeviceByCategoryId(Long categoryId){
        List<Device> devices = findDeviceByCategoryId(categoryId);
        if(devices.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        // 获取空间信息
        List<Space> spaces = spaceService.list();

        // 将设备按空间ID分组
        Map<Long, List<Device>> deviceMap = devices.stream()
                .collect(Collectors.groupingBy(Device::getSpaceId));

        // 找出有设备的空间ID集合（包括所有父节点）
        Set<Long> validSpaceIds = new HashSet<>(deviceMap.keySet());
        for (Long spaceId : deviceMap.keySet()) {
            addParentSpaceIds(validSpaceIds, spaces, spaceId);
        }

        // 过滤出有效的空间节点
        List<Space> validSpaces = spaces.stream()
                .filter(space -> validSpaceIds.contains(space.getId()))
                .collect(Collectors.toList());

        // 构建空间树
        List<SpaceDeviceDto> tree = buildSpaceTree(validSpaces, deviceMap);

        return Result.ok(tree);
    }

    /**
     * 添加父节点空间ID（非递归）
     */
    private void addParentSpaceIds(Set<Long> validSpaceIds, List<Space> spaces, Long spaceId) {
        Long currentSpaceId = spaceId;
        // 构建空间ID到空间的Map，便于快速查找
        Map<Long, Space> spaceMap = spaces.stream()
                .collect(Collectors.toMap(Space::getId, s -> s));

        // 循环向上查找父节点
        while (currentSpaceId != null) {
            Space space = spaceMap.get(currentSpaceId);
            if (space == null || space.getPid() == null) {
                break;
            }
            // 如果父节点不在有效集合中，添加并继续向上查找
            if (!validSpaceIds.contains(space.getPid())) {
                validSpaceIds.add(space.getPid());
                currentSpaceId = space.getPid();
            } else {
                // 父节点已在集合中，无需继续
                break;
            }
        }
    }

    /**
     * 构建空间树（非递归）
     * @param spaces 所有空间列表
     * @param deviceMap 设备按空间ID分组的Map
     * @return 空间设备树
     */
    private List<SpaceDeviceDto> buildSpaceTree(List<Space> spaces, Map<Long, List<Device>> deviceMap) {
        // 构建空间ID到SpaceDto的Map
        Map<Long, SpaceDeviceDto> dtoMap = new HashMap<>();

        // 第一遍：创建所有节点并设置基本信息和设备
        for (Space space : spaces) {
            SpaceDeviceDto dto = new SpaceDeviceDto();
            dto.setSpaceId(space.getId());
            dto.setSpaceName(space.getSpaceName());
            dto.setDevices(deviceMap.getOrDefault(space.getId(), Collections.emptyList()));
            dto.setChildren(new ArrayList<>());
            dtoMap.put(space.getId(), dto);
        }

        // 第二遍：建立父子关系
        List<SpaceDeviceDto> roots = new ArrayList<>();
        Map<Long, List<SpaceDeviceDto>> childrenMap = new HashMap<>();

        for (Space space : spaces) {
            SpaceDeviceDto dto = dtoMap.get(space.getId());
            if (space.getPid() == null || space.getPid() == 0) {
                // 根节点
                roots.add(dto);
            } else {
                // 子节点，添加到父节点的children中
                childrenMap.computeIfAbsent(space.getPid(), k -> new ArrayList<>()).add(dto);
            }
        }

        // 第三遍：将子节点挂载到父节点
        for (Map.Entry<Long, List<SpaceDeviceDto>> entry : childrenMap.entrySet()) {
            SpaceDeviceDto parentDto = dtoMap.get(entry.getKey());
            if (parentDto != null) {
                parentDto.setChildren(entry.getValue());
            }
        }

        return roots;
    }

    private List<Device> findDeviceByCategoryId(Long categoryId){
        if(categoryId == null){
            return Collections.emptyList();
        }
        DeviceDto param = new DeviceDto();
        param.setCategoryId(categoryId);
        return deviceService.list(param);
    }

}
