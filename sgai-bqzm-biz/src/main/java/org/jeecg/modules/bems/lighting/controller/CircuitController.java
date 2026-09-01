package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingPlanExecutionTime;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.jeecg.modules.bems.permission.annotation.ButtonPermission;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecg.modules.bems.permission.holder.DataPermissionHolder;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 泛光照明-回路
 */
@Api(tags = "照明-回路控制")
@Slf4j
@RestController
@RequestMapping("/bems/lighting/circuit")
@AllArgsConstructor
public class CircuitController {

    private final ILightingCircuitService service;

    private final ILightingAreaService areaService;

    private final ILightingDistrictService districtService;

    private final ILightingPlanService planService;

    @ApiOperation("分页查询回路列表")
    @GetMapping("/listPage")
    public Result<IPage<LightingCircuit>> listPage(LightingCircuitQueryDto param){
        return Result.ok(service.listPage(param));
    }

    @ApiOperation("查询所有回路（含区域名称）")
    @DataPermission
    @GetMapping("/all")
    public Result<List<LightingCircuit>> all(){
        List<LightingCircuit> circuits = service.list();
        Map<Long,LightingArea> areaMap = areaService.list()
                .stream().collect(Collectors.toMap(LightingArea::getId, Function.identity()));
        // 数据权限：回路表无 district_id，需通过"回路->区域->片区"过滤
        // 取当前用户允许的片区(district)集合，为空则不过滤（看全部）
        UserDataScope dataScope = DataPermissionHolder.getDataScope();
        Set<Long> allowedDistricts = dataScope != null ? dataScope.getPermissionIds("DISTRICT") : null;
        if (allowedDistricts != null && !allowedDistricts.isEmpty()) {
            circuits.removeIf(c -> {
                LightingArea area = areaMap.get(c.getAreaId());
                // 回路无对应区域 或 区域不在允许片区 -> 过滤掉
                return area == null || area.getDistrictId() == null || !allowedDistricts.contains(area.getDistrictId());
            });
        }
        // 片区 map（用于回路返回片区id和名称）
        Map<Long, LightingDistrict> districtMap = districtService.list()
                .stream().collect(Collectors.toMap(LightingDistrict::getId, Function.identity()));
        for (LightingCircuit circuit : circuits) {
            LightingArea area = areaMap.get(circuit.getAreaId());
            if(area != null){
                circuit.setAreaName(area.getAreaName());
                circuit.setSpaceName(area.getSpaceName());
                circuit.setDistrictId(area.getDistrictId());
                LightingDistrict district = area.getDistrictId() != null ? districtMap.get(area.getDistrictId()) : null;
                if(district != null){
                    circuit.setDistrictName(district.getDistrictName());
                }
            }
        }
        return Result.ok(circuits);
    }

    /**
     * 开启回路
     * @param id 回路id
     */
    @ApiOperation("开启回路")
    @ButtonPermission("northAreaLighting:switch")
    @PostMapping("/open")
    public Result<String> open(@RequestParam Long id){
        service.open(id);
        return Result.ok();
    }

    /**
     * 关闭回路
     * @param id 回路id
     */
    @ApiOperation("关闭回路")
    @ButtonPermission("northAreaLighting:switch")
    @PostMapping("/close")
    public Result<String> close(@RequestParam Long id){
        service.close(id);
        return Result.ok();
    }

    /**
     * 回路定时控制 - 创建定时计划
     * @param id 回路id
     * @param executionTime 执行时间 HH:mm:ss
     * @param operationType 操作类型：开启、关闭
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束日期 yyyy-MM-dd
     * @param enabledWeek 启用星期 "1,2,3,4,5,6,7" (1=周一 ... 7=周日)
     * @param planName 计划名称（可选，自动生成）
     */
    @ApiOperation("回路定时控制-创建定时计划")
    @PostMapping("/schedule")
    public Result<String> schedule(@RequestParam Long id,
                                    @RequestParam String executionTime,
                                    @RequestParam String operationType,
                                    @RequestParam String startDate,
                                    @RequestParam String endDate,
                                    @RequestParam String enabledWeek,
                                    @RequestParam(required = false) String planName){
        LightingCircuit circuit = service.getById(id);
        if(circuit == null){
            return Result.error("回路不存在");
        }

        LightingPlan plan = new LightingPlan();
        plan.setPlanName(planName != null ? planName : circuit.getCircuitName() + "-" + operationType);
        plan.setRelType(LightingPlan.REL_TYPE_CIRCUIT);
        plan.setRelIds(String.valueOf(id));
        plan.setOperationType(operationType);
        plan.setExecutionTime(executionTime);
        plan.setPlanType("定时任务");
        plan.setCycleType("自定义");

        planService.add(plan);

        LightingPlanExecutionTime executionTimeEntity = new LightingPlanExecutionTime();
        executionTimeEntity.setPlanId(plan.getId());
        executionTimeEntity.setExecutionTime(executionTime);
        executionTimeEntity.setStartDate(startDate);
        executionTimeEntity.setEndDate(endDate);
        executionTimeEntity.setEnabledWeek(enabledWeek);

        planService.enable(executionTimeEntity);

        return Result.ok("定时设置成功");
    }

}