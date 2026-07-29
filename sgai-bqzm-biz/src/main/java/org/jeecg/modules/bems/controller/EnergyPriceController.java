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
import org.jeecg.modules.bems.entity.EnergyPrice;
import org.jeecg.modules.bems.service.IEnergyPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * @Description: 能源价格管理
 * @Author: jeecg-boot
 * @Date:   2025-03-05
 * @Version: V1.0
 */
@Api(tags="能源价格管理")
@RestController
@RequestMapping("/bems/energyPrice")
@Slf4j
public class EnergyPriceController extends JeecgController<EnergyPrice, IEnergyPriceService> {
	@Autowired
	private IEnergyPriceService energyPriceService;
	
	/**
	 * 分页列表查询
	 *
	 * @param energyPrice
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "能源价格管理-分页列表查询")
	@ApiOperation(value="能源价格管理-分页列表查询", notes="能源价格管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<EnergyPrice>> queryPageList(EnergyPrice energyPrice,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<EnergyPrice> queryWrapper = QueryGenerator.initQueryWrapper(energyPrice, req.getParameterMap());
		Page<EnergyPrice> page = new Page<EnergyPrice>(pageNo, pageSize);
		IPage<EnergyPrice> pageList = energyPriceService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param energyPrice
	 * @return
	 */
	@AutoLog(value = "能源价格管理-添加")
	@ApiOperation(value="能源价格管理-添加", notes="能源价格管理-添加")
	@RequiresPermissions("bems:energy_price:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody EnergyPrice energyPrice) {
		energyPriceService.save(energyPrice);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param energyPrice
	 * @return
	 */
	@AutoLog(value = "能源价格管理-编辑")
	@ApiOperation(value="能源价格管理-编辑", notes="能源价格管理-编辑")
	@RequiresPermissions("bems:energy_price:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody EnergyPrice energyPrice) {
		energyPriceService.updateById(energyPrice);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "能源价格管理-通过id删除")
	@ApiOperation(value="能源价格管理-通过id删除", notes="能源价格管理-通过id删除")
	@RequiresPermissions("bems:energy_price:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		energyPriceService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "能源价格管理-批量删除")
	@ApiOperation(value="能源价格管理-批量删除", notes="能源价格管理-批量删除")
	@RequiresPermissions("bems:energy_price:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.energyPriceService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "能源价格管理-通过id查询")
	@ApiOperation(value="能源价格管理-通过id查询", notes="能源价格管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<EnergyPrice> queryById(@RequestParam(name="id",required=true) String id) {
		EnergyPrice energyPrice = energyPriceService.getById(id);
		if(energyPrice==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(energyPrice);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param energyPrice
    */
    @RequiresPermissions("bems:energy_price:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, EnergyPrice energyPrice) {
        return super.exportXls(request, energyPrice, EnergyPrice.class, "能源价格管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:energy_price:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, EnergyPrice.class);
    }

}
