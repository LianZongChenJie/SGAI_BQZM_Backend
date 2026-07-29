package org.jeecg.modules.bems.mdm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.mdm.dto.DeviceStaticDataDto;
import org.jeecg.modules.bems.mdm.entity.DeviceStaticData;
import org.jeecg.modules.bems.vo.DeviceStaticDataVo;

import java.util.List;

public interface IDeviceStaticDataService extends IService<DeviceStaticData> {

    List<DeviceStaticDataVo> list(String type, Long deviceId);

    boolean save(DeviceStaticDataDto data);
}
