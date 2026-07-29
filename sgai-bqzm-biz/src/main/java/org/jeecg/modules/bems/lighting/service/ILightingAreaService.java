package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;

import java.util.Collection;
import java.util.List;

public interface ILightingAreaService extends IService<LightingArea> {

    IPage<LightingArea> listPage(LightingAreaQueryDto params);

    IPage<LightingArea> listPage1(LightingAreaQueryDto params);

    void open(Long id);

    void close(Long id);

    void mqControl(String space,String areaCode,String value);

    LightingArea getByCode(String space,String areaCode);

    List<LightingArea> getByIds(Collection<Long> ids);

}
