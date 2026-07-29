package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.jeecg.modules.bems.patterned.dto.DeviceAttributeOperationDto;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.entity.PatterningPoint;
import org.jeecg.modules.bems.patterned.entity.PatterningStrategy;
import org.jeecg.modules.bems.patterned.mapper.PatterningStrategyMapper;
import org.jeecg.modules.bems.patterned.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 场景控制
 */
@Service
@AllArgsConstructor
public class PatterningStrategyServiceImpl extends ServiceImpl<PatterningStrategyMapper, PatterningStrategy> implements IPatterningStrategyService {

    private final IPatterningPointService patterningPointService;

    private final IPatterningRelatedService patterningRelatedService;

    private final IPatterningExecutionTimeService patterningExecutionTimeService;

    private final IStrategyExecuteRecordService strategyExecuteRecordService;

    private final DeviceAttributeOperationService deviceAttributeOperationService;

    @Transactional
    @Override
    public boolean save(PatterningStrategy entity) {
        // 设置策略编码
        entity.setStrategyCode(String.valueOf(System.currentTimeMillis()));
        entity.setEnabledStatus(PatterningStrategy.Disable);
        //获取点位
        List<PatterningPoint> patterningPointList = entity.getPatterningPointList();
        String executeDevice = patterningPointList.stream().map(PatterningPoint::getDeviceName).collect(Collectors.joining(","));
        entity.setExecuteDevice(executeDevice);
        boolean res = super.save(entity);
        Long patterningStrategyId = entity.getId();
        //循环赋值关联字段，新增节点表
        for (PatterningPoint patterningPoint : patterningPointList) {
            patterningPoint.setPatternStrategyId(patterningStrategyId);
            patterningPointService.save(patterningPoint);
        }
        //保存前后关联中间表
        if (entity.getPatterningRelatedList() != null) {
            entity.getPatterningRelatedList().forEach(patterningRelated -> {
                if(patterningRelated.getId() != null) {
                    patterningRelated.setPreAssociationId(patterningStrategyId);
                    patterningRelatedService.save(patterningRelated);
                }
            });
        }
        // TODO 新建场景控制后续操作
        return res;
    }

    @Transactional
    @Override
    public boolean updateById(PatterningStrategy entity) {
        if (entity.getId() == null) {
            throw new JeecgBootException("请选择策略");
        }
        PatterningStrategy byId = getById(entity.getId());
        if (byId == null) {
            throw new JeecgBootException("策略不存在");
        }
        if (PatterningStrategy.Enable.equals(byId.getEnabledStatus())) {
            throw new JeecgBootException("策略启用中，无法更改");
        }

        // 更新控制设备
        patterningPointService.removeByPatterningStrategyId(entity.getId());
        patterningPointService.save(entity.getId(), entity.getPatterningPointList());
        // 更新模式引用
        patterningRelatedService.removeByPreAssociationId(entity.getId());
        patterningRelatedService.save(entity.getId(), entity.getPatterningRelatedList());
        String executeDevice = entity.getPatterningPointList().stream().map(PatterningPoint::getDeviceName).collect(Collectors.joining(","));
        entity.setExecuteDevice(executeDevice);
        // TODO 检查是否循环引用
        return super.updateById(entity);
    }

    @Override
    public PatterningStrategy getDetailById(Long id) {
        PatterningStrategy detail = getById(id);
        if (detail == null) {
            throw new JeecgBootException("策略不存在");
        }
        // 获取关联的设备点位
        detail.setPatterningPointList(patterningPointService.findByPatterningStrategyId(id));
        // 获取关联的策略
        detail.setPatterningRelatedList(patterningRelatedService.findByPreAssociationId(id));
        return detail;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        PatterningStrategy byId = getById(id);
        if (PatterningStrategy.Enable.equals(byId.getEnabledStatus())) {
            throw new JeecgBootException("策略启用中，无法删除");
        }
        patterningPointService.removeByPatterningStrategyId(id);
        patterningRelatedService.removeByPreAssociationId(id);
        patterningRelatedService.removeByPostAssociationId(id);
        super.removeById(id);
        // TODO 删除场景控制后续操作
    }

    /**
     * 启用场景控制
     */
    @Override
    @Transactional
    public void startStrategy(PatterningExecutionTime data) {
        // 判断模式类型
        Long patterningId = data.getPatterningId();
        PatterningStrategy byId = getById(patterningId);
        if (byId == null) {
            throw new JeecgBootException("策略不存在");
        }
        data.setVersion(java.util.UUID.randomUUID().toString());
        // 自动模式
        patterningExecutionTimeService.saveOrUpdate(data);
        updateEnableStatus(patterningId, PatterningStrategy.Enable);
        patterningExecutionTimeService.getNextExecution(data,LocalDate.now());
    }

    /**
     * 禁用场景控制
     *
     * @param id 场景控制id
     */
    @Override
    public void stopStrategy(Long id) {
        updateEnableStatus(id, PatterningStrategy.Disable);
    }

    /**
     * 发送设备指令
     *
     * @param id 场景控制id
     */
    @Override
    public void executeImmediately(Long id) {
        PatterningStrategy byId = getById(id);
        if (byId == null) {
            throw new JeecgBootException("策略不存在");
        }
        if (PatterningStrategy.Disable.equals(byId.getEnabledStatus())) {
            throw new JeecgBootException("策略未启用");
        }
        operationPoint(byId);
    }

    @Override
    public Page<PatterningStrategy> listPage(PatterningStrategy params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()), new LambdaQueryWrapper<PatterningStrategy>()
                .like(StringUtils.isNotEmpty(params.getStrategyName()), PatterningStrategy::getStrategyName, params.getStrategyName())
                .like(StringUtils.isNotEmpty(params.getStrategyScene()), PatterningStrategy::getStrategyScene, params.getStrategyScene())
                .like(StringUtils.isNotEmpty(params.getStrategyTarget()), PatterningStrategy::getStrategyTarget, params.getStrategyTarget())
                .eq(params.getSpaceId() != null, PatterningStrategy::getSpaceId, params.getSpaceId())
                .eq(params.getProfessionalId() != null, PatterningStrategy::getProfessionalId, params.getProfessionalId())
                .eq(StringUtils.isNotEmpty(params.getModelType()), PatterningStrategy::getModelType, params.getModelType())
                .orderByDesc(PatterningStrategy::getCreateTime)
        );
    }

    /**
     * 更新场景控制状态
     */
    private void updateEnableStatus(Long id, String enableStatus) {
        update(new LambdaUpdateWrapper<PatterningStrategy>().eq(PatterningStrategy::getId, id).set(PatterningStrategy::getEnabledStatus, enableStatus));
    }

    /**
     * 执行场景控制
     * @param id 场景控制id
     * @param executeTime 执行时间
     */
    public void executeImmediately(Long id, LocalDateTime executeTime){
        // 获取executeTime与当前时间的秒数
        long between = Math.abs(ChronoUnit.SECONDS.between(LocalDateTime.now(), executeTime));
        if(between > 600){
            throw new JeecgBootException("场景控制执行时间间隔不能超过10分钟");
        }
        PatterningStrategy byId = getById(id);
        if (byId == null) {
            throw new JeecgBootException("策略不存在");
        }
        if (PatterningStrategy.Disable.equals(byId.getEnabledStatus())) {
            throw new JeecgBootException("策略未启用");
        }
        // 获取执行时间信息
        PatterningExecutionTime executionTime = patterningExecutionTimeService.getByPatterningId(byId.getId());
        if (executionTime == null) {
            throw new JeecgBootException("场景控制未配置执行时间");
        }
        if (executionTime.getBeginDate().isAfter(executeTime.toLocalDate()) || executionTime.getEndDate().isBefore(executeTime.toLocalDate())) {
            throw new JeecgBootException("场景控制执行时间未到");
        }
        if (!executionTime.getEnabledWeek().contains(executeTime.getDayOfWeek().getValue() + "")) {
            throw new JeecgBootException("场景控制执行时间未到");
        }
        if (!executionTime.getBeginTime().equals(executeTime.toLocalTime())) {
            throw new JeecgBootException("场景控制执行时间未到");
        }
        operationPoint(byId);
    }

    private void operationPoint(PatterningStrategy strategy){
        // 当前场景关联设备点位
        List<PatterningPoint> points = patterningPointService.findByPatterningStrategyId(strategy.getId());
        List<DeviceAttributeOperationDto> operations = new ArrayList<>();
        for (PatterningPoint point : points) {
            operations.add(new DeviceAttributeOperationDto(point.getDeviceId(), point.getPointId(), point.getConditionValue()));
        }
        deviceAttributeOperationService.operationDeviceAttribute(operations);
        // 发送指令
        strategy.setPatterningPointList(points);
        //TODO 保存执行日志,延迟更新执行状态
        strategyExecuteRecordService.addLog(strategy);
    }

}
