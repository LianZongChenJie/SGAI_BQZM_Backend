package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.mdm.entity.DeviceModelAttribute;

import java.util.List;

public interface IDeviceModelAttributeService extends IService<DeviceModelAttribute> {

    IPage<DeviceModelAttribute> queryPage(DeviceModelAttribute params);

    List<DeviceModelAttribute> listByModelId(Long modelId);
}
