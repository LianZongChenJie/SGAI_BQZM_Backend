package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;

public interface ILightingCircuitService extends IService<LightingCircuit> {

    IPage<LightingCircuit> listPage(LightingCircuitQueryDto params);

    void open(Long id);

    void close(Long id);

    void mqControl(String space,String areaCode,String circuitCode, String status);

    /**
     * 更新通讯状态
     */
    void updateComstat(String space,String areaCode,String circuitCode,String comstat);
}
