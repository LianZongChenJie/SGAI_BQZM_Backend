package org.jeecg.modules.bems.energyAnalysis.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import dm.jdbc.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointChatDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointData;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.util.Jexl3Util;
import org.jeecg.modules.bems.energyAnalysis.util.TableUtil;
import org.jeecg.modules.bems.energyAnalysis.vo.*;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChat;
import org.jeecg.modules.bems.energyAnalysis.vo.chat.PieChatSeriesData;
import org.jeecg.modules.bems.entity.*;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.jeecg.modules.bems.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@AllArgsConstructor
@Slf4j
public class MeteringPointDataServiceImpl implements IMeteringPointDataService {
    private final IMeteringPointService meteringPointService;
    private final IMeteringPointRelService meteringPointRelService;
    private final IDeviceService deviceService;
    private final IMeteringPointDataHourService hourDataService;
    private final IMeteringPointDataDayService dayDataService;
    private final IMeteringPointDataMonthService monthDataService;
    private final IMeteringPointDataYearService yearDataService;
    private final IHourDataService deviceHourDataService;
    private final IDayDataService deviceDayDataService;
    private final IMonthDataService deviceMonthDataService;
    private final IYearDataService deviceYearDataService;
    private final DateTimeFormatter filedForMatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    private final IMinuteDataService deviceMinuteDataService;

    private final IMeteringPointDataMinuteService minuteDataService;

    private final TransactionTemplate transactionTemplate;

    private final MqSendService mqSendService;


    @Override
    public Table findMinute(String energyFlowDiagramIds, LocalDateTime hour) {
        hour = hour.withMinute(0).withSecond(0).withNano(0);
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.minuteHeaders(hour);
        List<? extends MeteringPointData> meterDataList = minuteDataService.findByTimeRangeAndPointIds(
                hour,
                hour.plusHours(1),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public Table findDay(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.dayHeaders(localDate);
        List<? extends MeteringPointData> meterDataList = hourDataService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate, LocalTime.MIN),
                LocalDateTime.of(localDate, LocalTime.MIN.withHour(23)),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public Table findMonth(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.monthHeaders(localDate.getYear(),localDate.getMonthValue());
        List<? extends MeteringPointData> meterDataList = dayDataService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate.withDayOfMonth(1), LocalTime.MIN),
                LocalDateTime.of(localDate.withDayOfMonth(1).plusMonths(1), LocalTime.MIN),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public Table findYear(String energyFlowDiagramIds, LocalDate localDate) {
        List<MeteringPoint> configs = meteringPointService.getByIds(strToLongList(energyFlowDiagramIds));
        List<Long> configIds = configs.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<TableHeader> tableHeaderList = TableUtil.yearHeaders(localDate.getYear());
        List<? extends MeteringPointData> meterDataList = monthDataService.findByTimeRangeAndPointIds(
                LocalDateTime.of(localDate.withMonth(1).withDayOfMonth(1), LocalTime.MIN),
                LocalDateTime.of(localDate.withMonth(1).withDayOfMonth(1).plusYears(1), LocalTime.MIN),
                configIds
        );
        return createTable(tableHeaderList, configs, meterDataList);
    }

    @Override
    public void calculateValue(LocalDateTime hour){
        // TODO 根据小时进行加锁，防止重复计算,根据点位类别，进行多线程操作
        if(hour == null){
            return;
        }
        hour = hour.withMinute(0).withSecond(0).withNano(0);
        // 获取所有点位
        List<MeteringPoint> rules = reverseTree(meteringPointService.list());
        // 获取所有设备信息
        Map<Long,String> deviceCodeMap = deviceService.list().stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));
        // 获取小时所有数据
        Map<String,BigDecimal> hourData = deviceHourDataService.findByTime(hour).stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),HourData::getValue));
        // 获取日所有数据
        Map<String,BigDecimal> dayData = deviceDayDataService.findByTime(hour.withHour(0)).stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),DayData::getValue));
        // 获取月所有数据
        Map<String,BigDecimal> monthData = deviceMonthDataService.findByTime(hour.withDayOfMonth(1).withHour(0)).stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),MonthData::getValue,(k1,k2) -> k2));
        // 获取年所有数据
        Map<String,BigDecimal> yearData = deviceYearDataService.findByTime(hour.withDayOfMonth(1).withMonth(1).withHour(0)).stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),YearData::getValue,(k1,k2) -> k2));
        // 遍历所有点位
        for(MeteringPoint rule : rules){
            String formula = rule.getFormula();
            if(StringUtil.isEmpty(formula)) {
                continue;
            }

            // 更新小时值
            hourData.put(rule.getNodeCode(),Jexl3Util.getValue(formula,hourData));
            hourDataService.save(rule.getId(),hour, hourData.get(rule.getNodeCode()));
            // 更新日
            dayData.put(rule.getNodeCode(),Jexl3Util.getValue(formula, dayData));
            dayDataService.save(rule.getId(),hour.withHour(0),dayData.get(rule.getNodeCode()));
            // 更新月
            monthData.put(rule.getNodeCode(),Jexl3Util.getValue(formula,monthData));
            monthDataService.save(rule.getId(),hour.withDayOfMonth(1).withHour(0),monthData.get(rule.getNodeCode()));
            // 更新年
            yearData.put(rule.getNodeCode(),Jexl3Util.getValue(formula,yearData));
            yearDataService.save(rule.getId(),hour.withDayOfMonth(1).withMonth(1).withHour(0),yearData.get(rule.getNodeCode()));
        }
    }

    @Transactional
    @Override
    public void calculateValue(List<LocalDateTime> hours){
        hours = hours.stream().sorted().collect(Collectors.toList());
        LocalDate day = null;
        LocalDate month = null;
        int year = 0;
        List<MeteringPoint> rules = reverseTree(meteringPointService.list());

        Map<Long,String> deviceCodeMap = deviceService.list().stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));
        for(LocalDateTime hour : hours){
            LocalDate localDate = hour.toLocalDate();
            Map<String,BigDecimal> hourData = deviceHourDataService.findByTime(hour).stream()
                    .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                    .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),HourData::getValue));
            Map<String,BigDecimal> dayData = day == null || !day.equals(localDate) ? deviceDayDataService.findByTime(hour.withHour(0)).stream()
                    .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                    .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),DayData::getValue)) : null;
            Map<String,BigDecimal> monthData = month == null || !month.equals(localDate.withDayOfMonth(1)) ? deviceMonthDataService.findByTime(hour.withDayOfMonth(1).withHour(0)).stream()
                    .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                    .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),MonthData::getValue)) : null;
            Map<String,BigDecimal> yearData = year == 0 || year != localDate.getYear() ? deviceYearDataService.findByTime(hour.withDayOfMonth(1).withMonth(1).withHour(0)).stream()
                    .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                    .collect(Collectors.toMap(item -> deviceCodeMap.get(item.getDeviceId()),YearData::getValue)) : null;
            for(MeteringPoint item : rules){
                if(StringUtil.isEmpty(item.getFormula())){
                    continue;
                }
                BigDecimal hourValue = Jexl3Util.getValue(item.getFormula(), hourData);
                hourDataService.save(item.getId(),hour,hourValue);
                hourData.put(item.getNodeCode(),hourValue);
                if(dayData != null){
                    dayData.put(item.getNodeCode(),Jexl3Util.getValue(item.getFormula(), dayData));
                    dayDataService.save(item.getId(),hour.withHour(0),dayData.get(item.getNodeCode()));
                }
                if(monthData != null){
                    monthData.put(item.getNodeCode(),Jexl3Util.getValue(item.getFormula(), monthData));
                    monthDataService.save(item.getId(),hour.withDayOfMonth(1).withHour(0),monthData.get(item.getNodeCode()));
                }
                if(yearData != null){
                    yearData.put(item.getNodeCode(),Jexl3Util.getValue(item.getFormula(), yearData));
                    yearDataService.save(item.getId(),hour.withDayOfMonth(1).withMonth(1).withHour(0),yearData.get(item.getNodeCode()));
                }
            }
            day = localDate;
            month = localDate.withDayOfMonth(1);
            year = localDate.getYear();
        }
    }

    /**
     * 计量规则点位计算
     *
     * @param deviceId 设备id
     * @param hour     小时
     */
    @Override
    public void calculateValue(Long deviceId, LocalDateTime hour) {
        hour = hour.withMinute(0).withSecond(0);
        // 查询设备关联点位信息
        List<Long> pointIds = meteringPointRelService.findPointIdsByDeviceId(deviceId);
        // 获取点位信息
        List<MeteringPoint> points = meteringPointService.getByIds(pointIds);
        // 查询点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointIds(pointIds);
        //查询设备信息
        Map<Long,String> deviceCodeMap = deviceService.findByDeviceIds(deviceIds)
                .stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));

        // 获取设备能耗数据
        // 小时能耗
        Map<String,BigDecimal> hourData = deviceHourDataService.findByDeviceIdsAndTime(deviceIds, hour)
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null && item.getValue() != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""),HourData::getValue));
        // 日能耗
//        Map<String,BigDecimal> dayData =deviceDayDataService.findByDeviceIdsAndTime(deviceIds, hour.withHour(0))
//                .stream()
//                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
//                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""),DayData::getValue));
//
//        // 月能耗
//        Map<String,BigDecimal> monthData = deviceMonthDataService.findByDeviceIdsAndTime(deviceIds, hour.withDayOfMonth(1).withHour(0))
//                .stream()
//                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
//                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""), MonthData::getValue));
//        // 年能耗
//        Map<String,BigDecimal> yearData = deviceYearDataService.findByDeviceIdsAndTime(deviceIds, hour.withDayOfYear(1).withHour(0))
//                .stream()
//                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
//                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""), YearData::getValue));
        LocalDateTime finalHour = hour;
        for(MeteringPoint item : points){
            BigDecimal value = null;
            try {
                if (StringUtils.isEmpty(item.getTrueFormula())) {
                    continue;
                }
                value = Jexl3Util.getValue(item.getTrueFormula(), hourData);
                // 260323 计量规则数据不保存
//                value = transactionTemplate.execute(res -> {
//                    BigDecimal hourValue = Jexl3Util.getValue(item.getTrueFormula(), hourData);
//                    hourDataService.save(item.getId(), finalHour, hourValue);
//                    BigDecimal dayValue = Jexl3Util.getValue(item.getTrueFormula(), dayData);
//                    dayDataService.save(item.getId(), finalHour.withHour(0), dayValue);
//                    BigDecimal monthValue = Jexl3Util.getValue(item.getTrueFormula(), monthData);
//                    monthDataService.save(item.getId(), finalHour.withDayOfMonth(1).withHour(0), monthValue);
//                    BigDecimal yearValue = Jexl3Util.getValue(item.getTrueFormula(), yearData);
//                    yearDataService.save(item.getId(), finalHour.withDayOfYear(1).withHour(0), yearValue);
//                    return hourValue;
//                });
                mqSendService.sendMeteringPointDataChange(item.getId(),hour,value);
            }catch (Exception e){
                log.error("计量规则点位计算错误：点位id: {},时间：{}",item.getId(),finalHour, e);
            }
        }
    }

    /**
     * 计量规则点位计算
     *
     * @param pointId 点位id
     * @param hour    小时
     */
    @Override
    public void calculatePointValue(Long pointId, LocalDateTime hour) {
        // 获取点位信息
        MeteringPoint point = meteringPointService.getById(pointId);
        if(point == null || StringUtils.isEmpty(point.getTrueFormula())){
            return;
        }
        // 获取点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointId(pointId);
        //查询设备信息
        Map<Long,String> deviceCodeMap = deviceService.findByDeviceIds(deviceIds)
                .stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));
        // 获取设备能耗数据
        // 小时能耗
        Map<String,BigDecimal> hourData = deviceHourDataService.findByDeviceIdsAndTime(deviceIds, hour)
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null && item.getValue() != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""),HourData::getValue));
        // 日能耗
        Map<String,BigDecimal> dayData =deviceDayDataService.findByDeviceIdsAndTime(deviceIds, hour.withHour(0))
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""),DayData::getValue));

        // 月能耗
        Map<String,BigDecimal> monthData = deviceMonthDataService.findByDeviceIdsAndTime(deviceIds, hour.withDayOfMonth(1).withHour(0))
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""), MonthData::getValue));
        // 年能耗
        Map<String,BigDecimal> yearData = deviceYearDataService.findByDeviceIdsAndTime(deviceIds, hour.withDayOfYear(1).withHour(0))
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""), YearData::getValue));
        try {
            BigDecimal value = transactionTemplate.execute(res -> {
                BigDecimal hourValue = Jexl3Util.getValue(point.getTrueFormula(), hourData);
                hourDataService.save(point.getId(), hour, hourValue);
                BigDecimal dayValue = Jexl3Util.getValue(point.getTrueFormula(), dayData);
                dayDataService.save(point.getId(), hour.withHour(0), dayValue);
                BigDecimal monthValue = Jexl3Util.getValue(point.getTrueFormula(), monthData);
                monthDataService.save(point.getId(), hour.withDayOfMonth(1).withHour(0), monthValue);
                BigDecimal yearValue = Jexl3Util.getValue(point.getTrueFormula(), yearData);
                yearDataService.save(point.getId(), hour.withDayOfYear(1).withHour(0), yearValue);
                return hourValue;
            });
            mqSendService.sendMeteringPointDataChange(point.getId(),hour,value);
        }catch (Exception e){
            log.error("计量规则点位计算错误：点位id: {},时间：{}",point.getId(),hour, e);
        }

    }

    /**
     * 计量规则点位计算
     *
     * @param pointId 点位id
     * @param minute  分钟
     */
    @Override
    public void calculatePointValueMinute(Long pointId, LocalDateTime minute) {
        // 获取点位信息
        MeteringPoint point = meteringPointService.getById(pointId);
        if(point == null || StringUtils.isEmpty(point.getTrueFormula())){
            return;
        }
        // 获取点位关联设备信息
        List<Long> deviceIds = meteringPointRelService.findDeviceIdByPointId(pointId);
        //查询设备信息
        Map<Long,String> deviceCodeMap = deviceService.findByDeviceIds(deviceIds)
                .stream().collect(Collectors.toMap(Device::getId, Device::getDeviceCode));

        Map<String,BigDecimal> minuteData = deviceMinuteDataService.findByDeviceIdsAndTime(deviceIds, minute)
                .stream()
                .filter(item -> deviceCodeMap.get(item.getDeviceId()) != null && item.getValue() != null)
                .collect(Collectors.toMap(item -> deviceCodeMap.getOrDefault(item.getDeviceId(),""), MinuteData::getValue));
        try {
            transactionTemplate.execute(res -> {
                BigDecimal minuteValue = Jexl3Util.getValue(point.getTrueFormula(), minuteData);
                minuteDataService.save(point.getId(), minute, minuteValue);
                return null;
            });
        }catch (Exception e){
            log.error("计量规则点位计算错误：点位id: {},时间：{}",point.getId(),minute, e);
        }
    }

    /**
     * 查询饼图数据
     *
     */
    @Override
    public PieChat findPieChat(MeteringPointChatDto param) {
        initChatParam(param);
        // 获取计量点位信息
        MeteringPoint point = meteringPointService.getById(param.getPointId());
        // 获取该计量点下所有子集
        // 这块要按照层级来遍历存储
        List<MeteringPoint> points = meteringPointService.listByType(point.getType());
        Map<Long,List<MeteringPoint>> pointMap = points.stream().sorted(Comparator.comparing(MeteringPoint::getSort)).collect(Collectors.groupingBy(MeteringPoint::getParentId, Collectors.toList()));
        List<Long> pointParentId = new ArrayList<Long>(){{add(point.getId());}};
        List<Long> allIds = new ArrayList<Long>(){{add(point.getId());}};
        while(CollectionUtil.isNotEmpty(pointParentId)){
            List<MeteringPoint> list = new ArrayList<>();
            for(Long id : pointParentId){
                List<MeteringPoint> pointList = pointMap.get(id);
                if(CollectionUtil.isNotEmpty(pointList)){
                    list.addAll(pointList);
                }
            }
            if(CollectionUtil.isNotEmpty(list)) {
                List<Long> ids = list.stream().map(MeteringPoint::getId).collect(Collectors.toList());
                allIds.addAll(ids);
                pointParentId = ids;
            }else{
                pointParentId = new ArrayList<>();
            }
        }
        // 获取点位数据
        List<? extends MeteringPointData> meteringPointData = findMeteringPointData(allIds, param.getDateType(), param.getStartDate(), param.getEndDate());
        // 根据点位id进行分组求和
        Map<Long,BigDecimal> dataMap = meteringPointData.stream().collect(Collectors.groupingBy(MeteringPointData::getMeteringPointId,Collectors.mapping(MeteringPointData::getValue,Collectors.reducing(BigDecimal.ZERO,BigDecimal::add))));
        // 生成图
        List<PieChatSeriesData> seriesData = new ArrayList<>();
        List<MeteringPoint> meteringPoints = pointMap.getOrDefault(point.getId(),Collections.emptyList());
        for(MeteringPoint i : meteringPoints){
            seriesData.add(new PieChatSeriesData(i.getNodeName(),"",dataMap.getOrDefault(i.getId(),BigDecimal.ZERO),i.getMeteringUnit() + ""));
            List<MeteringPoint> child = pointMap.get(i.getId());
            if(CollectionUtil.isEmpty(child)){
                seriesData.add(new PieChatSeriesData(i.getNodeName(),i.getNodeName(),dataMap.getOrDefault(i.getId(),BigDecimal.ZERO),i.getMeteringUnit() + ""));
                continue;
            }
            for(MeteringPoint v : child){
                seriesData.add(new PieChatSeriesData(v.getNodeName(),i.getNodeName(),dataMap.getOrDefault(v.getId(),BigDecimal.ZERO),v.getMeteringUnit() + ""));
            }
        }
        return new PieChat(point.getNodeName(),seriesData);
    }

    /**
     * 查询折线图数据
     *
     * @param param
     */
    @Override
    public Chat findLineChat(MeteringPointChatDto param) {
        return findChat(param);
    }

    /**
     * 查询柱状图数据
     *
     * @param param
     */
    @Override
    public Chat findBarChat(MeteringPointChatDto param) {
        return findChat(param);
    }

    /**
     * 查询堆叠柱状图数据
     *
     * @param param
     */
    @Override
    public Chat findStackedColumnChart(MeteringPointChatDto param) {
        // 获取点位子集，
        List<MeteringPoint> list = meteringPointService.listByParentId(param.getPointId());
        List<Long> pointIds = list.stream().map(MeteringPoint::getId).collect(Collectors.toList());
        List<? extends MeteringPointData> dataList = findMeteringPointData(pointIds,param.getDateType(),param.getStartDate(),param.getEndDate());
        List<String> xAxis = getXAxis(param.getDateType(),param.getStartDate(),param.getEndDate());
        DateTimeFormatter formatter;
        switch (param.getDateType()){
            case "month":
                formatter = monthFormatter;
                break;
            case "year":
                formatter = yearFormatter;
                break;
            default:
                formatter = dayFormatter;
        }
        Chat chat = new Chat();
        chat.setXAxis(xAxis);
        List<ChatSeries> series = new ArrayList<ChatSeries>();
        Map<Long,Map<String,BigDecimal>> dataMap = dataList.stream().collect(Collectors.groupingBy(MeteringPointData::getMeteringPointId,Collectors.toMap(item -> item.getTime().format(formatter),MeteringPointData::getValue)));
        for(MeteringPoint item : list){
            Map<String,BigDecimal> map = dataMap.getOrDefault(item.getId(),new HashMap<>());
            List<Object> data = new ArrayList<>();
            for(String x : xAxis){
                data.add(map.getOrDefault(x,BigDecimal.ZERO));
            }
            series.add(new ChatSeries(item.getNodeName(),data));
        }
        chat.setChatSeriesList(series);
        return chat;
    }

    private Chat findChat(MeteringPointChatDto param){
        // 这块查询单个点的数据，两个时间段的数据
        MeteringPoint point = meteringPointService.getById(param.getPointId());
        if(point == null){
            return new Chat();
        }
        List<Long> pointIds = new ArrayList<Long>(){{add(point.getId());}};
        Map<String,BigDecimal> pointData = meteringPointDataToMapByDateType(findMeteringPointData(pointIds, param.getDateType(), param.getStartDate(), param.getEndDate()),param.getDateType());

        Map<String,BigDecimal> basePointData = meteringPointDataToMapByDateType(findMeteringPointData(pointIds,param.getDateType(),param.getBaseStartDate(),param.getBaseEndDate()),param.getDateType());
        // 获取横坐标信息，年：yyyy；月：yyyy-MM；日：yyyy-MM-dd
        List<String> xAxis = getXAxis(param.getDateType(),param.getStartDate(),param.getEndDate());
        List<String> baseXAxis = getXAxis(param.getDateType(), param.getBaseStartDate(), param.getBaseEndDate());
        // 实际
        List<Object> actual = new ArrayList<>();
        // 基准
        List<Object> base = new ArrayList<>();
        List<String> errorMessage = new ArrayList<>();
        // 较基准异常条件配置
        BigDecimal standard = StringUtils.isNotEmpty(param.getIncrease()) ? new BigDecimal(param.getIncrease()) : null;
        MessageFormat content = StringUtils.isNotEmpty(param.getIncreaseContent()) ? new MessageFormat(param.getIncreaseContent()) : null;
        for(int i = 0; i < xAxis.size(); i++){
            String x = xAxis.get(i);
            String basex = i >= baseXAxis.size() ? null : baseXAxis.get(i);
            BigDecimal actualValue = pointData.get(x);
            BigDecimal baseValue = basex == null ? BigDecimal.ZERO : basePointData.get(basex);
            actual.add(actualValue == null ? BigDecimal.ZERO : actualValue);
            base.add(baseValue == null ? BigDecimal.ZERO : baseValue);
            // 判断是否超过基准
            if(actualValue != null && baseValue != null && baseValue.compareTo(BigDecimal.ZERO) != 0 && standard != null && content != null){
                BigDecimal divide = actualValue.subtract(baseValue).divide(baseValue, 2, RoundingMode.HALF_UP);
                if(divide.compareTo(standard) > 0){
                    errorMessage.add(content.format(new Object[]{x}));
                }
            }
        }
        return new Chat("", xAxis, new ArrayList<ChatSeries>(){{add(new ChatSeries("实际",actual));add(new ChatSeries("基准",base));}},errorMessage);
    }

    private List<? extends MeteringPointData> findMeteringPointData(List<Long> pointIds,String dateType, LocalDate startDate, LocalDate endDate) {
        dateType = StringUtil.isEmpty(dateType) ? "day" : dateType;
        switch (dateType) {
            case "day":
                return dayDataService.findByTimeRangeAndPointIds(startDate.atStartOfDay(), endDate.atStartOfDay(), pointIds);
            case "month":
                return monthDataService.findByTimeRangeAndPointIds(startDate.atStartOfDay(), endDate.atStartOfDay(), pointIds);
            case "year":
                return yearDataService.findByTimeRangeAndPointIds(startDate.atStartOfDay(), endDate.atStartOfDay(), pointIds);
        }
        return Collections.emptyList();
    }

    private Table createTable(List<TableHeader> tableHeaderList, List<MeteringPoint> configs, List<? extends MeteringPointData> meterDataList) {
        Map<Long, Map<LocalDateTime, BigDecimal>> dataMap = meterDataList.stream()
                .collect(Collectors.groupingBy(MeteringPointData::getMeteringPointId,
                        Collectors.toMap(MeteringPointData::getTime, MeteringPointData::getValue)));
        List<TableData> tableDataList = new ArrayList<>();
        // 表格尾行合计
        TableData sum = new TableData();
        sum.put("name", "合计");
        sum.put("sum", BigDecimal.ZERO);
        for (MeteringPoint config : configs) {
            TableData tableData = new TableData();
            Map<LocalDateTime, BigDecimal> dateTimeBigDecimalMap = dataMap.get(config.getId());
            for(TableHeader header : tableHeaderList){
                String field = header.getField();
                if (field.equals("sum")) {
                    continue;
                }
                if (field.equals("name")) {
                    tableData.put(field, config.getNodeName());
                    continue;
                }
                LocalDateTime localDateTime = LocalDateTime.parse(field, filedForMatter);
                if(!sum.containsKey(field)){
                    sum.put(field,BigDecimal.ZERO);
                }
                BigDecimal value = dateTimeBigDecimalMap == null ? BigDecimal.ZERO : dateTimeBigDecimalMap.getOrDefault(localDateTime, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                tableData.put(field, value);
                sum.put(field,((BigDecimal)sum.get(field)).add(value));
            }
            tableData.calculateSum();
            sum.put("sum",((BigDecimal) sum.get("sum")).add((BigDecimal) tableData.get("sum")));
            tableDataList.add(tableData);
        }
        tableDataList.add(sum);
        Table table = new Table();
        table.setTableHeaderList(tableHeaderList);
        table.setTableDataList(tableDataList);
        return table;
    }

    private List<Long> strToLongList(String str) {
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * 将树倒序输出，子->父
     */
    private List<MeteringPoint> reverseTree(List<MeteringPoint> rules){
        Map<Long,List<MeteringPoint>> listMap = rules.stream().collect(Collectors.groupingBy(MeteringPoint::getParentId, Collectors.toList()));
        List<Long> parentIds = new ArrayList<Long>(){{add(MeteringPoint.ROOT_ID);}};
        List<MeteringPoint> res = new ArrayList<>();
        while(CollectionUtil.isNotEmpty(parentIds)){
            Long parentId = parentIds.remove(0);
            List<MeteringPoint> list = listMap.get(parentId);
            if(CollectionUtil.isNotEmpty(list)){
                parentIds.addAll(list.stream().map(MeteringPoint::getId).collect(Collectors.toList()));
                res.addAll(list);
            }
        }
        Collections.reverse(res);
        return res;
    }

    private Map<String,BigDecimal> meteringPointDataToMapByDateType(List<? extends MeteringPointData> dataList,String dateType){
        DateTimeFormatter formatter;
        if(dateType.equals("month")){
            formatter = monthFormatter;
        }else if(dateType.equals("year")){
            formatter = yearFormatter;
        } else {
            formatter = dayFormatter;
        }
        return dataList.stream().collect(Collectors.groupingBy(item -> item.getTime().format(formatter), Collectors.mapping(MeteringPointData::getValue, Collectors.reducing(BigDecimal.ZERO,BigDecimal::add))));
    }

    private List<String> getXAxis(String dateType,LocalDate startDate,LocalDate endDate){
        dateType = StringUtil.isEmpty(dateType) ? "day" : dateType;
        switch (dateType) {
            case "day":
                return LongStream.range(0L, ChronoUnit.DAYS.between(startDate, endDate)).mapToObj(i -> startDate.plusDays(i).format(dayFormatter)).collect(Collectors.toList());
            case "month":
                return LongStream.range(0L, ChronoUnit.MONTHS.between(startDate, endDate)).mapToObj(i -> startDate.plusMonths(i).format(monthFormatter)).collect(Collectors.toList());
            case "year":
                return LongStream.range(0L, ChronoUnit.YEARS.between(startDate, endDate)).mapToObj(i -> String.valueOf(startDate.plusYears(i).getYear())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void initChatParam(MeteringPointChatDto param) {
        // 填充默认数据，默认查询七天的数据
        if (param.getStartDate() == null) {
            param.setStartDate(LocalDate.now().minusDays(7));
        }
        if (param.getEndDate() == null) {
            param.setEndDate(LocalDate.now());
        }
        if (param.getBaseStartDate() == null) {
            param.setBaseStartDate(LocalDate.now().minusDays(7));
        }
        if (param.getBaseEndDate() == null) {
            param.setBaseEndDate(LocalDate.now());
        }
    }

}
