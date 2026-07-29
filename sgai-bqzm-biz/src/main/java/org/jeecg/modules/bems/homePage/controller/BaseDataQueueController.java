package org.jeecg.modules.bems.homePage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.bems.alarm.dto.AlarmRecordDto;
import org.jeecg.modules.bems.alarm.entity.AlarmCategory;
import org.jeecg.modules.bems.alarm.entity.AlarmRecord;
import org.jeecg.modules.bems.alarm.service.IAlarmCategoryService;
import org.jeecg.modules.bems.alarm.service.IAlarmRecordService;
import org.jeecg.modules.bems.energyAnalysis.entity.*;
import org.jeecg.modules.bems.energyAnalysis.service.*;
import org.jeecg.modules.bems.energyAnalysis.vo.Chat;
import org.jeecg.modules.bems.energyAnalysis.vo.ChatSeries;
import org.jeecg.modules.bems.homePage.dto.AlarmStatisticsDto;
import org.jeecg.modules.bems.homePage.dto.DeviceRunStateStatisticDto;
import org.jeecg.modules.bems.homePage.dto.RankingDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 通用数据查询
 */
@RestController
@RequestMapping("/bems/dataQueue")
@AllArgsConstructor
public class BaseDataQueueController {

    private final IBusinessConfigService businessConfigService;

    private final IMeteringPointDataDayService dayService;

    private final IMeteringPointDataMonthService monthService;

    private final IMeteringPointDataYearService yearService;

    private final IMeteringPointService meteringPointService;

    private final IEquipmentCategoryService categoryService;

    private final IDeviceService deviceService;

    private final IMeteringPointDataHourService hourService;

    private final IAlarmRecordService alarmRecordService;

    private final ICarbonEmissionFactorService carbonEmissionFactorService;

    private final IEnergyFlowDiagramService energyFlowDiagramService;

    private final IAlarmCategoryService alarmCategoryService;

    /**
     * 获取当天的点位值
     *
     * @param configPath 配置项地址
     * @return 当天点位值
     */
    @IgnoreAuth
    @GetMapping("/getPointDataForToDay")
    public Result<String> getPointDataForToDay(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataDay pointDataDay = dayService.findByDateAndPointId(LocalDate.now(), pointId);
        String result = pointDataDay == null || pointDataDay.getValue() == null ? "" : pointDataDay.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return Result.OK("", result);
    }

    /**
     * 获取当月的点位值
     *
     * @param configPath 配置项地址
     * @return 当月的点位值
     */
    @IgnoreAuth
    @GetMapping("/getPointDataForThisMonth")
    public Result<String> getPointDataForThisMonth(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataMonth pointData = monthService.findByDateAndPointId(LocalDate.now(), pointId);
        String result = pointData == null || pointData.getValue() == null ? "" : pointData.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return Result.OK("", result);
    }

    /**
     * 获取当年的点位值
     *
     * @param configPath 配置项地址
     * @return 当年的点位值
     */
    @IgnoreAuth
    @GetMapping("/getPointDataForThisYear")
    public Result<String> getPointDataForThisYear(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataYear pointData = yearService.findByDateAndPointId(LocalDate.now(), pointId);
        String result = pointData == null || pointData.getValue() == null ? "" : pointData.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return Result.OK("", result);
    }

    /**
     * 获取当天多个点位值的总和
     *
     * @param configPath 配置项地址
     * @return 当天的多个点位值的总和
     */
    @IgnoreAuth
    @GetMapping("/getPointsDataForToDay")
    public Result<String> getPointsDataForToDay(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        BigDecimal sum = dayService.findByDateAndPointIds(LocalDate.now(), pointIds)
                .stream()
                .map(MeteringPointData::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.OK("", sum.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * 获取当月多个点位值的总和
     *
     * @param configPath 配置项地址
     * @return 当月的多个点位值的总和
     */
    @IgnoreAuth
    @GetMapping("/getPointsDataForThisMonth")
    public Result<String> getPointsDataForThisMonth(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        BigDecimal sum = monthService.findByDateAndPointIds(LocalDate.now(), pointIds)
                .stream()
                .map(MeteringPointData::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.OK("", sum.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * 获取当年的多个点位值的总和
     *
     * @param configPath 配置项地址
     * @return 当年的多个点位值的总和
     */
    @IgnoreAuth
    @GetMapping("/getPointsDataForThisYear")
    public Result<String> getPointsDataForThisYear(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        BigDecimal sum = yearService.findByDateAndPointIds(LocalDate.now(), pointIds)
                .stream()
                .map(MeteringPointData::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.OK("", sum.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * 获取当天点位趋势
     *
     * @param configPath 配置项地址
     * @return 当天点位趋势
     */
    @IgnoreAuth
    @GetMapping("/getPointTrendForToDay")
    public Result<Chat> getPointTrendForToDay(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        return Result.OK("", getPointTrendForToDay(pointId));
    }

    /**
     * 获取当月点位趋势
     *
     * @param configPath 配置项地址
     * @return 当月点位趋势
     */
    @IgnoreAuth
    @GetMapping("/getPointTrendForThisMonth")
    public Result<Chat> getPointTrendForThisMonth(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);

        return Result.OK("", getPointTrendForThisMonth(pointId));
    }

    /**
     * 获取当年的点位趋势
     *
     * @param configPath 配置项地址
     * @return 当年的点位趋势
     */
    @IgnoreAuth
    @GetMapping("/getPointTrendForThisYear")
    public Result<Chat> getPointTrendForThisYear(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        return Result.OK("", getPointTrendForThisYear(pointId));
    }

    /**
     * 获取月对比趋势，当月、上月、去年本月的趋势对比
     *
     * @param configPath 配置项地址
     * @return 月对比趋势
     */
    @GetMapping("/getPointDataMonthlyComparisonTrend")
    public Result<?> getPointDataMonthlyComparisonTrend(@RequestParam String configPath) {
        Long pointId = businessConfigService.getLongByKey(configPath);
        LocalDate now = LocalDate.now();
        Map<String, BigDecimal> thisMonth = dayService.findByTimeRangeAndPointId(now.withDayOfMonth(1), now, pointId)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getDayOfMonth()), MeteringPointDataDay::getValue, (k1, k2) -> k2));
        Map<String, BigDecimal> lastMonth = dayService.findByTimeRangeAndPointId(now.minusMonths(1).withDayOfMonth(1), now.minusMonths(1), pointId)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getDayOfMonth()), MeteringPointDataDay::getValue, (k1, k2) -> k2));
        Map<String, BigDecimal> lastYear = dayService.findByTimeRangeAndPointId(now.minusYears(1).withDayOfMonth(1), now.minusYears(1), pointId)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getDayOfMonth()), MeteringPointDataDay::getValue, (k1, k2) -> k2));
        // 生成横坐标
        List<String> xAxis = IntStream.range(1, now.getDayOfMonth() + 1).mapToObj(String::valueOf).toList();
        Chat chat = new Chat();
        List<Object> thisMonthData = new ArrayList<>();
        List<Object> lastMonthData = new ArrayList<>();
        List<Object> lastYearData = new ArrayList<>();
        for (String x : xAxis) {
            thisMonthData.add(thisMonth.getOrDefault(x, BigDecimal.ZERO));
            lastMonthData.add(lastMonth.getOrDefault(x, BigDecimal.ZERO));
            lastYearData.add(lastYear.getOrDefault(x, BigDecimal.ZERO));
        }
        chat.setXAxis(xAxis);
        List<ChatSeries> chatSeriesList = new ArrayList<>();
        chatSeriesList.add(new ChatSeries("当月", thisMonthData));
        chatSeriesList.add(new ChatSeries("上月", lastMonthData));
        chatSeriesList.add(new ChatSeries("去年本月", lastYearData));
        chat.setChatSeriesList(chatSeriesList);
        return Result.ok(chat);
    }

    /**
     * 获取多个点位日排行
     *
     * @param configPath 配置项地址
     * @return 排行
     */
    @IgnoreAuth
    @GetMapping("/getPointDataDailyRanking")
    public Result<?> getPointDataDailyRanking(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        return Result.ok(ranking(pointIds, dayService.findByDateAndPointIds(LocalDate.now(), pointIds)));
    }

    /**
     * 获取多个点位月排行
     *
     * @param configPath 配置项地址
     * @return 排行
     */
    @IgnoreAuth
    @GetMapping("/getPointDataMonthlyRanking")
    public Result<?> getPointDataMonthlyRanking(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        return Result.ok(ranking(pointIds, monthService.findByDateAndPointIds(LocalDate.now(), pointIds)));
    }

    /**
     * 获取多个点位年排行
     *
     * @param configPath 配置项地址
     * @return 排行
     */
    @IgnoreAuth
    @GetMapping("/getPointDataAnnualRanking")
    public Result<?> getPointDataAnnualRanking(@RequestParam String configPath) {
        List<Long> pointIds = businessConfigService.getListByKey(configPath, Long.class);
        return Result.ok(ranking(pointIds, yearService.findByDateAndPointIds(LocalDate.now(), pointIds)));
    }

    /**
     * 获取设备运行状态统计
     *
     * @param configPath 配置项地址
     * @return 运行状态统计
     */
    @GetMapping("/getDeviceRunStateStatistic")
    public Result<?> getDeviceRunStateStatistic(@RequestParam String configPath) {
        List<Long> categoryIds = businessConfigService.getListByKey(configPath, Long.class);
        // 获取类别名称
        List<EquipmentCategory> categories = categoryService.findByIds(categoryIds);
        // 获取设备类别下设备信息
        Map<Long, List<Device>> devices = deviceService.findByCategoryIds(categoryIds)
                .stream()
                .collect(Collectors.groupingBy(Device::getCategoryId, Collectors.toList()));
        List<DeviceRunStateStatisticDto> result = new ArrayList<>();
        for (EquipmentCategory item : categories) {
            List<Device> list = devices.getOrDefault(item.getId(), Collections.emptyList());
            long onLine = list.stream().filter(device -> "在线".equals(device.getRunState())).count();
            DeviceRunStateStatisticDto dto = new DeviceRunStateStatisticDto();
            dto.setCategoryName(item.getCategoryName());
            dto.setOnLineNum(onLine);
            dto.setOffLineNum(list.size() - onLine);
            dto.setTotalNum((long) list.size());
            result.add(dto);
        }
        return Result.ok(result);
    }

    /**
     * 获取今日告警列表
     *
     * @param pageNo   页码
     * @param pageSize 页大小
     * @return 告警列表
     */
    @IgnoreAuth
    @GetMapping("/getAlarmListForToDay")
    public Result<?> getAlarmListForToDay(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) Integer pageSize) {
        return Result.ok(getAlarmList(pageNo, pageSize, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0), LocalDateTime.now()));
    }

    /**
     * 获取本月告警列表
     *
     * @param pageNo   页码
     * @param pageSize 页大小
     * @return 告警列表
     */
    @IgnoreAuth
    @GetMapping("/getAlarmListForThisMonth")
    public Result<?> getAlarmListForThisMonth(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) Integer pageSize) {
        return Result.ok(getAlarmList(pageNo, pageSize, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0), LocalDateTime.now()));
    }

    /**
     * 获取今日碳排
     * @param configPath 配置项地址
     * @return 碳排数据
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsForToDay")
    public Result<?> getCarbonEmissionsForToDay(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataDay pointDataDay = dayService.findByDateAndPointId(LocalDate.now(), pointId);
        BigDecimal energy = pointDataDay == null || pointDataDay.getValue() == null ? BigDecimal.ZERO : pointDataDay.getValue();
        return Result.OK("",getCarbonEmissions(energy));
    }

    /**
     * 获取本月碳排
     * @param configPath 配置项地址
     * @return 碳排数据
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsForThisMonth")
    public Result<?> getCarbonEmissionsForThisMonth(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataMonth pointDataMonth = monthService.findByDateAndPointId(LocalDate.now(), pointId);
        BigDecimal energy = pointDataMonth == null || pointDataMonth.getValue() == null ? BigDecimal.ZERO : pointDataMonth.getValue();
        return Result.OK("",getCarbonEmissions(energy));
    }

    /**
     * 获取今年碳排
     * @param configPath 配置项地址
     * @return 碳排数据
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsForThisYear")
    public Result<?> getCarbonEmissionsForThisYear(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        MeteringPointDataYear pointDataYear = yearService.findByDateAndPointId(LocalDate.now(), pointId);
        BigDecimal energy = pointDataYear == null || pointDataYear.getValue() == null ? BigDecimal.ZERO : pointDataYear.getValue();
        return Result.OK("",getCarbonEmissions(energy));
    }

    /**
     * 获取今日碳排趋势
     * @param configPath 配置项地址
     * @return 今日碳排趋势
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsTrendForToDay")
    public Result<?> getCarbonEmissionsTrendForToDay(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        Chat chat = getPointTrendForToDay(pointId);
        return Result.ok(getCarbonEmissionsTrend(chat));
    }

    /**
     * 获取本月碳排趋势
     * @param configPath 配置项地址
     * @return 本月碳排趋势
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsTrendForThisMonth")
    public Result<?> getCarbonEmissionsTrendForThisMonth(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        Chat chat = getPointTrendForThisMonth(pointId);
        return Result.ok(getCarbonEmissionsTrend(chat));
    }

    /**
     * 获取今年碳排趋势
     * @param configPath 配置项地址
     * @return 今年碳排趋势
     */
    @IgnoreAuth
    @GetMapping("/getCarbonEmissionsTrendForThisYear")
    public Result<?> getCarbonEmissionsTrendForThisYear(@RequestParam String configPath){
        Long pointId = businessConfigService.getLongByKey(configPath);
        Chat chat = getPointTrendForThisYear(pointId);
        return Result.ok(getCarbonEmissionsTrend(chat));
    }

    /**
     * 获取今日能流图
     * 点位及点位下所有点位
     * @param configPath 配置项地址
     * @return 今日能流图
     */
    @IgnoreAuth
    @GetMapping("/getEnergyFlowDiagramForToDay")
    public Result<?> getEnergyFlowDiagramForToDay(@RequestParam String configPath,@RequestParam(required = false) Integer level){
        Long pointId = businessConfigService.getLongByKey(configPath);
        return Result.ok(energyFlowDiagramService.findDay(pointId,level, LocalDate.now()));
    }

    /**
     * 获取当月能流图
     * 点位及点位下所有点位
     * @param configPath 配置项地址
     * @return 当月能流图
     */
    @IgnoreAuth
    @GetMapping("/getEnergyFlowDiagramForThisMonth")
    public Result<?> getEnergyFlowDiagramForThisMonth(@RequestParam String configPath,@RequestParam(required = false) Integer level){
        Long pointId = businessConfigService.getLongByKey(configPath);
        return Result.ok(energyFlowDiagramService.findMonth(pointId,level, LocalDate.now()));
    }

    /**
     * 获取当年能流图
     * 点位及点位下所有点位
     * @param configPath 配置项地址
     * @return 当年能流图
     */
    @IgnoreAuth
    @GetMapping("/getEnergyFlowDiagramForThisYear")
    public Result<?> getEnergyFlowDiagramForThisYear(@RequestParam String configPath,@RequestParam(required = false) Integer level){
        Long pointId = businessConfigService.getLongByKey(configPath);
        return Result.ok(energyFlowDiagramService.findYear(pointId,level, LocalDate.now()));
    }

    /**
     * 获取今日告警分类统计
     * @return 今日告警分类统计
     */
    @GetMapping("/getAlarmCategoryStatisticForToDay")
    public Result<?> getAlarmCategoryStatisticForToDay(){
        return Result.ok(getAlarmCategoryStatistics(LocalDate.now().atStartOfDay(),LocalDateTime.now()));
    }

    /**
     * 获取当月告警分类统计
     * @return 当月告警分类统计
     */
    @GetMapping("/getAlarmCategoryStatisticForThisMonth")
    public Result<?> getAlarmCategoryStatisticForThisMonth(){
        return Result.ok(getAlarmCategoryStatistics(LocalDate.now().atStartOfDay().withDayOfMonth(1),LocalDateTime.now()));
    }

    @GetMapping("/getDeviceRunStateStatisticByBuilding")
    public Result<?> getDeviceRunStateStatisticByBuilding(){
        List<Device> devices = deviceService.findByType(Device.DEVICE_TYPE_EQUIPMENT);
        // 统计在线离线
        List<DeviceRunStateStatisticDto> result = new ArrayList<>();
        DeviceRunStateStatisticDto dto = new DeviceRunStateStatisticDto();
        dto.setCategoryName("楼控设备");
        dto.setTotalNum((long) devices.size());
        long onLine = devices.stream().filter(device -> "在线".equals(device.getRunState())).count();
        dto.setOnLineNum(onLine);
        dto.setOffLineNum(devices.size() - onLine);
        result.add(dto);
        return Result.ok(result);
    }

    private List<AlarmStatisticsDto> getAlarmCategoryStatistics(LocalDateTime startTime,LocalDateTime endTime){
        Map<Long,Long> alarmRecordGroup = alarmRecordService.listByAlarmTimeRange(startTime, endTime)
                .stream()
                .collect(Collectors.groupingBy(AlarmRecord::getAlarmCategoryId,Collectors.counting()));
        List<AlarmCategory> alarmCategory = alarmCategoryService.list();
        List<AlarmStatisticsDto> result = new ArrayList<>();
        for(AlarmCategory category : alarmCategory){
            result.add(new AlarmStatisticsDto(category.getAlarmCategoryName(),alarmRecordGroup.getOrDefault(category.getId(),0L)));
        }
        return result;
    }

    private Chat getCarbonEmissionsTrend(Chat chat){
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        for (ChatSeries chatSeries : chat.getChatSeriesList()) {
            List<Object> newData = chatSeries.getData().stream()
                    .map(item -> ((BigDecimal) item).multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP))
                    .collect(Collectors.toList());
            chatSeries.setData(newData);
        }
        return chat;
    }

    private Chat getPointTrendForToDay(Long pointId){
        LocalDateTime now = LocalDateTime.now();
        Map<String, BigDecimal> dataMap = hourService.findByPointIdAndTimeRange(pointId, now.withHour(0).withMinute(0).withSecond(0), now)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getHour()), MeteringPointData::getValue, (k1, k2) -> k2));
        List<String> xAxis = IntStream.range(0, now.getHour()).mapToObj(String::valueOf).toList();
        List<Object> series = new ArrayList<>();
        for (String x : xAxis) {
            series.add(dataMap.getOrDefault(x, BigDecimal.ZERO));
        }
        Chat chat = new Chat();
        chat.setXAxis(xAxis);
        chat.setChatSeriesList(List.of(new ChatSeries("", series)));
        return chat;
    }

    private Chat getPointTrendForThisMonth(Long pointId){
        LocalDate now = LocalDate.now();
        Map<String, BigDecimal> dataMap = dayService.findByTimeRangeAndPointId(now.withDayOfMonth(1), now, pointId)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getDayOfMonth()), MeteringPointData::getValue, (k1, k2) -> k2));
        List<String> xAxis = IntStream.range(1, now.getDayOfMonth() + 1).mapToObj(String::valueOf).toList();
        List<Object> series = new ArrayList<>();
        for (String x : xAxis) {
            series.add(dataMap.getOrDefault(x, BigDecimal.ZERO));
        }
        Chat chat = new Chat();
        chat.setXAxis(xAxis);
        chat.setChatSeriesList(List.of(new ChatSeries("", series)));
        return chat;
    }

    private Chat getPointTrendForThisYear(Long pointId){
        LocalDate now = LocalDate.now();
        Map<String, BigDecimal> dataMap = monthService.findByTimeRangeAndPointId(now.withDayOfMonth(1), now, pointId)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getTime().getMonthValue()), MeteringPointData::getValue, (k1, k2) -> k2));
        List<String> xAxis = IntStream.range(1, now.getMonthValue() + 1).mapToObj(String::valueOf).toList();
        List<Object> series = new ArrayList<>();
        for (String x : xAxis) {
            series.add(dataMap.getOrDefault(x, BigDecimal.ZERO));
        }
        Chat chat = new Chat();
        chat.setXAxis(xAxis);
        chat.setChatSeriesList(List.of(new ChatSeries("", series)));
        return chat;
    }

    private String getCarbonEmissions(BigDecimal energy){
        BigDecimal emissionFactor = carbonEmissionFactorService.getElectricityCarbonEmissionFactor();
        return energy.multiply(emissionFactor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 获取时间范围内的报警数据
     *
     * @param pageNo        页码
     * @param pageSize      页大小
     * @param startDateTime 开始时间
     * @param endDateTime   结束时间
     * @return 告警列表
     */
    private IPage<AlarmRecord> getAlarmList(Integer pageNo, Integer pageSize, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        pageNo = pageNo == null ? 1 : pageNo;
        pageSize = pageSize == null ? 30 : pageSize;
        AlarmRecordDto param = new AlarmRecordDto();
        param.setStartDateTime(startDateTime);
        param.setEndDateTime(endDateTime);
        param.setPageNo(pageNo);
        param.setPageSize(pageSize);
        return alarmRecordService.listPage(param);
    }

    private List<RankingDto> ranking(List<Long> pointIds, List<? extends MeteringPointData> data) {
        List<MeteringPoint> points = meteringPointService.getByIds(pointIds);
        Map<Long, BigDecimal> meteringPointData = data
                .stream()
                .collect(Collectors.toMap(MeteringPointData::getMeteringPointId, MeteringPointData::getValue, (k1, k2) -> k2));
        List<RankingDto> result = new ArrayList<>();
        for (MeteringPoint point : points) {
            RankingDto dto = new RankingDto();
            dto.setId(point.getId());
            dto.setName(point.getNodeName());
            dto.setValue(meteringPointData.getOrDefault(point.getId(), BigDecimal.ZERO).toPlainString());
            result.add(dto);
        }

        return result.stream()
                .sorted(Comparator.comparing(dto -> new BigDecimal(dto.getValue()), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

}
