package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.dto.DataAmendLogDto;
import org.jeecg.modules.bems.dto.DataAmendParamDto;
import org.jeecg.modules.bems.entity.DataAmendLog;

public interface IDataAmendLogService extends IService<DataAmendLog> {

    IPage<DataAmendLogDto> listPage(DataAmendParamDto param);
}
