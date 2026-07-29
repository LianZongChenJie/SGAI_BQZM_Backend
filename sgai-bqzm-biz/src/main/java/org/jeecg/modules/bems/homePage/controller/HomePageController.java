package org.jeecg.modules.bems.homePage.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.alarm.dto.AlarmRecordDto;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.dataBoard.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.dataBoard.service.IDataBoardService;
import org.jeecg.modules.bems.dataBoard.vo.StatisticsVo;
import org.jeecg.modules.bems.energyAnalysis.service.ICarbonEmissionService;
import org.jeecg.modules.bems.energyAnalysis.vo.CarbonEmissionDataVo;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.homePage.dto.*;
import org.jeecg.modules.bems.homePage.service.AlarmStatisticsService;
import org.jeecg.modules.bems.homePage.service.EnergyConsumptionStatisticsService;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationData;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationResult;
import org.jeecg.modules.bems.project.service.IProjectService;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bems/homePage")
@AllArgsConstructor
@Slf4j
public class HomePageController {

    private final EnergyConsumptionStatisticsService energyConsumptionStatisticsService;

    private final IDataBoardService dataBoardService;

    private final AlarmStatisticsService alarmStatisticsService;

    private final IAlarmRecordService alarmRecordService;

    private final IBusinessConfigService businessConfigService;

    private final IDeviceAttributeService deviceAttributeService;

    private final RedisUtil redisUtil;

    private final ICarbonEmissionService carbonEmissionService;

    private final IProjectService projectService;

/**
 * 水能耗统计-当日
 * @return
 */
@GetMapping("/waterStatisticsForDay")
public Result<EnergyConsumptionStatisticsDto> waterStatisticsForDay(){
    Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER);

    // 从缓存获取数据
    String cacheKey = "homePage:waterStatisticsForDay";
    EnergyConsumptionStatisticsDto cachedData = (EnergyConsumptionStatisticsDto)redisUtil.get(cacheKey);
    if (cachedData != null) {
        return Result.ok(cachedData);
    }
    // 缓存未命中，查询数据
    EnergyConsumptionStatisticsDto result = energyConsumptionStatisticsService.energyConsumptionStatisticsForDay(pointId, LocalDate.now());
    redisUtil.set(cacheKey, result, 60 * 10);
    return Result.ok(result);
}

    /**
     * 定时更新能耗统计-当日缓存
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 每10分钟执行一次
    public void refreshWaterStatisticsForDayCache() {
        try {
            Long water = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER);
            EnergyConsumptionStatisticsDto waterData = energyConsumptionStatisticsService.energyConsumptionStatisticsForDay(water, LocalDate.now());

            String waterKey = "homePage:waterStatisticsForDay";
            redisUtil.set(waterKey, waterData, 60 * 10);
            log.info("成功更新水能耗统计-当日缓存");

            Long electricity = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY);
            EnergyConsumptionStatisticsDto electricityData = energyConsumptionStatisticsService.energyConsumptionStatisticsForDay(electricity, LocalDate.now());

            String electricityKey = "homePage:electricityStatisticsForDay";
            redisUtil.set(electricityKey, electricityData, 60 * 10);
            log.info("成功更新水能耗统计-当日缓存");
        } catch (Exception e) {
            log.error("更新水能耗统计-当日缓存失败", e);
        }
    }


    /**
     * 水能耗统计-当月
     * @return
     */
    @GetMapping("/waterStatisticsForMonth")
    public Result<EnergyConsumptionStatisticsDto> waterStatisticsForMonth(){
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER);

        return Result.ok(energyConsumptionStatisticsService.energyConsumptionStatisticsForMonth(pointId, LocalDate.now()));
    }

    /**
     * 水能耗统计-当年
     * @return
     */
    @GetMapping("/waterStatisticsForYear")
    public Result<EnergyConsumptionStatisticsDto> waterStatisticsForYear(){
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER);

        return Result.ok(energyConsumptionStatisticsService.energyConsumptionStatisticsForYear(pointId, LocalDate.now()));
    }

    /**
     * 电能耗统计-当日
     * @return
     */
    @GetMapping("/electricityStatisticsForDay")
    public Result<EnergyConsumptionStatisticsDto> electricityStatisticsForDay(){
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY);

        // 从缓存获取数据
        String cacheKey = "homePage:electricityStatisticsForDay";
        EnergyConsumptionStatisticsDto cachedData = (EnergyConsumptionStatisticsDto)redisUtil.get(cacheKey);
        if (cachedData != null) {
            return Result.ok(cachedData);
        }
        // 缓存未命中，查询数据
        EnergyConsumptionStatisticsDto result = energyConsumptionStatisticsService.energyConsumptionStatisticsForDay(pointId, LocalDate.now());
        redisUtil.set(cacheKey, result, 60 * 10);
        return Result.ok(result);
    }

    /**
     * 电能耗统计-当月
     * @return
     */
    @GetMapping("/electricityStatisticsForMonth")
    public Result<EnergyConsumptionStatisticsDto> electricityStatisticsForMonth(){
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY);

        return Result.ok(energyConsumptionStatisticsService.energyConsumptionStatisticsForMonth(pointId, LocalDate.now()));
    }

    /**
     * 电能耗统计-当年
     * @return
     */
    @GetMapping("/electricityStatisticsForYear")
    public Result<EnergyConsumptionStatisticsDto> electricityStatisticsForYear(){
        Long pointId = businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY);

        return Result.ok(energyConsumptionStatisticsService.energyConsumptionStatisticsForYear(pointId, LocalDate.now()));
    }

    /**
     * 近七日能耗趋势-电
     */
    @GetMapping("/energyConsumptionPSDElectricity")
    public Result<Chat> energyConsumptionPSDElectricity(){
        return Result.ok(dataBoardService.energyConsumptionPSDElectricity());
    }

    /**
     * 近七日能耗趋势-水
     */
    @GetMapping("/energyConsumptionPSDWater")
    public Result<Chat> energyConsumptionPSNWater(){
        return Result.ok(dataBoardService.energyConsumptionPSNWater());
    }

    /**
     * 报警统计-当日
     */
    @GetMapping("/alarmStatisticsForDay")
    public Result<List<AlarmStatisticsDto>> alarmStatisticsForDay(){
        return Result.ok(alarmStatisticsService.alarmStatisticsForDay());
    }

    /**
     * 报警统计-当月
     */
    @GetMapping("/alarmStatisticsForMonth")
    public Result<List<AlarmStatisticsDto>> alarmStatisticsForMonth(){
        return Result.ok(alarmStatisticsService.alarmStatisticsForMonth());
    }

    /**
     * 报警统计-当年
     */
    @GetMapping("/alarmStatisticsForYear")
    public Result<List<AlarmStatisticsDto>> alarmStatisticsForYear(){
        return Result.ok(alarmStatisticsService.alarmStatisticsForYear());
    }

    /**
     * 温度
     * TODO 未安装温湿度传感器，临时处理取用空调机组（回风温度、送风温度）平均值
     * @return
     */
    @GetMapping("/environment")
    public Result<EnvironmentDto> environment(){
        // 获取device_attribute中对应编码的数据，求平均值
        List<String> listByKey = businessConfigService.getListByKey("homePage:temperature", String.class);
        List<DeviceAttribute> byAttributeCodes = deviceAttributeService.findByAttributeCodes(listByKey);
        BigDecimal temperature = BigDecimal.ZERO;
        int i = 0;
        for (DeviceAttribute attribute : byAttributeCodes) {
            if(StringUtils.isEmpty(attribute.getValue())){
                continue;
            }
            try {
                temperature = temperature.add(new BigDecimal(attribute.getValue()));
                i++;
            }catch (Exception e){
                log.error("温度数据转换异常：{}",attribute.getValue());
            }
        }
        if(i != 0){
            temperature = temperature.divide(new BigDecimal(i),0, RoundingMode.HALF_UP);
        }
        EnvironmentDto result = new EnvironmentDto();
        result.setTemperature(temperature.toPlainString());
        return Result.ok(result);
    }

    /**
     * 能源使用安全
     * 统计设备负荷
     * @return 当前负荷、额定负荷、负荷率
     */
    @GetMapping("/energyUseSafety")
    public Result<List<EnergyUseSafetyDataDto>> energyUseSafety(@RequestParam(required = false) String configKey){
        if(StrUtil.isEmpty(configKey)){
            configKey = "by:energyUseSafety";
        }
        // 获取业务配置
        List<EnergyUseSafetyConfig> configs = businessConfigService.getListByKey(configKey, EnergyUseSafetyConfig.class);
        // 获取关联设备id
        Set<Long> deviceIds = configs.stream()
                .map(EnergyUseSafetyConfig::getDeviceIds)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        // 获取设备属性
        Map<Long,List<DeviceAttribute>> deviceAttributeMap = deviceAttributeService.findByDeviceIds(deviceIds)
                .stream()
                .collect(Collectors.groupingBy(DeviceAttribute::getDeviceId, Collectors.toList()));
        // 解析配置,组合结果
        List<EnergyUseSafetyDataDto> result = new ArrayList<>();
        for(EnergyUseSafetyConfig config : configs){
            // 获取设备id
            List<Long> ids = config.getDeviceIds();
            // 当前负荷、额定负荷、负荷率
            List<DeviceAttribute> attributes = new ArrayList<>();
            for(Long id : ids){
                attributes.addAll(deviceAttributeMap.getOrDefault(id, new ArrayList<>()));
            }
            BigDecimal ratedLoad = attributes.stream()
                    .filter(item -> StringUtils.isNotEmpty(item.getValue()) && config.getRatedLoadAttributeCode().equals(item.getAttributeCode()))
                    .map(item -> new BigDecimal(item.getValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).multiply(BigDecimal.valueOf(1000));
            BigDecimal currentLoad = attributes.stream()
                    .filter(item -> StringUtils.isNotEmpty(item.getValue()) && config.getCurrentLoadAttributeCode().equals(item.getAttributeCode()))
                    .map(item -> new BigDecimal(item.getValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 计算负荷率，返回百分比结果，保留两位小数，当额定负荷为0时，返回--%
            DecimalFormat df = new DecimalFormat("0.00%");
            String loadRate = currentLoad.compareTo(BigDecimal.ZERO) == 0 ? "--%" : df.format(ratedLoad.divide(currentLoad, 4, RoundingMode.HALF_UP));
            EnergyUseSafetyDataDto data = new EnergyUseSafetyDataDto();
            data.setName(config.getName());
            data.setRatedLoad(ratedLoad.setScale(2, RoundingMode.HALF_UP).toPlainString());
            data.setCurrentLoad(currentLoad.setScale(2, RoundingMode.HALF_UP).toPlainString());
            data.setLoadRate(loadRate);
            result.add(data);
        }
        return Result.ok(result);
    }

    /**
     * 碳足迹
     * @return 今日、本周、本月、本季度、本年
     */
    @GetMapping("/carbonFootprint")
    public Result<CarbonFootprintDataDto> carbonFootprint(){
        LocalDateTime now = LocalDateTime.now();
        CarbonEmissionDataVo day = carbonEmissionService.getCarbonEmissionForDay(now);
        CarbonEmissionDataVo week = carbonEmissionService.getCarbonEmissionForWeek(now.toLocalDate());
        CarbonEmissionDataVo month = carbonEmissionService.getCarbonEmissionForMonth(now.toLocalDate());
        CarbonEmissionDataVo quarter = carbonEmissionService.getCarbonEmissionForQuarter(now.toLocalDate());
        CarbonEmissionDataVo year = carbonEmissionService.getCarbonEmissionForYear(now.toLocalDate());
        CarbonFootprintDataDto result = new CarbonFootprintDataDto();
        result.setTodayCarbonEmission(day.getValue().toPlainString());
        result.setTodayCarbonEmissionCompare(day.getIncrease());
        result.setWeekCarbonEmission(week.getValue().toPlainString());
        result.setWeekCarbonEmissionCompare(week.getIncrease());
        result.setMonthCarbonEmission(month.getValue().toPlainString());
        result.setMonthCarbonEmissionCompare(month.getIncrease());
        result.setQuarterCarbonEmission(quarter.getValue().toPlainString());
        result.setQuarterCarbonEmissionCompare(quarter.getIncrease());
        result.setYearCarbonEmission(year.getValue().toPlainString());
        result.setYearCarbonEmissionCompare(year.getIncrease());
        return Result.ok(result);
    }

    /**
     * 能耗趋势-电-当日
     */
    @GetMapping("/energyConsumptionElectricityForDay")
    public Result<Chat> energyConsumptionElectricityForDay(){
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY),
                "电",
                "day",
                LocalDateTime.now()));
    }

    /**
     * 能耗趋势-电-当月
     */
    @GetMapping("/energyConsumptionElectricityForMonth")
    public Result<Chat> energyConsumptionElectricityForMonth(){
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY),
                "电",
                "month",
                LocalDateTime.now()));
    }

    /**
     * 能耗趋势-电-当年
     */
    @GetMapping("/energyConsumptionElectricityForYear")
    public Result<Chat> energyConsumptionElectricityForYear(){
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_ELECTRICITY),
                "电",
                "year",
                LocalDateTime.now()));
    }

    /**
     * 能耗趋势-水-当日
     */
    @GetMapping("/energyConsumptionWaterForDay")
    public Result<Chat> energyConsumptionWaterForDay() {
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER),
                "水",
                "day",
                LocalDateTime.now()));
    }

    /**
     * 能耗趋势-水-当月
     */
    @GetMapping("/energyConsumptionWaterForMonth")
    public Result<Chat> energyConsumptionWaterForMonth() {
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER),
                "水",
                "month",
                LocalDateTime.now()));
    }

    /**
     * 能耗趋势-水-当年
     */
    @GetMapping("/energyConsumptionWaterForYear")
    public Result<Chat> energyConsumptionWaterForYear() {
        return Result.ok(dataBoardService.energyConsumption(
                businessConfigService.getLongByKey(BusinessConfigConstant.ENERGY_CONSUMPTION_PSD_WATER),
                "水",
                "year",
                LocalDateTime.now()));
    }

    /**
     * 告警统计-当月
     * @return 报警次数、同比、环比
     */
    @GetMapping("/alarmStatistics")
    public Result<StatisticsVo> alarmStatistics(){
        return Result.ok(alarmStatisticsService.alarmStatistics());
    }

    /**
     * 告警记录查询当月
     * @param param pageNo,pageSize
     */
    @GetMapping("/alarmRecordListForMonth")
    public Result<IPage<AlarmRecord>> alarmRecordListForMonth(AlarmRecordDto param){
        LocalDateTime now = LocalDateTime.now();
        param.setStartDateTime(now.toLocalDate().withDayOfMonth(1).atStartOfDay());
        param.setEndDateTime(now);
        return Result.ok(alarmRecordService.listPage(param));
    }

    /**
     * 节能统计
     * 分节能类型统计节能量
     */
    @GetMapping("/energyConservationStatistics")
    public Result<ProjectEnergyConservationResult> energyConservationStatistics(){
        return Result.ok(projectService.energyConservationStatistics());
    }

}
