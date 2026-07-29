package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingCircuitQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.CircuitMapper;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LightingCircuitServiceImpl extends ServiceImpl<CircuitMapper, LightingCircuit> implements ILightingCircuitService {
    private final LightingService service;

    private final ILightingAreaService areaService;

    private final ILightingOperationLogService lightingOperationLogService;

    @Override
    public IPage<LightingCircuit> listPage(LightingCircuitQueryDto params) {
        Page<LightingCircuit> page = super.page(new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<LightingCircuit>()
                        .eq(params.getAreaId() != null, LightingCircuit::getAreaId, params.getAreaId()));
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
        control(id,true);
    }

    @Override
    @Transactional
    public void close(Long id) {
        control(id,false);
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
        // 更新数据
        circuit.setStatus(status);
        if(status.equals("开启")){
            circuit.setStartTime(LocalDateTime.now());
            circuit.setClosingTime(null);
        }else{
            circuit.setClosingTime(LocalDateTime.now());
            if(circuit.getStartTime() != null){
                // 计算开启时长
                long seconds = LocalDateTimeUtil.between(circuit.getStartTime(), circuit.getClosingTime()).getSeconds();
                Long allDuration = circuit.getAllDuration();
                if(allDuration == null){
                    allDuration = 0L;
                }
                circuit.setAllDuration(allDuration + seconds);
            }
        }
        super.updateById(circuit);
        // 更新场景开启时长
        areaService.update(new LambdaUpdateWrapper<LightingArea>()
                .eq(LightingArea::getId,area.getId())
                .lt(LightingArea::getAllDuration,circuit.getAllDuration())
                .set(LightingArea::getAllDuration,circuit.getAllDuration())
        );
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

    private void control(Long id,boolean type){
        LightingCircuit data = super.getById(id);
        if(data == null){
            throw new JeecgBootException("回路不存在");
        }
        LightingArea area = areaService.getById(data.getAreaId());
        if(area == null){
            throw new JeecgBootException("回路所属区域不存在");
        }
        if(type){
            service.circuitOpen(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.REL_TYPE_CIRCUIT,id,area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(),"回路开启");
        }else {
            service.circuitClose(area.getSpace(),area.getAreaCode(),data.getCircuitCode());
            lightingOperationLogService.saveLog(LightingOperationLog.REL_TYPE_CIRCUIT,id,area.getAreaName() + "-" + data.getCircuitName(), LocalDateTime.now(),"回路关闭");
        }
    }
}
