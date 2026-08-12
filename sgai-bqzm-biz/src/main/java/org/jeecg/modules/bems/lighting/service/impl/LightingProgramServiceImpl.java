package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingProgramQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.mapper.LightingProgramMapper;
import org.jeecg.modules.bems.lighting.mapper.LightingSceneMapper;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.ILightingProgramService;
import org.jeecg.modules.bems.lighting.service.YelIotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 照明节目 Service 实现
 */
@Service
@AllArgsConstructor
@Slf4j
public class LightingProgramServiceImpl extends ServiceImpl<LightingProgramMapper, LightingProgram> implements ILightingProgramService {

    private final LightingSceneMapper lightingSceneMapper;

    private final LightingSendService lightingSendService;

    private final ILightingOperationLogService lightingOperationLogService;

    private final YelIotService yelIotService;

    @Override
    public IPage<LightingProgram> listPage(LightingProgramQueryDto params) {
        return super.page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<LightingProgram>()
                        .like(StringUtils.isNotEmpty(params.getProgramName()), LightingProgram::getProgramName, params.getProgramName())
                        .eq(StringUtils.isNotEmpty(params.getGroupId()), LightingProgram::getGroupId, params.getGroupId())
                        .eq(StringUtils.isNotEmpty(params.getSpace()), LightingProgram::getSpace, params.getSpace())
                        .eq(StringUtils.isNotEmpty(params.getStatus()), LightingProgram::getStatus, params.getStatus())
                        .eq(StringUtils.isNotEmpty(params.getTagId()), LightingProgram::getTagId, params.getTagId())
                        .like(StringUtils.isNotEmpty(params.getTagName()), LightingProgram::getTagName, params.getTagName())
                        .orderByAsc(LightingProgram::getSort));
    }

    /**
     * 获取全部节目列表（不分页，供前端下拉选择），并回填节目运行状态
     */
    @Override
    public List<LightingProgram> list() {
        List<LightingProgram> list = super.list();
        fillProgramState(list);
        return list;
    }

    /**
     * 查询泛光总控系统所有节目状态，按 groupId 匹配回填节目状态字段（1→运行中，其余/未匹配→停止）
     */
    private void fillProgramState(List<LightingProgram> programs) {
        if (CollectionUtil.isEmpty(programs)) {
            return;
        }
        Map<String, Integer> stateMap = new HashMap<>();
        for (JSONObject st : yelIotService.getGroupStates()) {
            String id = st.getString("id");
            Integer state = st.getInteger("state");
            if (StringUtils.isEmpty(id) || state == null) {
                continue;
            }
            // 兼容 id 带 yel_ 前缀（mock 样例数据）的情况
            String groupId = id.startsWith("yel_") ? id.substring("yel_".length()) : id;
            stateMap.put(groupId, state);
        }
        for (LightingProgram program : programs) {
            Integer state = StringUtils.isEmpty(program.getGroupId()) ? null : stateMap.get(program.getGroupId().trim());
            program.setProgramState(convertGroupState(state));
        }
    }

    /**
     * 节目状态转文字：1→运行中，0/未匹配/未知→停止
     */
    private String convertGroupState(Integer state) {
        return state != null && state == 1 ? "运行中" : "停止";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(LightingProgram program) {
        if (StringUtils.isEmpty(program.getProgramName())) {
            throw new JeecgBootException("节目名称不能为空");
        }
        if (StringUtils.isEmpty(program.getGroupId())) {
            throw new JeecgBootException("泛光节目ID(groupId)不能为空");
        }
        program.setStatus(StringUtils.isEmpty(program.getStatus()) ? LightingProgram.STATUS_ENABLE : program.getStatus());
        super.save(program);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(LightingProgram program) {
        if (program.getId() == null) {
            throw new JeecgBootException("节目id不能为空");
        }
        if (super.getById(program.getId()) == null) {
            throw new JeecgBootException("节目不存在");
        }
        super.updateById(program);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LightingProgram program = super.getById(id);
        if (program == null) {
            throw new JeecgBootException("节目不存在");
        }
        // 被场景引用（lighting_scene.program_scene_ids 包含该节目ID）时禁止删除
        List<LightingScene> scenes = lightingSceneMapper.selectList(
                new LambdaQueryWrapper<LightingScene>().isNotNull(LightingScene::getProgramSceneIds));
        if (CollectionUtil.isNotEmpty(scenes)) {
            String idStr = String.valueOf(id);
            for (LightingScene scene : scenes) {
                for (String s : scene.getProgramSceneIds().split(",")) {
                    if (idStr.equals(s.trim())) {
                        throw new JeecgBootException("节目【" + program.getProgramName() + "】已被场景【" + scene.getSceneName() + "】引用，无法删除（请先从场景中移除）");
                    }
                }
            }
        }
        super.removeById(id);
    }

    @Override
    public void control(Long programId, String operationType) {
        if (programId == null) {
            throw new JeecgBootException("节目id不能为空");
        }
        LightingProgram program = super.getById(programId);
        if (program == null) {
            throw new JeecgBootException("节目不存在");
        }
        // 操作类型兼容：开启/关闭 或 OPEN/CLOSE
        String op = operationType;
        if ("OPEN".equalsIgnoreCase(op)) {
            op = LightingProgram.OPERATION_TYPE_OPEN;
        } else if ("CLOSE".equalsIgnoreCase(op)) {
            op = LightingProgram.OPERATION_TYPE_CLOSE;
        }
        if (!LightingProgram.OPERATION_TYPE_OPEN.equals(op) && !LightingProgram.OPERATION_TYPE_CLOSE.equals(op)) {
            throw new JeecgBootException("operationType 必须为 开启/关闭 或 OPEN/CLOSE");
        }
        if (StringUtils.isEmpty(program.getGroupId())) {
            throw new JeecgBootException("节目【" + program.getProgramName() + "】未配置泛光节目ID(groupId)，无法控制");
        }
        // 按 groupId 发泛光节目MQ（onOff：1开2关）
        int onOff = LightingProgram.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;
        log.info("控制节目【{}】{}，发送泛光节目MQ：groupId={}", program.getProgramName(), op, program.getGroupId());
        lightingSendService.sendGroupOper(program.getGroupId(), onOff, program.getId(), program.getProgramName());
        // 记录节目控制日志（顶层日志，操作类型=手动）
        LightingOperationLog programLog = new LightingOperationLog();
        programLog.setLogType(LightingOperationLog.LOG_TYPE_PROGRAM);
        programLog.setParentId(null);
        programLog.setRelType(LightingOperationLog.LOG_TYPE_PROGRAM);
        programLog.setRelId(program.getId());
        programLog.setName(program.getProgramName());
        programLog.setOperationTime(LocalDateTime.now());
        programLog.setOperationType("节目" + op);
        programLog.setOperationBy(resolveOperationBy());
        programLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
        lightingOperationLogService.save(programLog);
    }

    /**
     * 节目全开/全关：批量控制全部（或指定空间）启用状态的节目，按 groupId 逐个发泛光节目MQ，自动记录控制日志。
     * 不加 @Transactional——与场景/区域控制一致，避免"MQ 已下发但日志被回滚"的不一致。
     */
    @Override
    public int allControl(String operationType, String space) {
        // 操作类型兼容：开启/关闭 或 OPEN/CLOSE
        String op = operationType;
        if ("OPEN".equalsIgnoreCase(op)) {
            op = LightingProgram.OPERATION_TYPE_OPEN;
        } else if ("CLOSE".equalsIgnoreCase(op)) {
            op = LightingProgram.OPERATION_TYPE_CLOSE;
        }
        if (!LightingProgram.OPERATION_TYPE_OPEN.equals(op) && !LightingProgram.OPERATION_TYPE_CLOSE.equals(op)) {
            throw new JeecgBootException("operationType 必须为 开启/关闭 或 OPEN/CLOSE");
        }
        // 全部启用状态节目（可选按空间过滤），按 sort 升序保证下发顺序稳定
        List<LightingProgram> programs = super.list(new LambdaQueryWrapper<LightingProgram>()
                .eq(StringUtils.isNotEmpty(space), LightingProgram::getSpace, space)
                .eq(LightingProgram::getStatus, LightingProgram.STATUS_ENABLE)
                .orderByAsc(LightingProgram::getSort));
        if (CollectionUtil.isEmpty(programs)) {
            log.info("节目全开/全关：没有可控制的启用节目，space={}", space);
            return 0;
        }

        // 记录父日志（logType=节目，操作类型=节目全开/全关）
        // 注意：lighting_operation_log.rel_id 非空约束，批量操作无单一关联id，占位0（子日志带各节目真实id）
        LightingOperationLog parentLog = new LightingOperationLog();
        parentLog.setLogType(LightingOperationLog.LOG_TYPE_PROGRAM);
        parentLog.setParentId(null);
        parentLog.setRelType(LightingOperationLog.LOG_TYPE_PROGRAM);
        parentLog.setRelId(0L);
        parentLog.setName("节目" + op);
        parentLog.setOperationTime(LocalDateTime.now());
        parentLog.setOperationType("节目" + op);
        parentLog.setOperationBy(resolveOperationBy());
        parentLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
        lightingOperationLogService.save(parentLog);

        int onOff = LightingProgram.OPERATION_TYPE_OPEN.equals(op) ? 1 : 2;
        int count = 0;
        List<LightingOperationLog> childLogs = new ArrayList<>();
        for (LightingProgram program : programs) {
            if (StringUtils.isEmpty(program.getGroupId())) {
                log.warn("节目全开/全关：节目【{}】未配置泛光节目ID(groupId)，跳过", program.getProgramName());
                continue;
            }
            log.info("节目全开/全关：下发节目【{}】{}，groupId={}", program.getProgramName(), op, program.getGroupId());
            lightingSendService.sendGroupOper(program.getGroupId(), onOff, program.getId(), program.getProgramName());
            // 子日志挂在父日志下
            LightingOperationLog childLog = new LightingOperationLog();
            childLog.setLogType(LightingOperationLog.LOG_TYPE_PROGRAM);
            childLog.setParentId(parentLog.getId());
            childLog.setRelType(LightingOperationLog.LOG_TYPE_PROGRAM);
            childLog.setRelId(program.getId());
            childLog.setName(program.getProgramName());
            childLog.setOperationTime(LocalDateTime.now());
            childLog.setOperationType("节目" + op);
            childLog.setOperationBy(parentLog.getOperationBy());
            childLog.setOperatorType(LightingOperationLog.OPERATOR_TYPE_MANUAL);
            childLogs.add(childLog);
            count++;
        }
        if (CollectionUtil.isNotEmpty(childLogs)) {
            lightingOperationLogService.saveBatchLog(childLogs);
        }
        log.info("节目全开/全关完成：space={}, 操作={}, 共下发 {} 个节目", space, op, count);
        return count;
    }

    /**
     * 取当前登录用户，异步/无登录上下文时用默认用户
     */
    private String resolveOperationBy() {
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        return operationBy;
    }
}
