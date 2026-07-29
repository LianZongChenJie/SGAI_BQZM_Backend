package org.jeecg.modules.bems.alarm.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.exception.JeecgCloudException;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;
import org.jeecg.modules.bems.alarm.entity.AlarmRulePoint;
import org.jeecg.modules.bems.alarm.entity.AlarmRules;
import org.jeecg.modules.bems.alarm.mapper.AlarmRulesMapper;
import org.jeecg.modules.bems.alarm.service.IAlarmLevelService;
import org.jeecg.modules.bems.alarm.service.IAlarmRulePointService;
import org.jeecg.modules.bems.alarm.service.IAlarmRulesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class AlarmRulesServiceImpl extends ServiceImpl<AlarmRulesMapper, AlarmRules> implements IAlarmRulesService {

    private final IAlarmRulePointService alarmRulePointService;

    private final IAlarmLevelService alarmLevelService;

    @Override
    @Transactional
    public boolean save(AlarmRules entity) {
        // 校验规则信息
        check(entity);
        // 获取告警级别信息
        AlarmLevel alarmLevel = alarmLevelService.getById(entity.getAlarmLevelId());
        if(alarmLevel == null){
            throw new JeecgBootException("未找到对应告警级别");
        }
        entity.setAlarmLevelColor(alarmLevel.getAlarmLevelColor());
        entity.setEnabledStatus(AlarmRules.ENABLED_STATUS_DISABLE);
        boolean result = super.save(entity);
        // 保存关联点位信息,校验点位信息
        alarmRulePointService.save(entity.getId(),entity.getPoints());
        return result;
    }

    @Override
    @Transactional
    public boolean updateById(AlarmRules entity){
        // 校验规则信息
        check(entity);
        // 获取告警级别信息
        AlarmLevel alarmLevel = alarmLevelService.getById(entity.getAlarmLevelId());
        if(alarmLevel == null){
            throw new JeecgBootException("未找到对应告警级别");
        }
        entity.setAlarmLevelColor(alarmLevel.getAlarmLevelColor());
        entity.setEnabledStatus(null);
        boolean result = super.updateById(entity);
        alarmRulePointService.save(entity.getId(),entity.getPoints());
        return result;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        checkEnablesStatus((Long) id);
        alarmRulePointService.removeByAlarmRuleId((Long) id);
        return super.removeById(id);
    }

    private void check(AlarmRules entity) {
        checkEnablesStatus(entity.getId());
        if(count(new LambdaQueryWrapper<AlarmRules>().eq(AlarmRules::getRuleCode, entity.getRuleCode()).ne(entity.getId() != null,AlarmRules::getId, entity.getId())) > 0){
            throw new JeecgBootException("已存在相同编号的告警规则");
        }
        if(count(new LambdaQueryWrapper<AlarmRules>().eq(AlarmRules::getRuleName, entity.getRuleName()).ne(entity.getId() != null,AlarmRules::getId, entity.getId())) > 0){
            throw new JeecgCloudException("已存在相同名称的告警规则");
        }
        // 校验是否有点位信息
        if(CollectionUtils.isEmpty(entity.getPoints())){
            throw new JeecgBootException("请添加点位信息");
        }
        // 报警点类型校验点位信息
        if(AlarmRules.POINT_TYPE_INSTANT.equals(entity.getPointType())){
            // 瞬时值
            for(AlarmRulePoint alarmRulePoint : entity.getPoints()){
                if(alarmRulePoint.getPointId() == null){
                    throw new JeecgBootException("关联设备参数异常");
                }
            }
        }else if(AlarmRules.POINT_TYPE_ACCUMULATE.equals(entity.getPointType())){
            // 累计值
            for (AlarmRulePoint alarmRulePoint : entity.getPoints()){
                if(!(AlarmRulePoint.TIME_GRANULARITY_HOUR.equals(alarmRulePoint.getTimeGranularity())
                        || AlarmRulePoint.TIME_GRANULARITY_DAY.equals(alarmRulePoint.getTimeGranularity())
                        || AlarmRulePoint.TIME_GRANULARITY_MONTH.equals(alarmRulePoint.getTimeGranularity())
                        || AlarmRulePoint.TIME_GRANULARITY_YEAR.equals(alarmRulePoint.getTimeGranularity()))
                ){
                    throw new JeecgBootException("关联设备时间粒度错误");
                }
            }
        }else if(AlarmRules.POINT_TYPE_VIRTUAL.equals(entity.getPointType())){
            return;
        }else{
            throw new JeecgBootException("报警点类型错误");
        }
    }

    private void checkEnablesStatus(Long id){
        if(id != null){
            AlarmRules old = getById(id);
            if(AlarmRules.ENABLED_STATUS_ENABLE.equals(old.getEnabledStatus())){
                throw new JeecgBootException("告警规则已启用，禁止操作");
            }
        }
    }

    @Override
    public void startRule(Long id) {
        update(new LambdaUpdateWrapper<AlarmRules>()
                .eq(AlarmRules::getId, id)
                .set(AlarmRules::getEnabledStatus, AlarmRules.ENABLED_STATUS_ENABLE)
        );
        // TODO 启用规则后续操作
    }

    @Override
    public void stopRule(Long id) {
        update(new LambdaUpdateWrapper<AlarmRules>()
                .eq(AlarmRules::getId, id)
                .set(AlarmRules::getEnabledStatus, AlarmRules.ENABLED_STATUS_DISABLE)
        );
        // TODO 禁用规则后续操作
    }

    @Override
    public AlarmRules getDetailById(Long id) {
        AlarmRules alarmRules = getById(id);
        if(alarmRules == null){
            return null;
        }
        alarmRules.setPoints(alarmRulePointService.getByAlarmRuleId(id));
        return alarmRules;
    }

    @Override
    public IPage<AlarmRules> listPage(AlarmRules params) {
        return page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<AlarmRules>()
                        .like(params.getRuleName() != null, AlarmRules::getRuleName, params.getRuleName())
                        .like(params.getRuleCode() != null, AlarmRules::getRuleCode, params.getRuleCode())
                        .eq(params.getAlarmCategoryId() != null, AlarmRules::getAlarmCategoryId, params.getAlarmCategoryId())
                        .eq(params.getAlarmLevelId() != null, AlarmRules::getAlarmLevelId, params.getAlarmLevelId())
                        .eq(params.getEnabledStatus() != null, AlarmRules::getEnabledStatus, params.getEnabledStatus())
                        .orderByDesc(AlarmRules::getCreateTime)
        );
    }

    @Override
    public List<AlarmRules> listEnabledByIds(Collection<Long> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<AlarmRules>()
                .eq(AlarmRules::getEnabledStatus, AlarmRules.ENABLED_STATUS_ENABLE)
                .in(AlarmRules::getId, ids));
    }
}
