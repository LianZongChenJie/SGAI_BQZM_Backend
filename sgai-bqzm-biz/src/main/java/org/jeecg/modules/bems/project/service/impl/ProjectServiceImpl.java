package org.jeecg.modules.bems.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataDay;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointDataMonth;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataDayService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataHourService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointDataMonthService;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointService;
import org.jeecg.modules.bems.mdm.constant.DeviceConstant;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationData;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationResult;
import org.jeecg.modules.bems.project.dto.ProjectQueryDto;
import org.jeecg.modules.bems.project.dto.ProjectTypeEnergyConservationData;
import org.jeecg.modules.bems.project.vo.ProjectCountVo;
import org.jeecg.modules.bems.project.entity.Project;
import org.jeecg.modules.bems.project.mapper.ProjectMapper;
import org.jeecg.modules.bems.project.service.IProjectService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 项目管理
 * @Author: jeecg-boot
 * @Date:   2025-05-26
 * @Version: V1.0
 */
@Service
@AllArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

    private final IMeteringPointDataDayService meteringPointDataDayService;

    private final IMeteringPointService meteringPointService;

    private final ISysBaseAPI sysBaseAPI;

    @Override
    public ProjectCountVo queryProjectStatistics() {
        ProjectCountVo projectCountDto = new ProjectCountVo();
        LocalDateTime now = LocalDateTime.now();
        List<Project> list = super.list();
        long completedCount = list.stream()
                .filter(Project::isCompleted)
                .count();
        long totalCount = list.size();
        BigDecimal reduce = list.stream().map(Project::getProjectBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        projectCountDto.setInvestmentAmountCount(reduce);
        projectCountDto.setCompletedCount(completedCount);
        projectCountDto.setTotalCount(totalCount);
        return projectCountDto;
    }

    @Override
    public IPage<Project> queryPage(ProjectQueryDto params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<Project>()
                        .like(StringUtils.isNotEmpty(params.getProjectName()), Project::getProjectName, params.getProjectName())
                        .le(params.getStartDate() != null, Project::getProjectEstablishmentTime, params.getStartDate())
                        .ge(params.getEndDate() != null, Project::getProjectEstablishmentTime, params.getEndDate())
        );
    }

    @Override
    public ProjectEnergyConservationResult energyConservationStatistics() {
        // 这块是要改成按照仪表分类来进行统计
        LocalDate now = LocalDate.now();
        // 获取已完成项目
        List<Project> list = super.list().stream().filter(Project::isCompleted).toList();
        Map<Long,Map<String,BigDecimal>> data = new HashMap<>();
        data.put(DeviceConstant.CATEGORY_ELECTRICITY,new HashMap<>());
        data.put(DeviceConstant.CATEGORY_WATER,new HashMap<>());
        for(Project project : list){
            Long pointId = project.getPointId();
            MeteringPoint point = meteringPointService.getById(pointId);
            if(point == null){
                continue;
            }
            Map<String, BigDecimal> map = data.getOrDefault(point.getCategoryId(), new HashMap<>());
            // 获取结项时间
            LocalDateTime projectCompletionTime = project.getMeasurementTime();
            LocalDateTime projectEstablishmentTime = project.getProjectEstablishmentTime();
            List<MeteringPointDataDay> dayData = meteringPointDataDayService.findByTimeRangeAndPointId(projectCompletionTime.toLocalDate(), now, pointId);
            // 获取立项前日期平均值
            BigDecimal avg = meteringPointDataDayService.findAvgByLtTimeAndPointId(projectEstablishmentTime.toLocalDate(), pointId);
            // 节能量
            BigDecimal sum = dayData.stream().map(MeteringPointDataDay::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bigDecimal = avg == null ? BigDecimal.ZERO : avg.multiply(BigDecimal.valueOf(dayData.size())).subtract(sum);

            map.put(project.getProjectType(), map.getOrDefault(project.getProjectType(), BigDecimal.ZERO).add(bigDecimal));
            data.put(point.getCategoryId(), map);
        }
        Map<Long,ProjectEnergyConservationData> result = new HashMap<>();
        List<DictModel> dictModels = sysBaseAPI.queryDictItemsByCode(Project.PROJECT_TYPE_DICT_CODE);
        data.forEach((k,v) -> {
            List<ProjectTypeEnergyConservationData> itemData = new ArrayList<>();
            // 统计总数
            // 获取数据字典
            for(DictModel dict : dictModels){
                ProjectTypeEnergyConservationData item = new ProjectTypeEnergyConservationData();
                item.setProjectType(dict.getText());
                item.setValue(v.getOrDefault(dict.getValue(), BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP));
                itemData.add(item);
            }
            ProjectEnergyConservationData res = new ProjectEnergyConservationData();
            res.setTotal(itemData.stream().map(ProjectTypeEnergyConservationData::getValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP));
            res.setList(itemData.stream().sorted(Comparator.comparing(ProjectTypeEnergyConservationData::getValue).reversed()).toList());
            result.put(k, res);
        });
        ProjectEnergyConservationResult res = new ProjectEnergyConservationResult();
        res.setWater(result.get(DeviceConstant.CATEGORY_WATER));
        res.setElectricity(result.get(DeviceConstant.CATEGORY_ELECTRICITY));
        return res;
    }

}
