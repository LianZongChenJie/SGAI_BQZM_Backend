package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenter;
import org.jeecg.modules.bems.energyAnalysis.entity.EnergyPricingConfig;
import org.jeecg.modules.bems.energyAnalysis.mapper.CostCenterMapper;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterRelService;
import org.jeecg.modules.bems.energyAnalysis.service.ICostCenterService;
import org.jeecg.modules.bems.energyAnalysis.service.IEnergyPricingConfigService;
import org.jeecg.modules.bems.energyAnalysis.vo.CostAccountingVo;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CostCenterServiceImpl extends ServiceImpl<CostCenterMapper, CostCenter> implements ICostCenterService {


    @Override
    public void add(CostCenter data) {
        // 校验name和code是否重复
        if(count(new LambdaQueryWrapper<CostCenter>().eq(CostCenter::getName, data.getName())) > 0){
            throw new JeecgBootException("名称重复");
        }
        if(count(new LambdaQueryWrapper<CostCenter>().eq(CostCenter::getCode, data.getCode())) > 0){
            throw new JeecgBootException("编码重复");
        }
        if(data.getParentId() == null){
            data.setParentId(CostCenter.ROOT_ID);
        }
        save(data);
    }

    @Override
    public void update(CostCenter data) {
        // 校验name和code是否重复
        if(count(new LambdaQueryWrapper<CostCenter>().eq(CostCenter::getName, data.getName()).ne(CostCenter::getId, data.getId())) > 0){
            throw new JeecgBootException("名称重复");
        }
        if(count(new LambdaQueryWrapper<CostCenter>().eq(CostCenter::getCode, data.getCode()).ne(CostCenter::getId, data.getId())) > 0){
            throw new JeecgBootException("编码重复");
        }
        updateById(data);
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<CostCenter> getTree() {
        Map<Long,List<CostCenter>> listMap = list()
                .stream()
                .sorted(Comparator.comparing(CostCenter::getSort))
                .collect(Collectors.groupingBy(CostCenter::getParentId));
        listMap.values().forEach(v -> {
            for (CostCenter item : v) {
                item.setChildren(listMap.getOrDefault(item.getId(), new ArrayList<>()));
            }
        });
        return listMap.getOrDefault(CostCenter.ROOT_ID, Collections.emptyList());
    }

    @Override
    public Page<CostCenter> listByParentId(CostCenter param) {
        return page(new Page<>(param.getPageNo(), param.getPageSize()),
                new LambdaQueryWrapper<CostCenter>()
                        .and(param.getParentId() != null,
                                i -> i.eq(CostCenter::getParentId, param.getParentId())
                                        .or(j -> j.eq(CostCenter::getId, param.getParentId())))
                        .orderByAsc(CostCenter::getSort)
        );
    }

}
