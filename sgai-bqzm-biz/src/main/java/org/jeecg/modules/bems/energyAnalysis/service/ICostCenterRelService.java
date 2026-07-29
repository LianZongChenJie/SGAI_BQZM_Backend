package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenterRel;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;

import java.util.List;

public interface ICostCenterRelService extends IService<CostCenterRel> {

    void saveRel(Long costCenterId, List<MeteringPointVo> relList);

    List<CostCenterRel> listByCostCenterId(Long costCenterId);

    /**
     * 判断关联关系id是否存在
     * @param type 关联关系类型
     * @param relId 关联关系id
     * @return 存在：true；不存在：false
     */
    boolean checkRelId(String type,Long relId);

    /**
     * 判断计量点id是否存在关联关系
     * @param meteringPointId 计量点id
     * @return 存在：true；不存在：false
     */
    boolean checkMeteringPointId(Long meteringPointId);
}
