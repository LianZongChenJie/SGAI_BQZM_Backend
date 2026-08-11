package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
