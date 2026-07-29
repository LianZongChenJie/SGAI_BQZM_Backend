package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.mdm.entity.DeviceModel;

import java.util.List;

public interface IDeviceModelService extends IService<DeviceModel> {

    IPage<DeviceModel> queryPage(DeviceModel params);
    List<DeviceModel> queryByCategoryId(Long categoryId);
}
