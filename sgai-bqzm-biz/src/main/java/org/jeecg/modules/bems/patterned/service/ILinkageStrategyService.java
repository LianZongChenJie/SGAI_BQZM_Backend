package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.LinkageStrategy;

import java.math.BigDecimal;

public interface ILinkageStrategyService extends IService<LinkageStrategy> {

    LinkageStrategy getDetailById(Long id);

    /**
     * 启动策略
     */
    void startStrategy(Long id);

    /**
     * 禁用策略
     */
    void stopStrategy(Long id);

    IPage<LinkageStrategy> listPage(LinkageStrategy params);

    /**
     * 判断点位信息是否触发联动控制
     * @param deviceId 设备id
     * @param pointId 点位id
     * @param value 点位值
     */
    void detection(Long deviceId, Long pointId, String value);
}
