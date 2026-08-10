package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.CircuitMapper;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class LightingCircuitServiceImpl extends ServiceImpl<CircuitMapper, LightingCircuit> implements ILightingCircuitService {
    private final LightingService service;

    private final ILightingAreaService areaService;

    private final ILightingOperationLogService lightingOperationLogService;

    private final LightingSendService sendService;

    @Override
    public IPage<LightingCircuit> listPage(LightingCircuitQueryDto params) {
        LambdaQueryWrapper<LightingCircuit> queryWrapper = new LambdaQueryWrapper<LightingCircuit>()
                .eq(params.getAreaId() != null, LightingCircuit::getAreaId, params.getAreaId());
        // 按片区筛选：先查出该片区下的区域ID集合，再过滤回路
        if(params.getDistrictId() != null){
            List<Long> districtAreaIds = areaService.list(new LambdaQueryWrapper<LightingArea>()
                            .eq(LightingArea::getDistrictId, params.getDistrictId()))
                    .stream()
                    .map(LightingArea::getId)
                    .collect(Collectors.toList());
            if(districtAreaIds.isEmpty()){
                return new Page<>(params.getPageNo(), params.getPageSize());
            }
            queryWrapper.in(LightingCircuit::getAreaId, districtAreaIds);
        }
        Page<LightingCircuit> page = super.page(new Page<>(params.getPageNo(), params.getPageSize()), queryWrapper);
        List<LightingCircuit> records = page.getRecords();
        Set<Long> areaIds = records.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet());
        Map<Long,String> areaMap = areaService.getByIds(areaIds)
                .stream()
                .collect(Collectors.toMap(LightingArea::getId, LightingArea::getAreaName));
        for (LightingCircuit record : records) {
            record.setAreaName(areaMap.get(record.getAreaId()));
        }
        return page;
    }

    @Override
    public List<LightingCircuit> list(){
        List<LightingCircuit> list = super.list();
        Set<Long> areaIds = list.stream().map(LightingCircuit::getAreaId).collect(Collectors.toSet());
        Map<Long,String> areaMap = areaService.getByIds(areaIds)
                .stream()
                .collect(Collectors.toMap(LightingArea::getId, LightingArea::getAreaName));
        for (LightingCircuit record : list) {
            record.setAreaName(areaMap.get(record.getAreaId()));
        }
        return list;
    }

    @Override
    @Transactional
    public void open(Long id) {
        control(id, true, null);
    }

    @Override
    @Transactional
    public void open(Long id, Long parentId) {
        control(id, true, parentId);
    }

    @Override
    @Transactional
    public void close(Long id) {
        control(id, false, null);
    }

    @Override
    @Transactional
    public void close(Long id, Long parentId) {
        control(id, false, parentId);
    }

    @Override
    public void mqControl(String space,String areaCode, String circuitCode, String status) {
        // 获取回路信息
        LightingArea area = areaService.getByCode(space,areaCode);
        if(area == null){
            return;
        }
        LightingCircuit circuit = super.getOne(new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId,area.getId()).eq(LightingCircuit::getCircuitCode,circuitCode));
        if(circuit == null || status.equals(circuit.getStatus())){
            return;
        }
        // 更新数据（含开启/关闭时间、开启总时长，幂等累计）
        applyStatusFields(circuit, status, LocalDateTime.now());
        super.updateById(circuit);
        // 更新场景开启时长
        areaService.update(new LambdaUpdateWrapper<LightingArea>()
                .eq(LightingArea::getId,area.getId())
                .lt(LightingArea::getAllDuration,circuit.getAllDuration())
                .set(LightingArea::getAllDuration,circuit.getAllDuration())
        );
    }

    /**
     * 更新回路状态并维护开启/关闭时间、开启总时长
     * 幂等规则：
     * - 开启：仅当未记录开启时间或当前处于关闭状态时才刷新开启时间，避免周期状态上报刷新导致时长失真
     * - 关闭：仅当当前未记录关闭时间（即本周期未结算过）时才累计时长，重复关闭消息不会重复累计
     */
    @Override
    public void applyStatus(LightingCircuit circuit, String status) {
        applyStatusFields(circuit, status, LocalDateTime.now());
        super.updateById(circuit);
    }

    /**
     * 根据开关状态维护回路的开启时间、关闭时间、开启总时长（不落库，由调用方保存）
     */
    private void applyStatusFields(LightingCircuit circuit, String status, LocalDateTime time) {
        circuit.setStatus(status);
        if (LightingCircuit.STATUS_ON.equals(status)) {
            if (circuit.getStartTime() == null || circuit.getClosingTime() != null) {
                circuit.setStartTime(time);
            }
            circuit.setClosingTime(null);
        } else if (LightingCircuit.STATUS_OFF.equals(status)) {
            if (circuit.getClosingTime() == null && circuit.getStartTime() != null) {
                // 计算本周期开启时长并累计
                long seconds = LocalDateTimeUtil.between(circuit.getStartTime(), time).getSeconds();
                Long allDuration = circuit.getAllDuration();
                if (allDuration == null) {
                    allDuration = 0L;
                }
                circuit.setAllDuration(allDuration + seconds);
            }
            circuit.setClosingTime(time);
        }
    }

    /**
     * 控制指令发出后乐观更新回路的操作人、操作时间及开关时间
     * （不直接改状态，状态以设备回传为准；重复关闭不重复累计时长）
     */
    private void updateControlInfo(LightingCircuit circuit, boolean type) {
        applyStatusFields(circuit, type ? LightingCircuit.STATUS_ON : LightingCircuit.STATUS_OFF, LocalDateTime.now());
        circuit.setOperatorBy(currentOperator());
        circuit.setOperatorTime(LocalDateTime.now());
        super.updateById(circuit);
    }

    /**
     * 获取当前登录用户，异步/定时场景无登录上下文时使用默认值
     */
    private String currentOperator() {
        try {
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                return sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景（如MQ监听器、定时任务）中SecurityManager不可用
        }
        return "照明计划";
    }

    /**
     * 更新通讯状态
     */
    @Override
    public void updateComstat(String space, String areaCode, String circuitCode, String comstat) {
        // 获取回路信息
        LightingArea area = areaService.getByCode(space,areaCode);
        if(area == null){
            return;
        }
        LightingCircuit circuit = super.getOne(new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId,area.getId()).eq(LightingCircuit::getCircuitCode,circuitCode));
        if(circuit == null){
            return;
        }
        circuit.setComstat(comstat);
        super.updateById(circuit);
    }

    private void control(Long id, boolean type, Long parentId){
        LightingCircuit data = super.getById(id);
        if(data == null){
            throw new JeecgBootException("回路不存在");
        }
        LightingArea area = areaService.getById(data.getAreaId());
        if(area == null){
            throw new JeecgBootException("回路所属区域不存在");
        }

        // 1号馆（space=902）走新的MQ转发通道，不走老的KNX
        if("902".equals(area.getSpace())){
            control1hg(data, area, type, parentId);
            return;
        }

        // 北区（space=903）走新的MQ转发通道
        if("903".equals(area.getSpace())){
            controlBq(data, area, type, parentId);
            return;
        }

        if(type){
            service.circuitOpen(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId, LightingOperationLog.REL_TYPE_CIRCUIT, id, area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(), "回路开启");
        }else {
            service.circuitClose(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId, LightingOperationLog.REL_TYPE_CIRCUIT, id, area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(), "回路关闭");
        }
        // 乐观更新操作人/操作时间及开关时间
        updateControlInfo(data, type);
    }

    /**
     * 1号馆回路控制（走MQ转发小程序通道）
     */
    private void control1hg(LightingCircuit circuit, LightingArea area, boolean type, Long parentId){
        String circuitCode = circuit.getCircuitCode();
        if(circuitCode == null || circuitCode.isEmpty()){
            throw new JeecgBootException("回路编码为空，无法控制");
        }

        String value = type ? "100" : "0";
        String operName = type ? "回路开启" : "回路关闭";

        log.info("【1号馆】回路控制：circuitName={}, 操作={}, circuitCode={}",
                circuit.getCircuitName(), operName, circuitCode);

        sendService.send1hgControl(circuitCode, value);

        lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId,
                LightingOperationLog.REL_TYPE_CIRCUIT, circuit.getId(),
                area.getAreaName() + "-" + circuit.getCircuitName(),
                LocalDateTime.now(), operName);

        // 乐观更新操作人/操作时间及开关时间
        updateControlInfo(circuit, type);
    }

    /**
     * 北区（space=903）回路控制（走MQ转发小程序通道）
     */
    private void controlBq(LightingCircuit circuit, LightingArea area, boolean type, Long parentId){
        String circuitCode = circuit.getCircuitCode();
        if(circuitCode == null || circuitCode.isEmpty()){
            throw new JeecgBootException("回路编码为空，无法控制");
        }

        String value = type ? "100" : "0";
        String operName = type ? "回路开启" : "回路关闭";

        log.info("【北区】回路控制：circuitName={}, 操作={}, circuitCode={}",
                circuit.getCircuitName(), operName, circuitCode);

        sendService.sendBqControl(circuitCode, value);

        lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId,
                LightingOperationLog.REL_TYPE_CIRCUIT, circuit.getId(),
                area.getAreaName() + "-" + circuit.getCircuitName(),
                LocalDateTime.now(), operName);

        // 乐观更新操作人/操作时间及开关时间
        updateControlInfo(circuit, type);
    }
}
