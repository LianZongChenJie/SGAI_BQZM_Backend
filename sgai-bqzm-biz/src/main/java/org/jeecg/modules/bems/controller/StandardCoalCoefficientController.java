package org.jeecg.modules.bems.controller;

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
import org.jeecg.modules.bems.entity.StandardCoalCoefficient;
import org.jeecg.modules.bems.service.IStandardCoalCoefficientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

 /**
 * @Description: 折标煤系数管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
@Api(tags="折标煤系数管理")
@RestController
@RequestMapping("/bems/standardCoalCoefficient")
@Slf4j
public class 	StandardCoalCoefficientController extends JeecgController<StandardCoalCoefficient, IStandardCoalCoefficientService> {
	@Autowired
	private IStandardCoalCoefficientService standardCoalCoefficientService;
	
	/**
	 * 分页列表查询
	 *
	 * @param standardCoalCoefficient
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "折标煤系数管理-分页列表查询")
	@ApiOperation(value="折标煤系数管理-分页列表查询", notes="折标煤系数管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<StandardCoalCoefficient>> queryPageList(StandardCoalCoefficient standardCoalCoefficient,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        QueryWrapper<StandardCoalCoefficient> queryWrapper = QueryGenerator.initQueryWrapper(standardCoalCoefficient, req.getParameterMap());
		queryWrapper.orderByAsc("sort");
		Page<StandardCoalCoefficient> page = new Page<StandardCoalCoefficient>(pageNo, pageSize);
		IPage<StandardCoalCoefficient> pageList = standardCoalCoefficientService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param standardCoalCoefficient
	 * @return
	 */
	@AutoLog(value = "折标煤系数管理-添加")
	@ApiOperation(value="折标煤系数管理-添加", notes="折标煤系数管理-添加")
	@RequiresPermissions("bems:standard_coal_coefficient:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody StandardCoalCoefficient standardCoalCoefficient) {
		standardCoalCoefficientService.save(standardCoalCoefficient);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param standardCoalCoefficient
	 * @return
	 */
	@AutoLog(value = "折标煤系数管理-编辑")
	@ApiOperation(value="折标煤系数管理-编辑", notes="折标煤系数管理-编辑")
	@RequiresPermissions("bems:standard_coal_coefficient:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody StandardCoalCoefficient standardCoalCoefficient) {
		standardCoalCoefficientService.updateById(standardCoalCoefficient);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "折标煤系数管理-通过id删除")
	@ApiOperation(value="折标煤系数管理-通过id删除", notes="折标煤系数管理-通过id删除")
	@RequiresPermissions("bems:standard_coal_coefficient:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		standardCoalCoefficientService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "折标煤系数管理-批量删除")
	@ApiOperation(value="折标煤系数管理-批量删除", notes="折标煤系数管理-批量删除")
	@RequiresPermissions("bems:standard_coal_coefficient:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.standardCoalCoefficientService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "折标煤系数管理-通过id查询")
	@ApiOperation(value="折标煤系数管理-通过id查询", notes="折标煤系数管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<StandardCoalCoefficient> queryById(@RequestParam(name="id",required=true) String id) {
		StandardCoalCoefficient standardCoalCoefficient = standardCoalCoefficientService.getById(id);
		if(standardCoalCoefficient==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(standardCoalCoefficient);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param standardCoalCoefficient
    */
    @RequiresPermissions("bems:standard_coal_coefficient:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, StandardCoalCoefficient standardCoalCoefficient) {
        return super.exportXls(request, standardCoalCoefficient, StandardCoalCoefficient.class, "折标煤系数管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:standard_coal_coefficient:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, StandardCoalCoefficient.class);
    }

}
