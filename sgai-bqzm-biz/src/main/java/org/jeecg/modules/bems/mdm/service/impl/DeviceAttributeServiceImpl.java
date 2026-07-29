package org.jeecg.modules.bems.mdm.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.mdm.dto.AttributeBindingDto;
import org.jeecg.modules.bems.mdm.dto.AttributeData;
import org.jeecg.modules.bems.mdm.dto.DeviceAttributeData;
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.mapper.DeviceAttributeMapper;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeHistoryService;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.vo.DeviceAttributeDataVo;
import org.jeecg.modules.bems.mq.send.MqSendService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceAttributeServiceImpl extends ServiceImpl<DeviceAttributeMapper, DeviceAttribute> implements IDeviceAttributeService {

    private final MqSendService mqSendService;

    private final IDeviceService deviceService;

    private final IDeviceAttributeHistoryService deviceAttributeHistoryService;

    private static final String RUN_STATE = "def_comm_state";

    public DeviceAttributeServiceImpl(MqSendService mqSendService, @Lazy IDeviceService deviceService,IDeviceAttributeHistoryService deviceAttributeHistoryService) {
        this.mqSendService = mqSendService;
        this.deviceService = deviceService;
        this.deviceAttributeHistoryService = deviceAttributeHistoryService;
    }

    @Override
    public boolean save(DeviceAttribute entity){
        // 校验名称是否重复
        if(count(new LambdaQueryWrapper<DeviceAttribute>().eq(DeviceAttribute::getDeviceId, entity.getDeviceId()).eq(DeviceAttribute::getAttributeName, entity.getAttributeName())) > 0){
            throw new RuntimeException("已存在相同名称的属性");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(DeviceAttribute entity){
        entity.setDeviceId(null);
        // 校验名称是否重复
        if(count(new LambdaQueryWrapper<DeviceAttribute>().eq(DeviceAttribute::getDeviceId, entity.getDeviceId()).eq(DeviceAttribute::getAttributeName, entity.getAttributeName()).ne(DeviceAttribute::getId, entity.getId())) > 0){
            throw new RuntimeException("已存在相同名称的属性");
        }
        return super.updateById(entity);
    }

    @Override
    public IPage<DeviceAttribute> queryPage(DeviceAttribute params) {
        IPage<DeviceAttribute> page = new Page<>(params.getPageNo(),params.getPageSize());
        return page(page,new LambdaQueryWrapper<DeviceAttribute>()
                .eq(DeviceAttribute::getDeviceId, params.getDeviceId())
                .like(StringUtils.isNotEmpty(params.getAttributeName()), DeviceAttribute::getAttributeName, params.getAttributeName())
                .orderByDesc(DeviceAttribute::getSort));
    }

    @Override
    public List<DeviceAttributeDataVo> listByDeviceId(Long deviceId) {
        if(deviceId == null){
            return Collections.emptyList();
        }
        List<DeviceAttribute> list = list(new LambdaQueryWrapper<DeviceAttribute>().eq(DeviceAttribute::getDeviceId, deviceId).orderByAsc(DeviceAttribute::getSort));
        return list.stream().map(item -> DeviceAttributeDataVo.build(deviceId, item.getId(), item.getAttributeName(), item.getAttributeCode(), item.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<DeviceAttribute> getByDeviceId(Long deviceId) {
        return list(new LambdaQueryWrapper<DeviceAttribute>().eq(DeviceAttribute::getDeviceId, deviceId).orderByAsc(DeviceAttribute::getSort));
    }

    @Override
    public String getValueById(Long id) {
        DeviceAttribute entity = getById(id);
        return entity == null ? null : entity.getValue();
    }

    @Override
    @Transactional
    public void updateAttributeValue(Long deviceId,DeviceAttributeData data) {
        // 获取设备采集点位信息
        List<DeviceAttribute> byDeviceId = getByDeviceId(deviceId);
        if(byDeviceId.isEmpty()){
            return;
        }
        Map<String, DeviceAttribute> collect = byDeviceId.stream().collect(Collectors.toMap(DeviceAttribute::getAttributeCode, Function.identity()));
        List<DeviceAttribute> updateList = new ArrayList<>();
        for(AttributeData item : data.getData()){
            // 判断是否变更
            DeviceAttribute deviceAttribute = collect.get(item.getUniqueKey());
            if(deviceAttribute == null){
                continue;
            }
            deviceAttribute.setValue(item.getValue());
            deviceAttribute.setGatherTime(data.getTimestamp());
            updateList.add(deviceAttribute);
            update(new LambdaUpdateWrapper<DeviceAttribute>()
                    .eq(DeviceAttribute::getDeviceId, deviceId)
                    .eq(DeviceAttribute::getAttributeCode,item.getUniqueKey())
                    .set(DeviceAttribute::getValue, item.getValue())
                    .set(DeviceAttribute::getGatherTime, data.getTimestamp())
            );
        }
        // TODO 点位值变化，发送消息
        // 保存历史
        deviceAttributeHistoryService.saveAttributeHistory(updateList);
        try{
            for(DeviceAttribute item : updateList){
                mqSendService.sendDeviceAttributeValueChange(item.getDeviceId(),item.getId(),item.getValue());
            }
        }catch (Exception e){
            log.error("点位值变化消息发送失败",e);
        }
    }

    @Override
    public List<DeviceAttribute> findByDeviceIds(Collection<Long> deviceIds) {
        if(CollectionUtil.isEmpty(deviceIds)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DeviceAttribute>().in(DeviceAttribute::getDeviceId, deviceIds));
    }

    @Override
    public List<DeviceAttribute> findByIds(Collection<Long> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DeviceAttribute>().in(DeviceAttribute::getId, ids));
    }

    @Override
    public List<DeviceAttribute> findByAttributeCodes(Collection<String> attributeCodes) {
        if (CollectionUtil.isEmpty(attributeCodes)){
            return Collections.emptyList();
        }
        return super.list(new LambdaQueryWrapper<DeviceAttribute>()
                .in(DeviceAttribute::getAttributeCode, attributeCodes));
    }

    /**
     * 设备属性绑定楼控点位
     */
    @Override
    public void bindingBuildingControlPoint(AttributeBindingDto data) {
        String key = getBuildingControlPointKey(data.getGatewayAdr(), data.getBacnetAdr());
        // 更新
        update(new LambdaUpdateWrapper<DeviceAttribute>().eq(DeviceAttribute::getId,data.getPointId()).set(DeviceAttribute::getAcquisitionCoding,key));
    }

    /**
     * 设备属性关联楼控点位数据更新
     *
     * @param gatewayAdr     网关地址
     * @param bacnetAdr      BACnet地址
     * @param value          采集值
     * @param collectionTime 采集时间
     */
    @Override
    public void updateAttributeValue(String gatewayAdr, String bacnetAdr, String value, LocalDateTime collectionTime) {
        String key = getBuildingControlPointKey(gatewayAdr, bacnetAdr);
        update(new LambdaUpdateWrapper<DeviceAttribute>()
                .eq(DeviceAttribute::getAcquisitionCoding,key)
                .set(DeviceAttribute::getValue, value)
                .set(DeviceAttribute::getGatherTime, collectionTime)
        );
        // 获取点位关联设备属性
        List<DeviceAttribute> list = list(new LambdaQueryWrapper<DeviceAttribute>().eq(DeviceAttribute::getAcquisitionCoding, key));
        if(CollectionUtil.isEmpty(list)){
            return;
        }
        Set<Long> deviceIds = list.stream().map(DeviceAttribute::getDeviceId).collect(Collectors.toSet());
        Map<Long,Device> deviceMap = deviceService.findByDeviceIds(deviceIds)
                .stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(),(k1,k2) -> k2));

        for(DeviceAttribute item : list){
            Device device = deviceMap.get(item.getDeviceId());
            if(device == null){
                continue;
            }
            // 设备点位值变更
            mqSendService.sendDeviceAttributeValueChange(item.getDeviceId(),item.getId(),value);
            mqSendService.sendDeviceLastGatherTime(device.getDeviceCode(),collectionTime,collectionTime.plusMinutes(20));
        }
    }

    @Override
    public void updateAttributeForRunState(String deviceCode, String runState) {
        // 获取设备信息
        Device device = deviceService.getByDeviceCode(deviceCode);
        if(device == null){
            return;
        }
        DeviceAttributeData data = new DeviceAttributeData();
        data.setTimestamp(LocalDateTime.now());
        data.setEquipmentCode(deviceCode);
        AttributeData attributeData = new AttributeData();
        attributeData.setUniqueKey(RUN_STATE);
        attributeData.setValue("在线".equals(runState) ? "1" : "0");
        data.setData(Collections.singletonList(attributeData));
        this.updateAttributeValue(device.getId(),data);
    }

    /**
     * 增加运行状态属性
     *
     * @param deviceId 设备id
     */
    @Override
    public void addRunStateAttribute(Long deviceId) {
        DeviceAttribute attribute = new DeviceAttribute();
        attribute.setDeviceId(deviceId);
        attribute.setAttributeName("通讯状态");
        attribute.setAttributeCode(RUN_STATE);
        attribute.setReadwriteLevel("0");
        attribute.setSort(1);
        save(attribute);
    }

    @Override
    public List<DeviceAttribute> findByDeviceIdsAndCode(Collection<Long> deviceIds, String code) {
        if(CollectionUtil.isEmpty(deviceIds) || StringUtils.isBlank(code)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DeviceAttribute>()
                .eq(DeviceAttribute::getAttributeCode,code)
                .in(DeviceAttribute::getDeviceId,deviceIds));
    }

    private String getBuildingControlPointKey(String gatewayAdr,String bacnetAdr){
        return gatewayAdr + "-" + bacnetAdr;
    }
}
