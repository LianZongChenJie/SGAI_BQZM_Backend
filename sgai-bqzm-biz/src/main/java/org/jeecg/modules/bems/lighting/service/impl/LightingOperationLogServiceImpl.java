package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.lighting.dto.LightingOperationLogQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.LightingOperationLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LightingOperationLogServiceImpl extends ServiceImpl<LightingOperationLogMapper, LightingOperationLog> implements ILightingOperationLogService {

    @Override
    public void saveLog(String relType, Long relId, String name, LocalDateTime time, String operationType) {
        String operationBy = "照明计划";
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景（如MQ监听器）中SecurityManager不可用，使用默认用户
        }
        saveLog(relType, relId, name, time, operationType, operationBy);
    }

    @Override
    public void saveLog(String relType, Long relId, String name, LocalDateTime time, String operationType, String operationBy) {
        LightingOperationLog data = new LightingOperationLog();
        data.setRelType(relType);
        data.setRelId(relId);
        data.setName(name);
        data.setOperationTime(time);
        data.setOperationType(operationType);
        data.setOperationBy(operationBy);
        super.save(data);
    }

    @Override
    public IPage<LightingOperationLog> listPage(LightingOperationLogQueryDto params) {
        IPage<LightingOperationLog> result = page(new Page<>(params.getPageNo(), params.getPageSize()), new LambdaQueryWrapper<LightingOperationLog>()
                .eq(StrUtil.isNotEmpty(params.getRelType()), LightingOperationLog::getRelType, params.getRelType())
                // 操作类型模糊匹配：开/关（兼容 区域全开/回路开启/区域全关/回路关闭 等写法）
                .like(StrUtil.isNotEmpty(params.getOperationType()), LightingOperationLog::getOperationType, params.getOperationType())
                .ge(params.getStartTime() != null, LightingOperationLog::getOperationTime, params.getStartTime())
                .le(params.getEndTime() != null, LightingOperationLog::getOperationTime, params.getEndTime())
                .orderByDesc(LightingOperationLog::getOperationTime));
        // 仅输出层转换：operationType 只输出 开/关，不落库、不影响查询条件匹配
        result.getRecords().forEach(log -> {
            String type = log.getOperationType();
            if (StrUtil.isNotEmpty(type)) {
                if (type.contains("关")) {
                    log.setOperationType("关");
                } else if (type.contains("开")) {
                    log.setOperationType("开");
                }
            }
        });
        return result;
    }

}
