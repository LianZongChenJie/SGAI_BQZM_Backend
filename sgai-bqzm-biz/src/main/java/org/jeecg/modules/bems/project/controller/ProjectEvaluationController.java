package org.jeecg.modules.bems.project.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.project.dto.EvaluationReportData;
import org.jeecg.modules.bems.project.dto.EvaluationReportQueryDto;
import org.jeecg.modules.bems.project.vo.ProjectCategoryVo;
import org.jeecg.modules.bems.project.vo.ProjectOverviewVo;
import org.jeecg.modules.bems.project.entity.Project;
import org.jeecg.modules.bems.project.service.IProjectEvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目评价相关接口
 */
@RestController
@RequestMapping("/bems/project/evaluation")
@AllArgsConstructor
public class ProjectEvaluationController {

    private final IProjectEvaluationService service;

    /**
     * 获取项目总览
     */
    @GetMapping("/getOverview")
    public Result<ProjectOverviewVo> getOverview()
    {
        return Result.OK(service.getOverview());
    }

    /**
     * 投资前五排名
     */
    @GetMapping("/getInvestmentRanking")
    public Result<List<Project>> getInvestmentRanking(@RequestParam(defaultValue = "5") Integer top)
    {
        return Result.OK(service.getInvestmentRanking(top));
    }

    /**
     * 项目数量占比-各类别占比
     */
    @GetMapping("/proportionOfProjectCategories")
    public Result<List<ProjectCategoryVo>> proportionOfProjectCategories(){
        // service.getProportionOfProjectCategories()
        return Result.ok();
    }

    /**
     * 获取项目评价报告
     * @return 项目评价报告信息
     */
    @GetMapping("/getReport")
    public Result<EvaluationReportData> getReport(EvaluationReportQueryDto params){
        return Result.ok(service.getReport(params));
    }

}
