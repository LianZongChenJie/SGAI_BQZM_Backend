package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;

public interface ILightingCircuitService extends IService<LightingCircuit> {

    IPage<LightingCircuit> listPage(LightingCircuitQueryDto params);

    void open(Long id);

    void open(Long id, Long parentId);

    void close(Long id);

    void close(Long id, Long parentId);

    void mqControl(String space,String areaCode,String circuitCode, String status);

    /**
     * 更新回路状态并维护开启/关闭时间、开启总时长（幂等：重复关闭不重复累计时长）
     */
    void applyStatus(LightingCircuit circuit, String status);

    /**
     * 更新通讯状态
     */
    void updateComstat(String space,String areaCode,String circuitCode,String comstat);

    /**
     * 记录回路控制操作人/操作时间（区域全开/全关、一键全开/全关等批量控制时调用；
     * 不改变回路状态与开关时间，状态以设备回传为准）
     */
    void recordControlOperator(LightingCircuit circuit, String operatorBy);
}
