package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

public interface ILightingAreaService extends IService<LightingArea> {

    IPage<LightingArea> listPage(LightingAreaQueryDto params);

    IPage<LightingArea> listPage1(LightingAreaQueryDto params);

    /**
     * 导出区域列表Excel（查询条件同 listPage1，不分页）
     */
    void exportExcel(LightingAreaQueryDto params, HttpServletResponse response);

    void open(Long id);

    void open(Long id, Long parentId);

    void close(Long id);

    void close(Long id, Long parentId);

    void mqControl(String space,String areaCode,String value);

    LightingArea getByCode(String space,String areaCode);

    List<LightingArea> getByIds(Collection<Long> ids);

}
