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
import org.jeecg.modules.bems.entity.UnitManagement;
import org.jeecg.modules.bems.service.IUnitManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 计量单位管理
 * @Author: jeecg-boot
 * @Date:   2025-02-25
 * @Version: V1.0
 */
@Api(tags="计量单位管理")
@RestController
@RequestMapping("/bems/unitManagement")
@Slf4j
public class UnitManagementController extends JeecgController<UnitManagement, IUnitManagementService> {
	@Autowired
	private IUnitManagementService unitManagementService;
	
	/**
	 * 分页列表查询
	 *
	 * @param unitManagement
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "计量单位管理-分页列表查询")
	@ApiOperation(value="计量单位管理-分页列表查询", notes="计量单位管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<UnitManagement>> queryPageList(UnitManagement unitManagement,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        QueryWrapper<UnitManagement> queryWrapper = QueryGenerator.initQueryWrapper(unitManagement, req.getParameterMap());
		Page<UnitManagement> page = new Page<UnitManagement>(pageNo, pageSize);
		IPage<UnitManagement> pageList = unitManagementService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	@ApiOperation(value="计量单位管理-通过id查询所有", notes="计量单位管理-通过id查询所有")
	@GetMapping("/findAll")
	public Result<List<UnitManagement>> findAll(){
		return Result.OK(unitManagementService.list());
	}
	
	/**
	 *   添加
	 *
	 * @param unitManagement
	 * @return
	 */
	@AutoLog(value = "计量单位管理-添加")
	@ApiOperation(value="计量单位管理-添加", notes="计量单位管理-添加")
	@RequiresPermissions("bems:unit_management:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody UnitManagement unitManagement) {
		unitManagementService.save(unitManagement);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param unitManagement
	 * @return
	 */
	@AutoLog(value = "计量单位管理-编辑")
	@ApiOperation(value="计量单位管理-编辑", notes="计量单位管理-编辑")
	@RequiresPermissions("bems:unit_management:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody UnitManagement unitManagement) {
		unitManagementService.updateById(unitManagement);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "计量单位管理-通过id删除")
	@ApiOperation(value="计量单位管理-通过id删除", notes="计量单位管理-通过id删除")
	@RequiresPermissions("bems:unit_management:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		unitManagementService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "计量单位管理-批量删除")
	@ApiOperation(value="计量单位管理-批量删除", notes="计量单位管理-批量删除")
	@RequiresPermissions("bems:unit_management:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.unitManagementService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "计量单位管理-通过id查询")
	@ApiOperation(value="计量单位管理-通过id查询", notes="计量单位管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<UnitManagement> queryById(@RequestParam(name="id",required=true) String id) {
		UnitManagement unitManagement = unitManagementService.getById(id);
		if(unitManagement==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(unitManagement);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param unitManagement
    */
    @RequiresPermissions("bems:unit_management:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, UnitManagement unitManagement) {
        return super.exportXls(request, unitManagement, UnitManagement.class, "计量单位管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:unit_management:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, UnitManagement.class);
    }

}
