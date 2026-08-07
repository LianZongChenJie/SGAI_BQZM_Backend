package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.LightingAreaMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
public class LightingAreaServiceImpl extends ServiceImpl<LightingAreaMapper, LightingArea> implements ILightingAreaService {

    private final LightingService service;

    private final ILightingOperationLogService lightingOperationLogService;

    private final ILightingCircuitService circuitService;

    private final LightingSendService sendService;

    private final ILightingDistrictService districtService;

    /**
     * 注意：circuitService 与 LightingCircuitServiceImpl.areaService 存在循环依赖，
     * 必须在构造参数上用 @Lazy 打破（Lombok 的 @AllArgsConstructor 不会把字段上的 @Lazy 复制到构造参数上）
     */
    @Autowired
    public LightingAreaServiceImpl(LightingService service,
                                   ILightingOperationLogService lightingOperationLogService,
                                   @Lazy ILightingCircuitService circuitService,
                                   LightingSendService sendService,
                                   ILightingDistrictService districtService) {
        this.service = service;
        this.lightingOperationLogService = lightingOperationLogService;
        this.circuitService = circuitService;
        this.sendService = sendService;
        this.districtService = districtService;
    }

    @Override
    public IPage<LightingArea> listPage(LightingAreaQueryDto params) {
        IPage<LightingArea> page = listPage1(params);
        if(CollectionUtil.isEmpty(page.getRecords())){
            return page;
        }
        List<LightingArea> records = page.getRecords();
        for(LightingArea area : records){
            area.setAreaName(area.getSpaceName() + area.getAreaName());
        }
        return page;
    }

    @Override
    public IPage<LightingArea> listPage1(LightingAreaQueryDto params) {
        LambdaQueryWrapper<LightingArea> queryWrapper = new LambdaQueryWrapper<LightingArea>()
                .like(StringUtils.isNotEmpty(params.getRelName()),LightingArea::getRelName, params.getRelName())
                .eq(StringUtils.isNotEmpty(params.getSpace()),LightingArea::getSpace,params.getSpace())
                .eq(params.getDistrictId() != null,LightingArea::getDistrictId,params.getDistrictId())
                .like(StringUtils.isNotEmpty(params.getAreaName()),LightingArea::getAreaName, params.getAreaName())
                .orderByAsc(LightingArea::getSort);
        IPage<LightingArea> page = super.page(new Page<>(params.getPageNo(), params.getPageSize()),queryWrapper);
        fillDistrictName(page.getRecords());
        return page;
    }

    @Override
    public List<LightingArea> list() {
        List<LightingArea> list = super.list();
        fillDistrictName(list);
        return list;
    }

    @Override
    public List<LightingArea> list(Wrapper<LightingArea> queryWrapper) {
        List<LightingArea> list = super.list(queryWrapper);
        fillDistrictName(list);
        return list;
    }

    @Override
    public LightingArea getById(java.io.Serializable id) {
        LightingArea area = super.getById(id);
        if(area != null){
            fillDistrictName(Collections.singletonList(area));
        }
        return area;
    }

    /**
     * 区域-全开
     * @param id 区域id
     */
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

    /**
     * 区域-全关
     * @param id 区域id
     */
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

    /**
     * mq-状态监听
     * @param areaCode 区域编码
     */
    @Override
    public void mqControl(String space,String areaCode,String value) {
        LightingArea area = super.getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace, space).eq(LightingArea::getAreaCode, areaCode));
        if(area == null){
            return;
        }
        // 判断状态
        String status = "";
        if(value.equals(area.getOpenCode())){
            status = "开启";
        }else if(value.equals(area.getCloseCode())){
            status = "关闭";
        }else {
            log.error("照明区域场景状态错误。space: {},areaCode: {},value: {}",space,areaCode,value);
            return;
        }

        super.update(new LambdaUpdateWrapper<LightingArea>().eq(LightingArea::getId,area.getId()).set(LightingArea::getStatus,status));
    }

    @Override
    public LightingArea getByCode(String space,String areaCode) {
        return getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace,space).eq(LightingArea::getAreaCode,areaCode));
    }

    @Override
    public List<LightingArea> getByIds(Collection<Long> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return Collections.emptyList();
        }
        List<LightingArea> list = super.list(new LambdaQueryWrapper<LightingArea>().in(LightingArea::getId,ids));
        fillDistrictName(list);
        return list;
    }

    /**
     * 回填片区名称
     */
    private void fillDistrictName(List<LightingArea> areas) {
        if(CollectionUtil.isEmpty(areas)){
            return;
        }
        Set<Long> districtIds = areas.stream()
                .map(LightingArea::getDistrictId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if(districtIds.isEmpty()){
            return;
        }
        Map<Long, String> nameMap = districtService.listByIds(districtIds).stream()
                .collect(Collectors.toMap(LightingDistrict::getId, LightingDistrict::getDistrictName, (a, b) -> a));
        for(LightingArea area : areas){
            if(area.getDistrictId() != null){
                area.setDistrictName(nameMap.get(area.getDistrictId()));
            }
        }
    }

    private void control(Long id, boolean type, Long parentId){
        LightingArea area = super.getById(id);
        if(area == null){
            throw new JeecgBootException("区域不存在");
        }

        // 1号馆（space=902）走新的MQ转发通道，不走老的KNX
        if("902".equals(area.getSpace())){
            control1hg(area, type);
        } else if("903".equals(area.getSpace())){
            // 北区（space=903）走新的MQ转发通道
            controlBq(area, type);
        } else {
            if(type) {
                service.areaOpen(area.getSpace(),area.getAreaCode(),area.getOpenCode());
            }else {
                service.areaClose(area.getSpace(),area.getAreaCode(),area.getCloseCode());
            }
        }

        // 记录区域操作日志（logType=区域）
        String operationType = type ? "区域全开" : "区域全关";
        LightingOperationLog areaLog = new LightingOperationLog();
        areaLog.setLogType(LightingOperationLog.LOG_TYPE_AREA);
        areaLog.setParentId(parentId);
        areaLog.setRelType(LightingOperationLog.REL_TYPE_AREA);
        areaLog.setRelId(id);
        areaLog.setName(area.getAreaName());
        areaLog.setOperationTime(LocalDateTime.now());
        areaLog.setOperationType(operationType);
        String operatorType = lightingOperationLogService.resolveOperatorType(parentId);
        // 设置操作人
        String operationBy = "照明计划";
        try {
            org.jeecg.common.system.vo.LoginUser sysUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (sysUser != null) {
                operationBy = sysUser.getUsername();
            }
        } catch (Exception e) {
            // 异步场景中SecurityManager不可用，使用默认用户
        }
        areaLog.setOperationBy(operationBy);
        areaLog.setOperatorType(operatorType);
        lightingOperationLogService.save(areaLog);

        // 查询区域下所有回路，记录回路子日志
        List<LightingCircuit> circuitList = circuitService.list(
                new LambdaQueryWrapper<LightingCircuit>().eq(LightingCircuit::getAreaId, id));
        if (CollectionUtil.isNotEmpty(circuitList)) {
            List<LightingOperationLog> childLogs = new java.util.ArrayList<>();
            String circuitOpType = type ? "回路开启" : "回路关闭";
            for (LightingCircuit circuit : circuitList) {
                LightingOperationLog childLog = new LightingOperationLog();
                childLog.setLogType(LightingOperationLog.LOG_TYPE_CIRCUIT);
                childLog.setParentId(areaLog.getId());
                childLog.setRelType(LightingOperationLog.REL_TYPE_CIRCUIT);
                childLog.setRelId(circuit.getId());
                childLog.setName(area.getAreaName() + "-" + circuit.getCircuitName());
                childLog.setOperationTime(LocalDateTime.now());
                childLog.setOperationType(circuitOpType);
                childLog.setOperationBy(operationBy);
                childLog.setOperatorType(operatorType);
                childLogs.add(childLog);
            }
            lightingOperationLogService.saveBatchLog(childLogs);
        }
    }

    /**
     * 1号馆区域控制（走MQ转发小程序通道）
     * 遍历区域下所有回路，逐个发送控制消息
     */
    private void control1hg(LightingArea area, boolean type){
        // 查询区域下所有回路
        LambdaQueryWrapper<LightingCircuit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightingCircuit::getAreaId, area.getId());
        java.util.List<LightingCircuit> circuitList = circuitService.list(wrapper);

        if(CollectionUtil.isEmpty(circuitList)){
            log.warn("【1号馆】区域下没有回路，areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }

        String value = type ? "1" : "0";
        log.info("【1号馆】区域控制：areaName={}, 操作={}, 回路数={}", area.getAreaName(), type ? "全开" : "全关", circuitList.size());

        for(LightingCircuit circuit : circuitList){
            try {
                String circuitCode = circuit.getCircuitCode();
                if(StringUtils.isEmpty(circuitCode)){
                    continue;
                }
                // 拆分 circuit_code：第一个 "-" 前面是 GatewayAdr，后面是 KnxAdr
                int dashIndex = circuitCode.indexOf('-');
                if(dashIndex <= 0 || dashIndex >= circuitCode.length() - 1){
                    log.warn("【1号馆】回路编码格式不对，跳过：circuitCode={}", circuitCode);
                    continue;
                }
                String gatewayAdr = circuitCode.substring(0, dashIndex);
                String knxAdr = circuitCode.substring(dashIndex + 1);

                sendService.send1hgControl(gatewayAdr, knxAdr, value);
            } catch (Exception e){
                log.error("【1号馆】发送回路控制消息失败：circuitId={}, circuitName={}", circuit.getId(), circuit.getCircuitName(), e);
            }
        }

        log.info("【1号馆】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }

    /**
     * 北区（space=903）区域控制（走MQ转发小程序通道）
     * 遍历区域下所有回路，逐个发送控制消息
     */
    private void controlBq(LightingArea area, boolean type){
        // 查询区域下所有回路
        LambdaQueryWrapper<LightingCircuit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightingCircuit::getAreaId, area.getId());
        java.util.List<LightingCircuit> circuitList = circuitService.list(wrapper);

        if(CollectionUtil.isEmpty(circuitList)){
            log.warn("【北区】区域下没有回路，areaId={}, areaName={}", area.getId(), area.getAreaName());
            return;
        }

        String value = type ? "100" : "0";
        log.info("【北区】区域控制：areaName={}, 操作={}, 回路数={}", area.getAreaName(), type ? "全开" : "全关", circuitList.size());

        for(LightingCircuit circuit : circuitList){
            try {
                String circuitCode = circuit.getCircuitCode();
                if(StringUtils.isEmpty(circuitCode)){
                    continue;
                }

                sendService.sendBqControl(circuitCode, value);
            } catch (Exception e){
                log.error("【北区】发送回路控制消息失败：circuitId={}, circuitName={}", circuit.getId(), circuit.getCircuitName(), e);
            }
        }

        log.info("【北区】区域控制完成：areaName={}, 操作={}", area.getAreaName(), type ? "全开" : "全关");
    }
}
