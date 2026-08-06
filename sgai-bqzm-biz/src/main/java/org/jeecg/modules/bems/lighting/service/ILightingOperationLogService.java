package org.jeecg.modules.bems.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.dto.LightingOperationLogQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ILightingOperationLogService extends IService<LightingOperationLog> {

    /**
     * 保存操作记录（自动获取当前登录用户作为操作人）
     * @param logType 日志类型：场景、定时任务、区域、回路
     * @param parentId 父日志ID，顶层日志传null
     * @param relType 关联类型
     * @param relId 关联id
     * @param name 名称
     * @param time 时间
     * @param operationType 操作类型
     */
    void saveLog(String logType, Long parentId, String relType, Long relId, String name, LocalDateTime time, String operationType);

    /**
     * 保存操作记录（指定操作人，用于定时器等非用户触发的场景）
     * @param logType 日志类型
     * @param parentId 父日志ID
     * @param relType 关联类型
     * @param relId 关联id
     * @param name 名称
     * @param time 时间
     * @param operationType 操作类型
     * @param operationBy 操作人
     */
    void saveLog(String logType, Long parentId, String relType, Long relId, String name, LocalDateTime time, String operationType, String operationBy);

    /**
     * 批量保存子日志
     * @param logList 日志列表
     */
    void saveBatchLog(List<LightingOperationLog> logList);

    /**
     * 分页查询控制日志（默认只查顶层日志）
     * 支持按日志类型、关联类型、操作类型、操作时间段筛选
     */
    IPage<LightingOperationLog> listPage(LightingOperationLogQueryDto params);

    /**
     * 查询日志详情（包含子日志列表）
     * @param id 日志ID
     * @return 日志详情（带子日志列表）
     */
    LightingOperationLog getDetail(Long id);

    /**
     * 查询子日志列表
     * @param parentId 父日志ID
     * @return 子日志列表
     */
    List<LightingOperationLog> getChildren(Long parentId);
}
