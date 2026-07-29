package org.jeecg.modules.bems.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointService;
import org.jeecg.modules.bems.project.dto.ProjectQueryDto;
import org.jeecg.modules.bems.project.vo.ProjectCountVo;
import org.jeecg.modules.bems.project.entity.Project;
import org.jeecg.modules.bems.project.service.IProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

 /**
 * @Description: 项目管理
 * @Author: jeecg-boot
 * @Date:   2025-05-26
 * @Version: V1.0
 */
@Api(tags="项目管理")
@RestController
@RequestMapping("/bems/project")
@Slf4j
public class ProjectController extends JeecgController<Project, IProjectService> {
	@Autowired
	private IProjectService projectService;

	@Autowired
	private IMeteringPointService meteringPointService;
	
	/**
	 * 分页列表查询
	 *
	 * @param project
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "项目管理-分页列表查询")
	@ApiOperation(value="项目管理-分页列表查询", notes="项目管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<Project>> queryPageList(Project project,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        // 自定义查询规则
        Map<String, QueryRuleEnum> customeRuleMap = new HashMap<>();
        // 自定义多选的查询规则为：LIKE_WITH_OR
        customeRuleMap.put("projectStatus", QueryRuleEnum.LIKE_WITH_OR);
        QueryWrapper<Project> queryWrapper = QueryGenerator.initQueryWrapper(project, req.getParameterMap(),customeRuleMap);
		Page<Project> page = new Page<Project>(pageNo, pageSize);
		IPage<Project> pageList = projectService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	 /**
	  * 分页查询
	  * @param params 查询参数
	  * @return IPage<Project>
	  */
	@GetMapping("/queryPage")
	public Result<IPage<Project>> queryPage(ProjectQueryDto params){
		return Result.ok(service.queryPage(params));
	}
	
	/**
	 *   添加
	 *
	 * @param project
	 * @return
	 */
	@AutoLog(value = "项目管理-添加")
	@ApiOperation(value="项目管理-添加", notes="项目管理-添加")
	@RequiresPermissions("bems:project:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody Project project) {
		if(project.getMeasurementTime() == null){
			project.setMeasurementTime(LocalDateTime.now());
		}
		projectService.save(project);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param project
	 * @return
	 */
	@AutoLog(value = "项目管理-编辑")
	@ApiOperation(value="项目管理-编辑", notes="项目管理-编辑")
	@RequiresPermissions("bems:project:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody Project project) {
		projectService.updateById(project);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "项目管理-通过id删除")
	@ApiOperation(value="项目管理-通过id删除", notes="项目管理-通过id删除")
	@RequiresPermissions("bems:project:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		projectService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "项目管理-批量删除")
	@ApiOperation(value="项目管理-批量删除", notes="项目管理-批量删除")
	@RequiresPermissions("bems:project:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.projectService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "项目管理-通过id查询")
	@ApiOperation(value="项目管理-通过id查询", notes="项目管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<Project> queryById(@RequestParam(name="id",required=true) String id) {
		Project project = projectService.getById(id);
		if(project==null) {
			return Result.error("未找到对应数据");
		}
		// 获取关联计量点位名称
		Long pointId = project.getPointId();
		project.setPointName(meteringPointService.getMeteringPointFullNameById(pointId));
		return Result.OK(project);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param project
    */
    @RequiresPermissions("bems:project:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Project project) {
        return super.exportXls(request, project, Project.class, "项目管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:project:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, Project.class);
    }

	 /**
	  * 项目统计
	  *
	  * @return
	  */
	 //@AutoLog(value = "项目管理-通过id查询")
	 @ApiOperation(value="项目管理-项目统计", notes="项目管理-项目统计")
	 @GetMapping(value = "/queryProjectStatistics")
	 public Result<ProjectCountVo> queryProjectStatistics() {
		 ProjectCountVo project = projectService.queryProjectStatistics();
		 if(project==null) {
			 return Result.error("未找到对应数据");
		 }
		 return Result.OK(project);
	 }

}
