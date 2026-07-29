package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPointRel;

import java.util.Collection;
import java.util.List;

public interface IMeteringPointRelService extends IService<MeteringPointRel> {

    List<MeteringPointRel> findByTypeAndRelId(String type, Long relId);

    void updateRel(Long pointId, Collection<Long> deviceIds, Collection<Long> pointIds);

    void removeByPointIds(Collection<Long> pointIds);

    List<Long> findDeviceIdByPointIds(Collection<Long> pointIds);

    List<Long> findPointIdsByDeviceId(Long deviceId);

    List<Long> findDeviceIdByPointId(Long pointId);
}
