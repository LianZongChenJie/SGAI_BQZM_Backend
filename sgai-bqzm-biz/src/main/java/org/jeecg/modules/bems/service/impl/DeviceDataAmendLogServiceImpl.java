package org.jeecg.modules.bems.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.modules.bems.constant.LogConstant;
import org.jeecg.modules.bems.service.IDeviceDataAmendLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DeviceDataAmendLogServiceImpl implements IDeviceDataAmendLogService {

    private final BaseCommonService baseCommonService;

    /**
     * 保存数据修正日志
     * @param content 内容
     * @param type 自动修正：101；手动修正：102；
     */
    @Async
    public void saveAmendLog(String content,int type){
        baseCommonService.addLog(content, LogConstant.LOG_TYPE_DATA_AMEND, type);
    }
}
