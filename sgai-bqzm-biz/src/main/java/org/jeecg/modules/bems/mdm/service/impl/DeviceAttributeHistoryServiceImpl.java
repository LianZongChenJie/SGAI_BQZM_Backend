package org.jeecg.modules.bems.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.mdm.dto.DeviceAttributeHistoryQueryDto;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.entity.DeviceAttributeHistory;
import org.jeecg.modules.bems.mdm.mapper.DeviceAttributeHistoryMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeHistoryService;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class DeviceAttributeHistoryServiceImpl extends ServiceImpl<DeviceAttributeHistoryMapper, DeviceAttributeHistory> implements IDeviceAttributeHistoryService {

    private final IBusinessConfigService businessConfigService;
    @Override
    public List<DeviceAttributeHistory> listByAttributeId(DeviceAttributeHistoryQueryDto param) {
        if (param.getDeviceAttributeId() == null) {
            return Collections.emptyList();
        }
        return super.list(new LambdaQueryWrapper<DeviceAttributeHistory>()
                .eq(DeviceAttributeHistory::getAttributeId, param.getDeviceAttributeId())
                .between(DeviceAttributeHistory::getCollectionTime, param.getStartTime(), param.getEndTime())
                .orderByDesc(DeviceAttributeHistory::getCollectionTime)
        );
    }

    @Override
    public void saveAttributeHistory(Collection<DeviceAttribute> attributes) {
        if(attributes == null || attributes.isEmpty()){
            return;
        }
        // 获取需要保存的设备id
        List<Long> deviceIds = businessConfigService.getListByKey("device_attribute_history", Long.class);
        if(deviceIds == null || deviceIds.isEmpty()){
            return;
        }
        for (DeviceAttribute attribute : attributes) {
            if (!deviceIds.contains(attribute.getDeviceId())) {
                continue;
            }
            DeviceAttributeHistory history = new DeviceAttributeHistory();
            history.setAttributeId(attribute.getId());
            history.setDeviceId(attribute.getDeviceId());
            history.setCollectionTime(attribute.getGatherTime());
            history.setValue(attribute.getValue());
            save(history);
        }
    }
}
