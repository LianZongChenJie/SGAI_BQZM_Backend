package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.mdm.service.IDeviceAttributeService;
import org.jeecg.modules.bems.patterned.entity.FrontPoint;
import org.jeecg.modules.bems.patterned.entity.LinkageStrategy;
import org.jeecg.modules.bems.patterned.entity.RearPoint;
import org.jeecg.modules.bems.patterned.mapper.LinkageStrategyMapper;
import org.jeecg.modules.bems.patterned.service.IFrontPointService;
import org.jeecg.modules.bems.patterned.service.ILinkageStrategyService;
import org.jeecg.modules.bems.patterned.service.IRearPointService;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LinkageStrategyServiceImpl extends ServiceImpl<LinkageStrategyMapper, LinkageStrategy> implements ILinkageStrategyService {

    private final IFrontPointService frontPointService;

    private final IRearPointService rearPointService;

    private final SpelExpressionParser parser =  new SpelExpressionParser();

    private final IDeviceAttributeService deviceAttributeService;

    public LinkageStrategyServiceImpl(IFrontPointService frontPointService, IRearPointService rearPointService, IDeviceAttributeService deviceAttributeService) {
        this.frontPointService = frontPointService;
        this.rearPointService = rearPointService;
        this.deviceAttributeService = deviceAttributeService;
    }

    @Override
    @Transactional
    public boolean save(LinkageStrategy entity) {
        // 补充策略信息
        supplement(entity);
        boolean res = super.save(entity);
        // 保存前置点位
        entity.getFrontPointList().forEach(frontPoint -> {
            frontPoint.setLinkageStrategyId(entity.getId());
            frontPointService.save(frontPoint);
        });
        // 保存后置点位
        entity.getRearPointList().forEach(rearPoint -> {
            rearPoint.setLinkageStrategyId(entity.getId());
            rearPointService.save(rearPoint);
        });
        return res;
    }

    @Override
    @Transactional
    public boolean updateById(LinkageStrategy entity) {
        if(entity.getId() == null){
            throw new JeecgBootException("请选择策略");
        }
        LinkageStrategy byId = getById(entity.getId());
        if(byId == null){
            throw new JeecgBootException("策略不存在");
        }
        if(LinkageStrategy.Enable.equals(byId.getEnabledStatus())){
            throw new JeecgBootException("策略启用中，无法更改");
        }
        // 补充策略信息
        supplement(entity);
        entity.setEnabledStatus(null);
        boolean res = super.updateById(entity);
        // 删除原有点位
        frontPointService.removeByLinkageStrategyId(entity.getId());
        rearPointService.removeByLinkageStrategyId(entity.getId());
        // 更新前置点位
        entity.getFrontPointList().forEach(frontPoint -> {
            frontPoint.setLinkageStrategyId(entity.getId());
            frontPointService.save(frontPoint);
        });
        // 更新后置点位
        entity.getRearPointList().forEach(rearPoint -> {
            rearPoint.setLinkageStrategyId(entity.getId());
            rearPointService.save(rearPoint);
        });
        return res;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        LinkageStrategy byId = getById(id);
        if(LinkageStrategy.Enable.equals(byId.getEnabledStatus())){
            throw new JeecgBootException("策略启用中，无法删除");
        }
        // 删除原有点位
        frontPointService.removeByLinkageStrategyId(byId.getId());
        rearPointService.removeByLinkageStrategyId(byId.getId());
        return super.removeById(id);
    }

    @Override
    public LinkageStrategy getDetailById(Long id) {
        LinkageStrategy byId = getById(id);
        if(byId == null){
            throw new JeecgBootException("策略不存在");
        }
        byId.setFrontPointList(frontPointService.getListByLinkageStrategyId(id));
        byId.setRearPointList(rearPointService.getListByLinkageStrategyId(id));
        return byId;
    }

    /**
     * 启动策略
     *
     * @param id
     */
    @Override
    public void startStrategy(Long id) {
        update(new LambdaUpdateWrapper<LinkageStrategy>()
                .eq(LinkageStrategy::getId, id)
                .set(LinkageStrategy::getEnabledStatus, LinkageStrategy.Enable));
        // TODO 启动策略后续操作
    }

    /**
     * 禁用策略
     *
     * @param id 策略id
     */
    @Override
    public void stopStrategy(Long id) {
        update(new LambdaUpdateWrapper<LinkageStrategy>()
                .eq(LinkageStrategy::getId, id)
                .set(LinkageStrategy::getEnabledStatus, LinkageStrategy.Disable)
        );
        // TODO 禁用策略后续操作
    }

    @Override
    public IPage<LinkageStrategy> listPage(LinkageStrategy params) {
        IPage<LinkageStrategy> page = page(new Page<>(params.getPageNo(), params.getPageSize()));
        return page(page,
                new LambdaQueryWrapper<LinkageStrategy>()
                        .like(StringUtils.isNotEmpty(params.getStrategyName()), LinkageStrategy::getStrategyName, params.getStrategyName())
                        .like(StringUtils.isNotEmpty(params.getFrontDevice()), LinkageStrategy::getFrontDevice, params.getFrontDevice())
                        .like(StringUtils.isNotEmpty(params.getRearDevice()), LinkageStrategy::getRearDevice, params.getRearDevice())
                        .orderByDesc(LinkageStrategy::getCreateTime)
        );
    }

    /**
     * 判断点位信息是否触发联动控制
     *
     * @param deviceId 设备id
     * @param pointId  点位id
     * @param value    点位值
     */
    @Override
    public void detection(Long deviceId, Long pointId, String value) {
        List<FrontPoint> listByPointId = frontPointService.getListByPointId(pointId);
        if(listByPointId.isEmpty()){
            return;
        }
        Set<Long> linkageStrategyIds = new HashSet<>();
        for(FrontPoint point : listByPointId){
            String operator = point.getOperator();
            if(StringUtils.isEmpty(operator)){
                continue;
            }
            // 校验条件值
            try {
                if (checkConditionValue(operator,value,point.getConditionValue())) {
                    linkageStrategyIds.add(point.getLinkageStrategyId());
                }
            }catch (Exception e){
                log.error("联动控制：点位条件校验失败，前置点位id：{},设备id：{},点位id：{},值：{}",point.getId(),deviceId,pointId,value,e);
            }
        }
        List<LinkageStrategy> linkageStrategies = getDetailByIds(linkageStrategyIds);
        for(LinkageStrategy item : linkageStrategies){
            runStrategy(item);
        }
    }

    private void runStrategy(LinkageStrategy strategy){
        try{
            List<FrontPoint> frontPointList = strategy.getFrontPointList();
            for(FrontPoint point : frontPointList){
                // 获取点位值
                Long pointId = point.getPointId();
                // 获取点位值
                String value = deviceAttributeService.getValueById(pointId);
                if(StringUtils.isEmpty(value)){
                    return;
                }
                if (!checkConditionValue(point.getOperator(),value,point.getConditionValue())) {
                    return;
                }
            }
            // 执行联动控制后置设备
            List<RearPoint> rearPointList = strategy.getRearPointList();
            for(RearPoint point : rearPointList){
                // TODO  执行联动控制后置设备,点位设置
            }
        }catch (Exception e){
            log.error("联动控制：策略前置点位校验或执行失败，策略id：{}",strategy.getId(),e);
        }
    }

    private boolean checkConditionValue(String operator,String value,String conditionValue){
        if("=".equals(operator)){
            operator += "=";
        }
        return Boolean.TRUE.equals(parser.parseExpression(value + operator + conditionValue).getValue(Boolean.class));
    }

    private List<LinkageStrategy> getDetailByIds(Collection<Long> ids){
        // 获取启用的策略
        List<LinkageStrategy> linkageStrategies = super.list(
                new LambdaQueryWrapper<LinkageStrategy>()
                        .eq(LinkageStrategy::getEnabledStatus,LinkageStrategy.Enable)
                        .in(LinkageStrategy::getId,ids)
        );
        // 获取前置点位
        Map<Long,List<FrontPoint>> forePointMap = frontPointService.getListByLinkageStrategyIds(ids).stream()
                .collect(Collectors.groupingBy(FrontPoint::getLinkageStrategyId,  Collectors.toList()));
        // 获取后置点位
        Map<Long,List<RearPoint>> rearPointMap = rearPointService.getListByLinkageStrategyIds(ids).stream()
                .collect(Collectors.groupingBy(RearPoint::getLinkageStrategyId,  Collectors.toList()));
        for(LinkageStrategy item : linkageStrategies){
            item.setFrontPointList(forePointMap.getOrDefault(item.getId(), Collections.emptyList()));
            item.setRearPointList(rearPointMap.getOrDefault(item.getId(), Collections.emptyList()));
        }
        return linkageStrategies;
    }

    private void supplement(LinkageStrategy entity) {
        if(entity.getId() == null) {
            // 设置策略编码
            entity.setStrategyCode("DX" + System.currentTimeMillis());
            // 设置启用状态
            entity.setEnabledStatus(LinkageStrategy.Disable);
        }
        // 设置前置设备信息
        entity.setFrontDevice(entity.getFrontPointList().stream().map(FrontPoint::getDeviceName).collect(Collectors.joining(",")));
        // 设置后置设备信息
        entity.setRearDevice(entity.getRearPointList().stream().map(RearPoint::getDeviceName).collect(Collectors.joining(",")));
    }
}
