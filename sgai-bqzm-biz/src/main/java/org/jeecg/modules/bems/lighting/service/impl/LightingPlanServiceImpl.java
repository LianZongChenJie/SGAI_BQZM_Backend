package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingPlanDetailDto;
import org.jeecg.modules.bems.lighting.dto.LightingPlanQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.mapper.LightingPlanMapper;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LightingPlanServiceImpl extends ServiceImpl<LightingPlanMapper, LightingPlan> implements ILightingPlanService {
    private final LightingService lightingService;

    private final ILightingAreaService lightingAreaService;

    private final ILightingCircuitService lightingCircuitService;

    private final LightingSendService lightingSendService;

    private final ILightingPlanExecutionTimeService executionTimeService;

    @Override
    public IPage<LightingPlan> listPage(LightingPlanQueryDto param) {
        Page<LightingPlan> page = super.page(new Page<>(param.getPageNo(), param.getPageSize()),
                new LambdaQueryWrapper<LightingPlan>()
                        .eq(StringUtils.isNotEmpty(param.getRelType()), LightingPlan::getRelType, param.getRelType())
                        .gt(StringUtils.isNotEmpty(param.getStartTime()), LightingPlan::getExecutionTime, param.getStartTime())
                        .lt(StringUtils.isNotEmpty(param.getEndTime()), LightingPlan::getExecutionTime, param.getEndTime())
                        .orderByAsc(LightingPlan::getSort)
        );
        List<LightingPlan> records = page.getRecords();
        if(CollectionUtil.isEmpty(records)){
            return page;
        }
        // 获取计划执行配置信息
        Map<Long, LightingPlanExecutionTime> executionTimeMap = executionTimeService.getByPlanIds(records.stream().map(LightingPlan::getId).toList())
                .stream().collect(Collectors.toMap(LightingPlanExecutionTime::getPlanId, Function.identity()));
        records.forEach(plan -> {
            plan.setExecutionInfo(executionTimeMap.get(plan.getId()));
        });
        return page;
    }

    @Override
    public void add(LightingPlan plan) {
        // 检验名称是否重复
        check(plan);
        // 增加各种校验
        plan.setStatus(LightingPlan.STATUS_DISABLE);
        // 设置排序字段
        if(plan.getSort() == null){
            plan.setSort(getMaxSort() + 1);
        }
        super.save(plan);
    }

    private Long getMaxSort(){
        Page<LightingPlan> page = super.page(new Page<>(1, 1, false), new LambdaQueryWrapper<LightingPlan>().orderByDesc(LightingPlan::getSort));
        return CollectionUtil.isNotEmpty(page.getRecords()) ? page.getRecords().get(0).getSort() : 0;
    }

    @Override
    public void edit(LightingPlan plan) {
        check(plan);
        LightingPlan old = super.getById(plan.getId());
        if(old == null){
            throw new JeecgBootException("计划不存在");
        }
        if (!LightingPlan.STATUS_DISABLE.equals(old.getStatus())) {
            throw new JeecgBootException("计划已启用，不能修改");
        }
        super.updateById(plan);
    }

    @Override
    public void delete(Long id) {
        super.removeById(id);
    }

    /**
     * 照明计划执行
     * @param id 计划id
     * @param version 版本号
     */
    @Override
    public void execution(Long id,String version) {
        // 获取计划信息
        LightingPlan plan = super.getById(id);
        if(plan == null || !LightingPlan.STATUS_ENABLE.equals(plan.getStatus())){
            return;
        }

        LightingPlanExecutionTime executionTime = executionTimeService.getByPlanIdAndVersion(id,version);
        if(executionTime == null){
            return;
        }

        LocalTime time = LocalTime.now();
        long between = Math.abs(ChronoUnit.SECONDS.between(time, executionTime.getExecutionLocalTime()));
        if(between > 300){
            log.error("当前时间与计划执行时间相差>300秒，执行失败。计划id：" + id);
            return;
        }
        Set<Long> relIds = Arrays.stream(plan.getRelIds().split(",")).map(Long::parseLong).collect(Collectors.toSet());
        if(LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())){
            // 区域（场景）
            executeArea(relIds,plan.getOperationType());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds,plan.getOperationType());
        }

    }

    @Override
    @Transactional
    public void enable(LightingPlanExecutionTime data) {
        LightingPlan plan = super.getById(data.getPlanId());
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        plan.setStatus(LightingPlan.STATUS_ENABLE);
        plan.setExecutionTime(data.getExecutionTime());
        super.updateById(plan);
        executionTimeService.saveOrUpdate(data);
        plan.setExecutionTime(data.getExecutionTime());
        // 判断下次执行时间
        if (data.getExecutionLocalTime().isBefore(LocalTime.now())) {
            return;
        }
        LocalDate now = LocalDate.now();
        if(!data.getEnabledWeek().contains(String.valueOf(now.getDayOfWeek().getValue()))){
            return;
        }
        if(data.getStartLocalDate().isAfter(now) || data.getEndLocalDate().isBefore(now)){
            return;
        }
        lightingSendService.sendPlan(plan.getId(),data.getVersion(),now.atTime(data.getExecutionLocalTime()));
    }

    @Override
    public void disable(Long id) {
        LightingPlan plan = super.getById(id);
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        if(LightingPlan.STATUS_DISABLE.equals(plan.getStatus())){
            return;
        }
        plan.setStatus(LightingPlan.STATUS_DISABLE);
        super.updateById(plan);
    }

    private void executeArea(Collection<Long> areaIds,String operationType){
        for(Long areaId : areaIds){
            if(LightingPlan.OPERATION_TYPE_OPEN.equals(operationType)){
                lightingAreaService.open(areaId);
            }else if(LightingPlan.OPERATION_TYPE_CLOSE.equals(operationType)){
                lightingAreaService.close(areaId);
            }
        }
    }

    private void executeCircuit(Collection<Long> circuitIds,String operationType){
        for(Long circuitId : circuitIds){
            if(LightingPlan.OPERATION_TYPE_OPEN.equals(operationType)){
                lightingCircuitService.open(circuitId);
            }else if(LightingPlan.OPERATION_TYPE_CLOSE.equals(operationType)){
                lightingCircuitService.close(circuitId);
            }
        }
    }
    private void check(LightingPlan plan){
        if(count(new LambdaQueryWrapper<LightingPlan>().ne(plan.getId() != null, LightingPlan::getId, plan.getId()).eq(LightingPlan::getPlanName, plan.getPlanName())) > 0){
            throw new JeecgBootException("名称重复");
        }
    }

    @Override
    public LightingPlanDetailDto getDetail(Long id) {
        // 查询计划基本信息
        LightingPlan plan = super.getById(id);
        if (plan == null) {
            throw new JeecgBootException("计划不存在");
        }

        // 查询执行时间配置
        LightingPlanExecutionTime executionTime = executionTimeService.getByPlanId(id);

        // 构建 DTO
        LightingPlanDetailDto dto = new LightingPlanDetailDto();
        dto.setId(plan.getId());
        dto.setPlanName(plan.getPlanName());
        dto.setRelType(plan.getRelType());
        dto.setOperationType(plan.getOperationType());
        dto.setStatus(plan.getStatus());
        dto.setExecutionTime(plan.getExecutionTime());

        if (executionTime != null) {
            dto.setStartDate(executionTime.getStartDate());
            dto.setEndDate(executionTime.getEndDate());
            dto.setEnabledWeek(executionTime.getEnabledWeek());
            dto.setVersion(executionTime.getVersion());
        }

        // 查询关联信息
        if (StringUtils.isNotEmpty(plan.getRelIds())) {
            List<Long> relIds = Arrays.stream(plan.getRelIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())) {
                // 查询区域列表
                dto.setAreaList(lightingAreaService.getByIds(relIds));
            } else if (LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())) {
                // 查询回路列表
                List<LightingCircuit> circuits = lightingCircuitService.listByIds(relIds);
                // 查询区域
                Map<Long,LightingArea> areaMap = lightingAreaService.listByIds(circuits.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet()))
                                .stream()
                                        .collect(Collectors.toMap(LightingArea::getId,Function.identity()));
                for(LightingCircuit circuit : circuits){
                    LightingArea area = areaMap.get(circuit.getAreaId());
                    if(area != null){
                        circuit.setAreaName(area.getAreaName());
                        circuit.setSpaceName(area.getSpaceName());
                    }
                }
                dto.setCircuitList(circuits);
            }
        }

        return dto;
    }

    /**
     * 现在执行
     *
     * @param id 计划id
     */
    @Override
    public void executionNow(Long id) {
        LightingPlan plan = super.getById(id);
        if(plan == null){
            throw new JeecgBootException("计划不存在");
        }
        Set<Long> relIds = Arrays.stream(plan.getRelIds().split(",")).map(Long::parseLong).collect(Collectors.toSet());
        if(LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())){
            // 区域（场景）
            executeArea(relIds,plan.getOperationType());
        }else if(LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())){
            // 回路
            executeCircuit(relIds,plan.getOperationType());
        }
    }
}
