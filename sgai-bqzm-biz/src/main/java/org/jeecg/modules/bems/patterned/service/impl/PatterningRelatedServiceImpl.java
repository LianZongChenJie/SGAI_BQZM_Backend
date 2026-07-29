package org.jeecg.modules.bems.patterned.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.patterned.entity.PatterningRelated;
import org.jeecg.modules.bems.patterned.mapper.PatterningRelatedMapper;
import org.jeecg.modules.bems.patterned.service.IPatterningRelatedService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatterningRelatedServiceImpl extends ServiceImpl<PatterningRelatedMapper, PatterningRelated> implements IPatterningRelatedService {
    @Override
    public void removeByPreAssociationId(Long patterningStrategyId) {
        remove(new LambdaQueryWrapper<PatterningRelated>().eq(PatterningRelated::getPreAssociationId, patterningStrategyId));
    }

    @Override
    public void removeByPostAssociationId(Long patterningStrategyId) {
        remove(new LambdaQueryWrapper<PatterningRelated>().eq(PatterningRelated::getPostAssociationId, patterningStrategyId));
    }

    @Override
    @Transactional
    public void save(Long preAssociationId, List<PatterningRelated> patterningRelatedList) {
        for (PatterningRelated patterningRelated : patterningRelatedList) {
            patterningRelated.setId(null);
            patterningRelated.setPreAssociationId(preAssociationId);
            save(patterningRelated);
        }
    }

    @Override
    public List<PatterningRelated> findByPreAssociationId(Long preAssociationId) {
        return list(new LambdaQueryWrapper<PatterningRelated>().eq(PatterningRelated::getPreAssociationId, preAssociationId));
    }
}
