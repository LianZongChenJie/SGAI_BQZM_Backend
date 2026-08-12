package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingProgramQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;

/**
 * 照明节目 Service
 * 节目独立存储于 lighting_program 表（从 lighting_scene 拆分），承载泛光节目ID(groupId)。
 * 场景（lighting_scene.program_scene_ids）引用节目 id，控制时按 groupId 发泛光节目MQ。
 */
public interface ILightingProgramService extends IService<LightingProgram> {

    /**
     * 分页查询节目列表
     */
    IPage<LightingProgram> listPage(LightingProgramQueryDto params);

    /**
     * 新增节目
     */
    void add(LightingProgram program);

    /**
     * 编辑节目
     */
    void edit(LightingProgram program);

    /**
     * 删除节目（被场景引用时禁止删除）
     */
    void delete(Long id);

    /**
     * 节目开/关：直接控制单个节目（按 groupId 发泛光节目MQ，onOff：1开2关），自动记录控制日志
     *
     * @param programId     节目id
     * @param operationType 开启/关闭 或 OPEN/CLOSE
     */
    void control(Long programId, String operationType);

    /**
     * 节目全开/全关：批量控制全部（或指定空间）启用状态的节目，按 groupId 逐个发泛光节目MQ，自动记录控制日志
     *
     * @param operationType 开启/关闭 或 OPEN/CLOSE
     * @param space         空间编码（可选，不传控制全部启用节目）
     * @return 实际下发的节目数
     */
    int allControl(String operationType, String space);
}
