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
import org.jeecg.modules.bems.entity.EnergyAttributeManagement;
import org.jeecg.modules.bems.service.IEnergyAttributeManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

 /**
 * @Description: 能源属性管理
 * @Author: jeecg-boot
 * @Date:   2025-02-26
 * @Version: V1.0
 */
@Api(tags="能源属性管理")
@RestController
@RequestMapping("/bems/energyAttributeManagement")
@Slf4j
public class EnergyAttributeManagementController extends JeecgController<EnergyAttributeManagement, IEnergyAttributeManagementService> {
	@Autowired
	private IEnergyAttributeManagementService energyAttributeManagementService;
	
	/**
	 * 分页列表查询
	 *
	 * @param energyAttributeManagement
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "能源属性管理-分页列表查询")
	@ApiOperation(value="能源属性管理-分页列表查询", notes="能源属性管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<EnergyAttributeManagement>> queryPageList(EnergyAttributeManagement energyAttributeManagement,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        QueryWrapper<EnergyAttributeManagement> queryWrapper = QueryGenerator.initQueryWrapper(energyAttributeManagement, req.getParameterMap());
		Page<EnergyAttributeManagement> page = new Page<EnergyAttributeManagement>(pageNo, pageSize);
		IPage<EnergyAttributeManagement> pageList = energyAttributeManagementService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param energyAttributeManagement
	 * @return
	 */
	@AutoLog(value = "能源属性管理-添加")
	@ApiOperation(value="能源属性管理-添加", notes="能源属性管理-添加")
	@RequiresPermissions("bems:energy_attribute_management:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody EnergyAttributeManagement energyAttributeManagement) {
		energyAttributeManagementService.save(energyAttributeManagement);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param energyAttributeManagement
	 * @return
	 */
	@AutoLog(value = "能源属性管理-编辑")
	@ApiOperation(value="能源属性管理-编辑", notes="能源属性管理-编辑")
	@RequiresPermissions("bems:energy_attribute_management:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody EnergyAttributeManagement energyAttributeManagement) {
		energyAttributeManagementService.updateById(energyAttributeManagement);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "能源属性管理-通过id删除")
	@ApiOperation(value="能源属性管理-通过id删除", notes="能源属性管理-通过id删除")
	@RequiresPermissions("bems:energy_attribute_management:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		energyAttributeManagementService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "能源属性管理-批量删除")
	@ApiOperation(value="能源属性管理-批量删除", notes="能源属性管理-批量删除")
	@RequiresPermissions("bems:energy_attribute_management:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.energyAttributeManagementService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "能源属性管理-通过id查询")
	@ApiOperation(value="能源属性管理-通过id查询", notes="能源属性管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<EnergyAttributeManagement> queryById(@RequestParam(name="id",required=true) String id) {
		EnergyAttributeManagement energyAttributeManagement = energyAttributeManagementService.getById(id);
		if(energyAttributeManagement==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(energyAttributeManagement);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param energyAttributeManagement
    */
    @RequiresPermissions("bems:energy_attribute_management:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, EnergyAttributeManagement energyAttributeManagement) {
        return super.exportXls(request, energyAttributeManagement, EnergyAttributeManagement.class, "能源属性管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:energy_attribute_management:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, EnergyAttributeManagement.class);
    }

}
