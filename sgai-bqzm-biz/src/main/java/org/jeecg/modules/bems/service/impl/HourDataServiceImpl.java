package org.jeecg.modules.bems.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.dto.DeviceDataFindDto;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.entity.HourData;
import org.jeecg.modules.bems.entity.RealData;
import org.jeecg.modules.bems.mapper.HourDataMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.service.IHourDataService;
import org.jeecg.modules.bems.service.IRealDataService;
import org.jeecg.modules.bems.vo.HourDataVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HourDataServiceImpl extends ServiceImpl<HourDataMapper,HourData> implements IHourDataService {

    private final IDeviceService deviceService;
    private final IRealDataService realDataService;

    private final RedisUtil redisUtil;

    private static final String CACHE_KEY_PREFIX = "device_energy_data_hour:";

    /**
     * 最新数据缓存key
     */
    private static final String CACHE_KEY_PREFIX_MAX = "device_energy_data_hour_max:";

    private static final long CACHE_TIME = 60L * 90L;
    @Override
    public HourData findByDeviceIdAndTime(Long deviceId, LocalDateTime time) {
        String cacheKey = getCacheKey(deviceId, time);
        HourData hourData = (HourData)redisUtil.get(cacheKey);
        if(hourData == null){
            HourData latest = findLatest(deviceId);
            if(latest == null || latest.getTime().isBefore(time)){
                hourData = getOne(new LambdaQueryWrapper<HourData>().eq(HourData::getDeviceId, deviceId).eq(HourData::getTime, time));
            }
        }
        if(hourData != null){
            redisUtil.set(cacheKey, hourData, CACHE_TIME);
        }
        return hourData;
    }

    @Override
    public boolean saveOrUpdate(HourData entity){
        if ((entity.getComputeValue() == null && entity.getValue() != null) || (entity.getComputeValue() != null && entity.getValue() != null && entity.getComputeValue().compareTo(entity.getValue()) != 0)) {
            entity.setUpdateBy("系统修正");
        }
        boolean b = super.saveOrUpdate(entity);
        if(b){
            redisUtil.set(getCacheKey(entity.getDeviceId(),entity.getTime()), entity, CACHE_TIME);
            HourData max = (HourData)redisUtil.get(getCacheKeyMax(entity.getDeviceId()));
            if(max == null || max.getTime().isBefore(entity.getTime())){
                redisUtil.set(getCacheKeyMax(entity.getDeviceId()), entity);
            }
        }
        return b;
    }

    @Override
    public void preGeneration(List<Long> deviceIds, LocalDate date) {
        Map<Long, Map<LocalDateTime, HourData>> hourDataMap = this.findByTimeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(HourData::getDeviceId,
                        Collectors.toMap(HourData::getTime, Function.identity(), (k1, k2) -> k2)
                ));
        for(Long deviceId : deviceIds){
            Map<LocalDateTime, HourData> dataMap = hourDataMap.getOrDefault(deviceId,new HashMap<>());
            List<HourData> list = new ArrayList<>();
            for(int i = 0; i < 24; i++){
                LocalDateTime time = date.atStartOfDay().plusHours(i);
                HourData hourData = dataMap.get(time);
                if(hourData == null){
                    hourData = new HourData();
                    hourData.setDeviceId(deviceId);
                    hourData.setTime(time);
                    list.add(hourData);
                }
            }
            super.saveBatch(list);
            list.addAll(dataMap.values());
            for (HourData hourData : list){
                redisUtil.set(getCacheKey(hourData.getDeviceId(),hourData.getTime()), hourData, 60L * 60L * 24L);
            }
        }
    }

    @Override
    public boolean updateById(HourData entity) {
        boolean b = super.updateById(entity);
        if(b) {
            redisUtil.set(getCacheKey(entity.getDeviceId(), entity.getTime()), entity, CACHE_TIME);
        }
        return b;
    }

    @Override
    public List<HourData> findByDeviceIdsAndTimeRange(Collection<Long> deviceIds, LocalDateTime startTime, LocalDateTime endTime) {
        if(CollectionUtil.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HourData> wrapper = new LambdaQueryWrapper<HourData>()
                .in(HourData::getDeviceId, deviceIds)
                .between(HourData::getTime, startTime, endTime);
        return list(wrapper);
    }

    @Override
    public IPage<HourDataVo> listPage(DeviceDataFindDto params) {
        List<Device> devices = deviceService.findMeasurementBySpaceIdAndCategoryId(params.getDeviceName(),params.getDeviceCode(),params.getSpaceIdList(), params.getCategoryIdList());
        LambdaQueryWrapper<HourData> wrapper = new LambdaQueryWrapper<>();
        if(devices.isEmpty()){
            return new Page<>();
        }
        wrapper.in(HourData::getDeviceId, devices.stream().map(Device::getId).collect(Collectors.toList()));

        if(params.getDateTime() != null){
            wrapper.between(HourData::getTime, params.getDateTime().withMinute(0).withSecond(0).withNano(0), params.getDateTime().withMinute(59).withSecond(59).withNano(0));
        }
        if(StringUtils.isNotEmpty(params.getAbnormalType())){
            switch(params.getAbnormalType()){
                case "空值异常":
                    wrapper.isNull(HourData::getComputeValue);
                    break;
                case "负值异常":
                    wrapper.lt(HourData::getComputeValue,0);
                    break;
            }
        }
        wrapper.orderByDesc(HourData::getTime);

        IPage<HourDataVo> list = page(new Page<>(params.getPageNo(), params.getPageSize()), wrapper).convert(HourDataVo::convert);
        List<HourDataVo> records = list.getRecords();
        Map<Long, Device> deviceMap = devices.stream().collect(Collectors.toMap(Device::getId, Function.identity(), (v1, v2) -> v1));
        for (HourDataVo record : records) {
            Device device = deviceMap.get(record.getDeviceId());
            if(device != null){
                record.setDeviceName(device.getDeviceName());
                record.setDeviceCode(device.getDeviceCode());
            }
        }
        return list;
    }

    @Override
    public List<HourData> findByDeviceIdAndTimes(Long deviceId, List<LocalDateTime> times) {
        return list(new LambdaQueryWrapper<HourData>().eq(HourData::getDeviceId, deviceId).in(HourData::getTime, times));
    }

    @Override
    public List<HourData> findByTime(LocalDateTime hour) {
        return list(new LambdaQueryWrapper<HourData>().eq(HourData::getTime, hour));
    }

    @Override
    public List<HourData> findByDeviceIdAndTimeRange(Long deiceId, LocalDateTime startTime, LocalDateTime endTime) {
        return list(new LambdaQueryWrapper<HourData>().eq(HourData::getDeviceId, deiceId).between(HourData::getTime, startTime, endTime));
    }

    /**
     * 设置小时能耗的开始值和结束值
     */
    @Override
    public void setStartValueAndEndValue(LocalDate month) {
        // 获取设备信息
        // 根据设备来设置开始值和结束值
        List<Device> list = deviceService.list();
        List<List<Device>> split = CollectionUtil.split(list, 20);
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<Future<String>> futures = new ArrayList<>();
        for(List<Device> devices : split){
            Future<String> future = executor.submit(() -> {
                for (Device device : devices) {
                    // 获取设备的小时数据
                    // 按月份来设置开始值和结束值
                    List<HourData> hourDataList = findByDeviceIdAndTimeRange(device.getId(), month.withDayOfMonth(1).atStartOfDay(),month.withDayOfMonth(1).plusMonths(1).atStartOfDay());
                    // 查询实时值
                    Map<LocalDateTime, BigDecimal> realDataMap = realDataService.findByDeviceIdAndTimeRange(device.getId(), month.withDayOfMonth(1).atStartOfDay(),month.withDayOfMonth(1).plusMonths(1).atStartOfDay())
                            .stream().collect(Collectors.toMap(RealData::getTime, RealData::getValue, (v1, v2) -> v1));
                    // 校验是否为空，返回
                    for(HourData hourData : hourDataList){
                        // 获取这个小时数据
                        LocalDateTime time = hourData.getTime();
                        BigDecimal endValue = realDataMap.get(time);
                        endValue  = endValue == null ? null : endValue.multiply(device.getMagnification());
                        // 获取上个小时数据
                        BigDecimal startValue = realDataMap.get(time.minusHours(1));
                        startValue  = startValue == null ? null : startValue.multiply(device.getMagnification());
                        hourData.setStartValue(startValue);
                        hourData.setEndValue(endValue);
                    }
                    updateBatchById(hourDataList);
                }
                return devices.stream().map(Device::getId).map(Object::toString).collect(Collectors.joining(","));
            });
            futures.add(future);
        }

        for (Future<String> future : futures) {
            try {
                System.out.println(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<HourData> findByDeviceIdsAndTime(Collection<Long> deviceIds, LocalDateTime time) {

        if(CollectionUtil.isEmpty(deviceIds) || time == null){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<HourData>()
                .eq(HourData::getTime, time)
                .in(HourData::getDeviceId, deviceIds));
    }

    @Override
    public List<HourData> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return super.list(new LambdaQueryWrapper<HourData>().between(HourData::getTime,startTime,endTime));
    }

    private HourData findLatest(Long deviceId){
        HourData hourData = (HourData) redisUtil.get(getCacheKeyMax(deviceId));
        if(hourData == null){
            hourData = findLatestByDeviceId(deviceId);
            if(hourData == null){
                return null;
            }
            redisUtil.set(getCacheKeyMax(deviceId),hourData);
        }
        return hourData;
    }

    private HourData findLatestByDeviceId(Long deviceId){
        List<HourData> list = list(
                new LambdaQueryWrapper<HourData>()
                        .eq(HourData::getDeviceId,deviceId)
                        .orderByDesc(HourData::getTime)
                        .comment("limit 1")
        );
        if(CollectionUtil.isEmpty(list)){
            return null;
        }
        return list.get(0);
    }

    private String getCacheKey(Long deviceId,LocalDateTime time){
        return CACHE_KEY_PREFIX + deviceId + "_" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String getCacheKeyMax(Long deviceId){
        return CACHE_KEY_PREFIX_MAX + deviceId;
    }
}
