package org.jeecg.modules.bems.patterned.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.patterned.entity.PatterningRelated;

import java.util.List;

public interface IPatterningRelatedService extends IService<PatterningRelated> {

    void removeByPreAssociationId(Long patterningStrategyId);

    void removeByPostAssociationId(Long patterningStrategyId);

    void save(Long preAssociationId, List<PatterningRelated> patterningRelatedList);

    List<PatterningRelated> findByPreAssociationId(Long preAssociationId);
}
