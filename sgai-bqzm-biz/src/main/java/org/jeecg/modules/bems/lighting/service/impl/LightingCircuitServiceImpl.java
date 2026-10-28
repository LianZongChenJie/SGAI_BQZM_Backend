package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
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
            // 开启总时长按实时计算（只改出参不落库）：
            // - 开启中：累计值 + 当前开启周期已运行时长
            // - 已关闭：显示累计值；累计值为空且本周期时长可算时，兜底显示本次开关周期时长（避免历史数据缺失显示0）
            fillDisplayDuration(record, LocalDateTime.now());
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
        LightingCircuit circuit = getOneCircuit(area.getId(), circuitCode);
        if(circuit == null){
            return;
        }
        // 状态未变：正常跳过，但要顺手指掉历史脏数据（开启态却残留关闭时间，否则永远不会被清理）
        if(status.equals(circuit.getStatus())){
            clearClosingTimeIfOn(circuit, status, circuit.getClosingTime() != null);
            return;
        }
        // 更新数据（含开启/关闭时间、开启总时长，幂等累计）
        applyStatus(circuit, status);
        // 更新场景开启时长
        areaService.update(new LambdaUpdateWrapper<LightingArea>()
                .eq(LightingArea::getId,area.getId())
                .lt(LightingArea::getAllDuration,circuit.getAllDuration())
                .set(LightingArea::getAllDuration,circuit.getAllDuration())
        );
    }

    /**
     * 记录回路控制操作人/操作时间（区域全开/全关等批量控制调用，不改变回路状态与开关时间）
     */
    @Override
    public void recordControlOperator(LightingCircuit circuit, String operatorBy) {
        if (circuit == null || circuit.getId() == null) {
            return;
        }
        // 只定向更新操作人/操作时间：整实体 updateById 会把调用方读取时的旧快照（状态、开关时间）一起回写，
        // 批量控制循环里若期间收到 MQ 状态回传，就会把新状态覆盖回去
        super.update(new LambdaUpdateWrapper<LightingCircuit>()
                .eq(LightingCircuit::getId, circuit.getId())
                .set(LightingCircuit::getOperatorBy,
                        operatorBy != null && !operatorBy.isEmpty() ? operatorBy : currentOperator())
                .set(LightingCircuit::getOperatorTime, LocalDateTime.now()));
    }

    /**
     * 回路开启总时长（出参，不落库）：
     * - 开启中：allDuration 累计值 + 从 startTime 到当前时间的时长
     * - 已关闭：allDuration 累计值；若累计值为空/0 且 startTime、closingTime 齐全，则兜底显示本次开关周期时长
     */
    private void fillDisplayDuration(LightingCircuit circuit, LocalDateTime now) {
        Long duration = circuit.getAllDuration();
        if (duration == null || duration < 0) {
            duration = 0L;
        }
        LocalDateTime start = circuit.getStartTime();
        if (start == null) {
            circuit.setAllDuration(duration);
            return;
        }
        // 历史脏数据兜底（出参不落库）：关闭时间早于开启时间说明是上一周期残留值，不再展示
        if (circuit.getClosingTime() != null && circuit.getClosingTime().isBefore(start)) {
            circuit.setClosingTime(null);
        }
        if (LightingCircuit.STATUS_ON.equals(circuit.getStatus())) {
            // 开启中：实时累加当前周期
            if (!start.isAfter(now)) {
                duration += LocalDateTimeUtil.between(start, now).getSeconds();
            }
        } else if (duration == 0L && circuit.getClosingTime() != null && !circuit.getClosingTime().isBefore(start)) {
            // 已关闭且累计值为空：兜底显示本次开关周期时长
            duration = LocalDateTimeUtil.between(start, circuit.getClosingTime()).getSeconds();
        }
        circuit.setAllDuration(duration);
    }

    /**
     * 更新回路状态并维护开启/关闭时间、开启总时长（设备 MQ 回传状态时调用，是状态唯一写入源）
     * 幂等规则：
     * - 开启：仅当未记录开启时间或当前处于关闭状态时才刷新开启时间，避免周期状态上报刷新导致时长失真
     * - 关闭：本周期已结算（closing_time 非空）时不再处理，重复关闭消息不会重复累计时长、也不会刷新关闭时间
     */
    @Override
    public void applyStatus(LightingCircuit circuit, String status) {
        if (circuit == null || circuit.getId() == null) {
            return;
        }
        applyStatusFields(circuit, status, LocalDateTime.now());
        persistStatusFields(circuit);
    }

    /**
     * 只落库状态相关的 4 个字段（status / start_time / closing_time / all_duration）。
     * <p>
     * 不用整实体 updateById：它会把调用方查库时的旧快照里的所有非 null 字段（electric_current、
     * comstat、operator_by 等）一起回写，并发链路（如 DataType=1 电流消息、Web 控制）刚写入的新值
     * 会被旧快照覆盖。2026-10-28 与 recordControlOperator 的修复属同一类问题。
     * <p>
     * closing_time 为 null 时必须用 setSql 写字面量：常规 .set(...) 传 null 在达梦驱动上易报
     * "无效的列类型"；updateById 则因 NOT_NULL 策略直接丢弃该字段。
     */
    private void persistStatusFields(LightingCircuit circuit) {
        LambdaUpdateWrapper<LightingCircuit> wrapper = new LambdaUpdateWrapper<LightingCircuit>()
                .eq(LightingCircuit::getId, circuit.getId())
                .set(LightingCircuit::getStatus, circuit.getStatus());
        if (circuit.getStartTime() != null) {
            wrapper.set(LightingCircuit::getStartTime, circuit.getStartTime());
        }
        if (circuit.getClosingTime() == null) {
            wrapper.setSql("closing_time = null");
        } else {
            wrapper.set(LightingCircuit::getClosingTime, circuit.getClosingTime());
        }
        if (circuit.getAllDuration() != null) {
            wrapper.set(LightingCircuit::getAllDuration, circuit.getAllDuration());
        }
        super.update(wrapper);
    }

    /**
     * 开启时显式清空 closing_time（仅用于 mqControl 的"状态未变"分支；
     * 状态变化时由 {@link #persistStatusFields} 在同一条 UPDATE 里一并处理）。
     * updateById 走 MyBatis-Plus 默认 NOT_NULL 更新策略，null 字段不会进入 UPDATE 语句，
     * applyStatusFields 里 setClosingTime(null) 实际是空操作，会导致关闭时间残留上一周期的值
     * （表现为"关闭时间早于开启时间"，且该回路之后再也不更新关闭时间、不累加时长）。
     * <p>
     * 这里用 setSql 硬拼 NULL，而不是 .set(getClosingTime, null)：后者会以参数绑定方式传 null，
     * 达梦驱动对 jdbcType=OTHER 的 null 容易报"无效的列类型"，走字面量最稳。
     *
     * @param hadClosingTime 进入本次处理前 closing_time 是否非空；只有非空才需要真的发一条 UPDATE
     */
    private void clearClosingTimeIfOn(LightingCircuit circuit, String status, boolean hadClosingTime) {
        if (hadClosingTime && LightingCircuit.STATUS_ON.equals(status) && circuit.getId() != null) {
            super.update(new LambdaUpdateWrapper<LightingCircuit>()
                    .eq(LightingCircuit::getId, circuit.getId())
                    .setSql("closing_time = null"));
        }
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
            // 本周期已结算过（closing_time 非空）就不再处理：
            // 避免设备重复/周期上报"关闭"时把关闭时间一直往后推、开启时长被重复累计
            if (circuit.getClosingTime() == null) {
                LocalDateTime endTime = time;
                if (circuit.getStartTime() != null) {
                    // 消息乱序/延迟兜底：关闭时刻早于开启时刻时按开启时刻结算，避免累加出负时长
                    if (endTime.isBefore(circuit.getStartTime())) {
                        endTime = circuit.getStartTime();
                    }
                    // 计算本周期开启时长并累计
                    long seconds = LocalDateTimeUtil.between(circuit.getStartTime(), endTime).getSeconds();
                    Long allDuration = circuit.getAllDuration();
                    if (allDuration == null) {
                        allDuration = 0L;
                    }
                    circuit.setAllDuration(allDuration + seconds);
                }
                circuit.setClosingTime(endTime);
            }
        }
    }

    /**
     * 控制指令发出后只记录回路的操作人、操作时间：
     * 状态、开启时间、关闭时间、开启总时长一律不改，完全以设备回传为准（见 applyStatus）。
     * 指令是否真正执行、何时执行，由 MQ 回传状态时落库；设备不回传则保持原状态。
     */
    private void updateControlInfo(LightingCircuit circuit) {
        // 只定向更新操作人/操作时间，避免整实体回写把期间收到的 MQ 状态/开关时间覆盖回旧快照
        super.update(new LambdaUpdateWrapper<LightingCircuit>()
                .eq(LightingCircuit::getId, circuit.getId())
                .set(LightingCircuit::getOperatorBy, currentOperator())
                .set(LightingCircuit::getOperatorTime, LocalDateTime.now()));
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
     * 按 area_id + circuit_code 查唯一回路。
     * lighting_circuit 无主键/唯一索引，出现重复行时 getOne 会抛 TooManyResultsException，
     * 使整条 MQ 状态消息被丢弃；此处改为取第一条并告警（已知脏数据：id=1065 两条重复）。
     */
    private LightingCircuit getOneCircuit(Long areaId, String circuitCode) {
        List<LightingCircuit> list = super.list(new LambdaQueryWrapper<LightingCircuit>()
                .eq(LightingCircuit::getAreaId, areaId)
                .eq(LightingCircuit::getCircuitCode, circuitCode));
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            log.warn("回路存在重复数据，取第一条：area_id={}, circuit_code={}, 匹配条数={}, 取用id={}",
                    areaId, circuitCode, list.size(), list.get(0).getId());
        }
        return list.get(0);
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
        LightingCircuit circuit = getOneCircuit(area.getId(), circuitCode);
        if(circuit == null){
            return;
        }
        circuit.setComstat(comstat);
        // 只定向更新通讯状态：整实体 updateById 会把旧快照里的状态/开关时间/电流一起回写
        super.update(new LambdaUpdateWrapper<LightingCircuit>()
                .eq(LightingCircuit::getId, circuit.getId())
                .set(LightingCircuit::getComstat, comstat));
    }

    @Override
    public void updateElectricCurrent(Long id, Double electricCurrent) {
        if (id == null || electricCurrent == null) {
            return;
        }
        // 只定向更新电流：DataType=1 消息极频繁，整实体回写会覆盖并发写入的状态/开关时间
        super.update(new LambdaUpdateWrapper<LightingCircuit>()
                .eq(LightingCircuit::getId, id)
                .set(LightingCircuit::getElectricCurrent, electricCurrent));
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

        // 904（新灯控）走新的MQ转发通道
        if("904".equals(area.getSpace())){
            control904(data, area, type, parentId);
            return;
        }

        // 四高炉（space=901）走电箱控制小程序通道，回路编码作为 operRd 发送
        if("901".equals(area.getSpace())){
            controlSgf(data, area, type, parentId);
            return;
        }

        if(type){
            service.circuitOpen(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId, LightingOperationLog.REL_TYPE_CIRCUIT, id, area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(), "回路开启");
        }else {
            service.circuitClose(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId, LightingOperationLog.REL_TYPE_CIRCUIT, id, area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(), "回路关闭");
        }
        // 只记录操作人/操作时间（状态与开关时间以设备回传为准）
        updateControlInfo(data);
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

        // 只记录操作人/操作时间（状态与开关时间以设备回传为准）
        updateControlInfo(circuit);
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

        // 只记录操作人/操作时间（状态与开关时间以设备回传为准）
        updateControlInfo(circuit);
    }

    /**
     * 904（新灯控）回路控制（走MQ转发小程序通道）
     * 消息：DataType=0，带 AreaID 和 CircuitCode，GatewayCode固定54，发到 lighting_control_bq_54
     */
    private void control904(LightingCircuit circuit, LightingArea area, boolean type, Long parentId){
        String circuitCode = circuit.getCircuitCode();
        if(circuitCode == null || circuitCode.isEmpty()){
            throw new JeecgBootException("回路编码为空，无法控制");
        }

        String value = type ? "100" : "0";
        String operName = type ? "回路开启" : "回路关闭";

        log.info("【904】回路控制：circuitName={}, 操作={}, circuitCode={}",
                circuit.getCircuitName(), operName, circuitCode);

        sendService.send904Control(area.getAreaCode(), circuitCode, value);

        lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId,
                LightingOperationLog.REL_TYPE_CIRCUIT, circuit.getId(),
                area.getAreaName() + "-" + circuit.getCircuitName(),
                LocalDateTime.now(), operName);

        // 只记录操作人/操作时间（状态与开关时间以设备回传为准）
        updateControlInfo(circuit);
    }

    /**
     * 四高炉（space=901）回路控制（走电箱控制小程序通道）
     * 区域即电箱，area_code 即 deviceSn；把本回路编码 circuit_code 作为 operRd 发送，与 902/903 逻辑一致
     */
    private void controlSgf(LightingCircuit circuit, LightingArea area, boolean type, Long parentId){
        String circuitCode = circuit.getCircuitCode();
        if(circuitCode == null || circuitCode.isEmpty()){
            throw new JeecgBootException("回路编码为空，无法控制");
        }
        String areaCode = area.getAreaCode();
        if(StringUtils.isEmpty(areaCode)){
            throw new JeecgBootException("区域编码为空，无法控制");
        }

        String oper = type ? "1" : "0";
        String operName = type ? "回路开启" : "回路关闭";

        log.info("【四高炉】回路控制：circuitName={}, 操作={}, deviceSn={}, circuitCode={}",
                circuit.getCircuitName(), operName, areaCode, circuitCode);

        sendService.sendSgfControl(areaCode, circuitCode, oper);

        lightingOperationLogService.saveLog(LightingOperationLog.LOG_TYPE_CIRCUIT, parentId,
                LightingOperationLog.REL_TYPE_CIRCUIT, circuit.getId(),
                area.getAreaName() + "-" + circuit.getCircuitName(),
                LocalDateTime.now(), operName);

        // 只记录操作人/操作时间（状态与开关时间以设备回传为准）
        updateControlInfo(circuit);
    }
}
