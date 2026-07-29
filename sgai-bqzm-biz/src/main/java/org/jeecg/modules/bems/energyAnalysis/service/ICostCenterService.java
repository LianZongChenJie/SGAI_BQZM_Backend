package org.jeecg.modules.bems.energyAnalysis.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.energyAnalysis.entity.CostCenter;

import java.util.List;

public interface ICostCenterService extends IService<CostCenter> {

    void add(CostCenter data);

    void update(CostCenter data);

    void delete(Long id);

    List<CostCenter> getTree();

    Page<CostCenter> listByParentId(CostCenter param);

}
