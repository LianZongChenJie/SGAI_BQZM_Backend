package org.jeecg.modules.bems.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationData;
import org.jeecg.modules.bems.project.dto.ProjectEnergyConservationResult;
import org.jeecg.modules.bems.project.dto.ProjectQueryDto;
import org.jeecg.modules.bems.project.vo.ProjectCountVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.project.entity.Project;

/**
 * @Description: 项目管理
 * @Author: jeecg-boot
 * @Date:   2025-05-26
 * @Version: V1.0
 */
public interface IProjectService extends IService<Project> {

    ProjectCountVo queryProjectStatistics();

    IPage<Project> queryPage(ProjectQueryDto params);

    /**
     * 耗能总量
     */
    ProjectEnergyConservationResult energyConservationStatistics();
}
