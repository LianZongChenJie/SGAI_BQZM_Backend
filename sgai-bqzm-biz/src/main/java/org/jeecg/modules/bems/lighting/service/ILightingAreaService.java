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

    /**
     * 撤回区域MQ下发消息：只删除该区域下发、且未被消费的消息（不影响同一队列中其他区域的消息）
     * @param id 区域id
     * @return 撤回的消息总数
     */
    int recallMqMessages(Long id);

    void close(Long id);

    void close(Long id, Long parentId);

    void mqControl(String space,String areaCode,String value);

    LightingArea getByCode(String space,String areaCode);

    List<LightingArea> getByIds(Collection<Long> ids);

    /**
     * 按区域查询1号馆的所有区域：查询条件写死为 space_name='1号馆'，id 参数仅为兼容前端调用（不参与查询）
     *
     * @param id 区域id（前端固定传，如 478）
     */
    List<LightingArea> listBySpaceName(Long id);

    /**
     * 按空间名称控制该空间下所有回路的开/关（走1号馆902控制逻辑）
     * 等价于 SELECT * FROM lighting_area WHERE space_name = #{spaceName}，再遍历各区域下所有回路逐个下发
     *
     * @param spaceName 空间名称（如：1号馆）
     * @param type true=开，false=关
     */
    void controlBySpaceName(String spaceName, boolean type);

}
