package org.jeecg.modules.bems.energyAnalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointRel;
import org.jeecg.modules.bems.energyAnalysis.mapper.MeteringPointRelMapper;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointRelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeteringPointRelServiceImpl extends ServiceImpl<MeteringPointRelMapper, MeteringPointRel> implements IMeteringPointRelService {
    @Override
    public List<MeteringPointRel> findByTypeAndRelId(String type, Long relId) {
        return list(new LambdaQueryWrapper<MeteringPointRel>()
                .eq(MeteringPointRel::getRelType, type)
                .eq(MeteringPointRel::getRelId, relId)
        );
    }

    @Transactional
    @Override
    public void updateRel(Long pointId, Collection<Long> deviceIds, Collection<Long> pointIds) {
        remove(new LambdaQueryWrapper<MeteringPointRel>().eq(MeteringPointRel::getMeteringPointId, pointId));
        for(Long id : deviceIds){
            save(new MeteringPointRel(null,pointId, id, MeteringPointRel.TYPE_DEVICE));
        }
        for(Long id : pointIds){
            save(new MeteringPointRel(null,pointId, id, MeteringPointRel.TYPE_METERING_POINT));
        }
    }

    @Override
    public void removeByPointIds(Collection<Long> pointIds) {
        if(CollectionUtils.isEmpty(pointIds)){
            return;
        }
        remove(new LambdaQueryWrapper<MeteringPointRel>().in(MeteringPointRel::getMeteringPointId, pointIds));
    }

    @Override
    public List<Long> findDeviceIdByPointIds(Collection<Long> pointIds) {
        if(CollectionUtils.isEmpty(pointIds)){
            return Collections.emptyList();
        }
        return list(
                new LambdaQueryWrapper<MeteringPointRel>()
                        .eq(MeteringPointRel::getRelType,MeteringPointRel.TYPE_DEVICE)
                        .in(MeteringPointRel::getMeteringPointId, pointIds)
        ).stream().map(MeteringPointRel::getRelId).collect(Collectors.toList());
    }

    @Override
    public List<Long> findPointIdsByDeviceId(Long deviceId) {
        return list(new LambdaQueryWrapper<MeteringPointRel>().eq(MeteringPointRel::getRelType,MeteringPointRel.TYPE_DEVICE).eq(MeteringPointRel::getRelId, deviceId))
                .stream()
                .map(MeteringPointRel::getMeteringPointId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findDeviceIdByPointId(Long pointId) {
        return list(new LambdaQueryWrapper<MeteringPointRel>().eq(MeteringPointRel::getRelType,MeteringPointRel.TYPE_DEVICE).eq(MeteringPointRel::getMeteringPointId, pointId))
                .stream()
                .map(MeteringPointRel::getRelId)
                .collect(Collectors.toList());
    }

}
