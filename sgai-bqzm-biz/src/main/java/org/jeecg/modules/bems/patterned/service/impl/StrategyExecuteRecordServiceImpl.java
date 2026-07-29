package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.mdm.entity.DeviceAttribute;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.patterned.dto.StrategyExecuteRecordDto;
import org.jeecg.modules.bems.patterned.entity.PatterningPoint;
import org.jeecg.modules.bems.patterned.entity.PatterningStrategy;
import org.jeecg.modules.bems.patterned.entity.PointExecuteRecord;
import org.jeecg.modules.bems.patterned.entity.StrategyExecuteRecord;
import org.jeecg.modules.bems.patterned.mapper.StrategyExecuteRecordMapper;
import org.jeecg.modules.bems.patterned.service.IPointExecuteRecordService;
import org.jeecg.modules.bems.patterned.service.IStrategyExecuteRecordService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class StrategyExecuteRecordServiceImpl extends ServiceImpl<StrategyExecuteRecordMapper, StrategyExecuteRecord> implements IStrategyExecuteRecordService {

    private final IPointExecuteRecordService pointExecuteRecordService;

    private final IDeviceAttributeService deviceAttributeService;

    @Override
    @Transactional
    public void addLog(PatterningStrategy strategy) {
        LocalDateTime now = LocalDateTime.now();
        StrategyExecuteRecord record = getStrategyExecuteRecord(strategy, now);
        save(record);
        // 获取点位配置
        Map<Long,DeviceAttribute> deviceAttributeMap = deviceAttributeService.findByIds(strategy.getPatterningPointList().stream().map(PatterningPoint::getPointId).toList())
                .stream()
                .collect(Collectors.toMap(DeviceAttribute::getId, Function.identity()));
        // 保存点位执行信息
//        List<PointExecuteRecord> pointExecuteRecordList = new ArrayList<>();
        for (PatterningPoint patterningPoint : strategy.getPatterningPointList()) {
            PointExecuteRecord pointExecuteRecord = getPointExecuteRecord(patterningPoint, record, now);
            DeviceAttribute deviceAttribute = deviceAttributeMap.get(pointExecuteRecord.getPointId());
            if(deviceAttribute == null){
                pointExecuteRecord.setConditionRemark(pointExecuteRecord.getConditionValue());
            }else{
                pointExecuteRecord.setConditionRemark(deviceAttribute.convertValue(pointExecuteRecord.getConditionValue()));
            }
            pointExecuteRecordService.save(pointExecuteRecord);
//            pointExecuteRecordList.add(pointExecuteRecord);
        }
//        pointExecuteRecordService.saveBatch(pointExecuteRecordList);
    }

    private static @NotNull StrategyExecuteRecord getStrategyExecuteRecord(PatterningStrategy strategy, LocalDateTime now) {
        String executedBy = "场景控制";
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            executedBy = sysUser.getRealname();
        }catch (Exception e){

        }

        StrategyExecuteRecord record = new StrategyExecuteRecord();
        record.setBusinessKey(strategy.getId());
        record.setBusinessType(StrategyExecuteRecord.BusinessType_Patterning);
        record.setSuccessFlag(StrategyExecuteRecord.SuccessFlag_Executing);
        record.setExecutedTime(now);
        record.setDescription(strategy.getStrategyName());
        record.setExecutedBy(executedBy);
        return record;
    }

    @Override
    public Page<StrategyExecuteRecord> listPage(StrategyExecuteRecordDto params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()),new LambdaQueryWrapper<StrategyExecuteRecord>()
                .like(StringUtils.isNotEmpty(params.getStrategyName()), StrategyExecuteRecord::getDescription, params.getStrategyName())
                .between(params.getStartTime() != null && params.getEndTime() != null, StrategyExecuteRecord::getExecutedTime, params.getStartTime(), params.getEndTime())
                .ge(params.getStartTime() != null, StrategyExecuteRecord::getExecutedTime, params.getStartTime())
                .le(params.getEndTime() != null, StrategyExecuteRecord::getExecutedTime, params.getEndTime())
                .orderByDesc(StrategyExecuteRecord::getExecutedTime)
        );
    }

    private static @NotNull PointExecuteRecord getPointExecuteRecord(PatterningPoint patterningPoint, StrategyExecuteRecord record, LocalDateTime now) {
        PointExecuteRecord pointExecuteRecord = new PointExecuteRecord();
        pointExecuteRecord.setStrategyExecuteId(record.getId());
        pointExecuteRecord.setPointId(patterningPoint.getPointId());
        pointExecuteRecord.setPointName(patterningPoint.getPointName());
        pointExecuteRecord.setDeviceId(patterningPoint.getDeviceId());
        pointExecuteRecord.setDeviceName(patterningPoint.getDeviceName());
        pointExecuteRecord.setExecutedTime(now);
        pointExecuteRecord.setConditionValue(patterningPoint.getConditionValue());
        pointExecuteRecord.setSuccessFlag(StrategyExecuteRecord.SuccessFlag_Executing);
        return pointExecuteRecord;
    }
}
