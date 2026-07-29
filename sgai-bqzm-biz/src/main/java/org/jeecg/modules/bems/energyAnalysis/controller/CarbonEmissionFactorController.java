package org.jeecg.modules.bems.energyAnalysis.controller;

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
import org.jeecg.modules.bems.energyAnalysis.entity.CarbonEmissionFactor;
import org.jeecg.modules.bems.energyAnalysis.service.ICarbonEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

 /**
 * @Description: 碳排放因子管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
@Api(tags="碳排放因子管理")
@RestController
@RequestMapping("/bems/carbonEmissionFactor")
@Slf4j
public class CarbonEmissionFactorController extends JeecgController<CarbonEmissionFactor, ICarbonEmissionFactorService> {
	@Autowired
	private ICarbonEmissionFactorService carbonEmissionFactorService;
	
	/**
	 * 分页列表查询
	 *
	 * @param carbonEmissionFactor
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "碳排放因子管理-分页列表查询")
	@ApiOperation(value="碳排放因子管理-分页列表查询", notes="碳排放因子管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<CarbonEmissionFactor>> queryPageList(CarbonEmissionFactor carbonEmissionFactor,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        QueryWrapper<CarbonEmissionFactor> queryWrapper = QueryGenerator.initQueryWrapper(carbonEmissionFactor, req.getParameterMap());
		queryWrapper.orderByAsc("sort");
		Page<CarbonEmissionFactor> page = new Page<CarbonEmissionFactor>(pageNo, pageSize);
		IPage<CarbonEmissionFactor> pageList = carbonEmissionFactorService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param carbonEmissionFactor
	 * @return
	 */
	@AutoLog(value = "碳排放因子管理-添加")
	@ApiOperation(value="碳排放因子管理-添加", notes="碳排放因子管理-添加")
	@RequiresPermissions("bems:carbon_emission_factor:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody CarbonEmissionFactor carbonEmissionFactor) {
		carbonEmissionFactorService.save(carbonEmissionFactor);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param carbonEmissionFactor
	 * @return
	 */
	@AutoLog(value = "碳排放因子管理-编辑")
	@ApiOperation(value="碳排放因子管理-编辑", notes="碳排放因子管理-编辑")
	@RequiresPermissions("bems:carbon_emission_factor:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody CarbonEmissionFactor carbonEmissionFactor) {
		carbonEmissionFactorService.updateById(carbonEmissionFactor);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "碳排放因子管理-通过id删除")
	@ApiOperation(value="碳排放因子管理-通过id删除", notes="碳排放因子管理-通过id删除")
	@RequiresPermissions("bems:carbon_emission_factor:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		carbonEmissionFactorService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "碳排放因子管理-批量删除")
	@ApiOperation(value="碳排放因子管理-批量删除", notes="碳排放因子管理-批量删除")
	@RequiresPermissions("bems:carbon_emission_factor:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.carbonEmissionFactorService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "碳排放因子管理-通过id查询")
	@ApiOperation(value="碳排放因子管理-通过id查询", notes="碳排放因子管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<CarbonEmissionFactor> queryById(@RequestParam(name="id",required=true) String id) {
		CarbonEmissionFactor carbonEmissionFactor = carbonEmissionFactorService.getById(id);
		if(carbonEmissionFactor==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(carbonEmissionFactor);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param carbonEmissionFactor
    */
    @RequiresPermissions("bems:carbon_emission_factor:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, CarbonEmissionFactor carbonEmissionFactor) {
        return super.exportXls(request, carbonEmissionFactor, CarbonEmissionFactor.class, "碳排放因子管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:carbon_emission_factor:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, CarbonEmissionFactor.class);
    }

}
