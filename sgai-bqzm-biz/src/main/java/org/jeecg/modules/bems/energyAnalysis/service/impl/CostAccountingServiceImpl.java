package org.jeecg.modules.bems.energyAnalysis.service.impl;

import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.util.pricing.LadderPricing;
import org.jeecg.modules.bems.energyAnalysis.vo.CostAccountingVo;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CostAccountingServiceImpl implements ICostAccountingService {

    private final ICostCenterRelService costCenterRelService;
    private final IEnergyPricingConfigService energyPricingConfigService;
    private final IDeviceService deviceService;
    private final IMeteringPointService meteringPointService;

    private final ICostCenterDataHourService costCenterDataHourService;
    private final ICostCenterDataDayService costCenterDataDayService;
    private final ICostCenterDataMonthService costCenterDataMonthService;
    private final ICostCenterDataYearService costCenterDataYearService;

    private final TransactionTemplate transactionTemplate;


    /**
     * 日成本计算
     * @param costCenterId 成本中心id
     * @param day 日期
     */
    @Override
    public List<CostAccountingVo> findCostByDay(Long costCenterId, LocalDate day) {
        // 获取成本中心下科目
        List<CostCenterRel> list = costCenterRelService.listByCostCenterId(costCenterId);
        Map<String, List<CostCenterRel>> relTypeMap = list.stream().collect(Collectors.groupingBy(CostCenterRel::getRelType));
        List<CostCenterDataDay> dataDayList = new ArrayList<>();
        relTypeMap.forEach((relType, relList) -> {
            dataDayList.addAll(costCenterDataDayService.listByRelTypeAndRelIdsAndTime(relType, relList.stream().map(CostCenterRel::getRelId).collect(Collectors.toList()), day.atStartOfDay()));
        });

        return convert(list,dataDayList);
    }

    /**
     * 月成本计算
     *
     * @param costCenterId 成本中心id
     * @param month        月份
     */
    @Override
    public List<CostAccountingVo> findCostByMonth(Long costCenterId, LocalDate month) {
        List<CostCenterRel> list = costCenterRelService.listByCostCenterId(costCenterId);
        Map<String, List<CostCenterRel>> relTypeMap = list.stream().collect(Collectors.groupingBy(CostCenterRel::getRelType));
        List<CostCenterDataMonth> dataDayList = new ArrayList<>();
        relTypeMap.forEach((relType, relList) -> {
            dataDayList.addAll(costCenterDataMonthService.listByRelTypeAndRelIdsAndTime(relType, relList.stream().map(CostCenterRel::getRelId).collect(Collectors.toList()), month.withDayOfMonth(1).atStartOfDay()));
        });
        return convert(list,dataDayList);
    }

    /**
     * 年成本计算
     *
     * @param costCenterId 成本中心id
     * @param year         年份
     */
    @Override
    public List<CostAccountingVo> findCostByYear(Long costCenterId, LocalDate year) {
        List<CostCenterRel> list = costCenterRelService.listByCostCenterId(costCenterId);
        Map<String, List<CostCenterRel>> relTypeMap = list.stream().collect(Collectors.groupingBy(CostCenterRel::getRelType));
        List<CostCenterDataYear> dataDayList = new ArrayList<>();
        relTypeMap.forEach((relType, relList) -> {
            dataDayList.addAll(costCenterDataYearService.listByRelTypeAndRelIdsAndTime(relType, relList.stream().map(CostCenterRel::getRelId).collect(Collectors.toList()), year.withDayOfYear(1).atStartOfDay()));
        });
        return convert(list,dataDayList);
    }

    private List<CostAccountingVo> convert(List<CostCenterRel> relList,List<? extends CostCenterData> dataList){
        // 填充数据
        Map<String,CostCenterData> dataDayMap = dataList.stream()
                .collect(Collectors.toMap(item -> item.getType() + "_" + item.getRelId(), Function.identity()));
        List<CostAccountingVo> res = new ArrayList<>();
        for (CostCenterRel item : relList) {
            CostCenterData data = dataDayMap.get(item.getRelType() + "_" + item.getRelId());
            CostAccountingVo vo = new CostAccountingVo();
            vo.setId(item.getId());
            vo.setRelType(item.getRelType());
            vo.setRelId(item.getRelId());
            vo.setCostAccountName(item.getPointName());
            vo.setAccountingQuantity(data == null ? BigDecimal.ZERO : data.getValue());
            vo.setAccountingCost(data == null ? BigDecimal.ZERO : data.getCost());
            res.add(vo);
        }
        return res;
    }

    /**
     * 计算用能成本
     * @param relId 关联id
     * @param hour 小时
     * @param value 小时能耗
     */
    public void calculationCost(String type,Long relId, LocalDateTime hour,BigDecimal value){
        hour = hour.withMinute(0).withSecond(0);
        if(!costCenterRelService.checkRelId(type,relId)){
            // 不存在关联关系
            return;
        }
        Long categoryId = getCategoryId(type, relId);
        if(categoryId == null){
            return;
        }
        // 获取计价规则
        EnergyPricingConfig config = energyPricingConfigService.getByCategoryId(categoryId);
        if(config == null){
            return;
        }
        BigDecimal cost = BigDecimal.ZERO;
        switch (config.getBillingWay()){
            case "1":
                // 峰谷分时计价
                Map<String,BigDecimal> pvts = config.formatPVTS();
                cost = pvts.getOrDefault(hour.format(EnergyPricingConfig.filedForMatter), BigDecimal.ZERO).multiply(value);
                break;
            case "2":
                // 固定计价
                cost = config.getFixedUnitPrice().multiply(value);
                break;
            case "3":
                // 阶梯计价
                List<LadderPricing> ladderPricings = config.formatLadderPricing();
                // 获取截止到上个小时能耗总量
                // TODO 获取截止到上个小时当前类别能耗总量，设置为total
                // CostCalculationUtil.calculationLadderPricing(ladderPricings, total, value)
                cost = BigDecimal.valueOf(-1L);
                break;
            default:
                throw new JeecgBootException("未定义的能源价格计算方式！");
        }
        // 更新成本信息
        saveCost(type, relId, hour, value, cost);
    }

    /**
     * 获取类别信息
     * @param type 类别
     * @param relId 关联id
     * @return 类别id
     */
    private Long getCategoryId(String type,Long relId){
        switch(type){
            case "1":
                MeteringPoint point = meteringPointService.getById(relId);
                return point == null ? null : point.getCategoryId();
            case "2":
                Device device = deviceService.getById(relId);
                return device == null ? null : device.getCategoryId();
            default:
                throw new RuntimeException("关联类别无效");
        }
    }

    /**
     * 保存成本数据
     */
    private void saveCost(String type, Long relId, LocalDateTime time, BigDecimal value, BigDecimal cost){
        // 更新小时成本
        // 同步更新日成本、月成本、年成本
        transactionTemplate.execute(status -> {
            costCenterDataHourService.save(type, relId, time, value, cost);
            costCenterDataDayService.save(type, relId, time.withHour(0), value, cost);
            costCenterDataMonthService.save(type, relId, time.withDayOfMonth(1).withHour(0), value, cost);
            costCenterDataYearService.save(type, relId, time.withDayOfYear(1).withHour(0), value, cost);
            return status;
        });
    }
}
