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
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.entity.EnergyMediumManage;
import org.jeecg.modules.bems.service.IEnergyMediumManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

 /**
 * @Description: 能介管理
 * @Author: jeecg-boot
 * @Date:   2025-02-25
 * @Version: V1.0
 */
@Api(tags="能介管理")
@RestController
@RequestMapping("/bems/energyMediumManage")
@Slf4j
public class EnergyMediumManageController extends JeecgController<EnergyMediumManage, IEnergyMediumManageService>{
	@Autowired
	private IEnergyMediumManageService energyMediumManageService;

	/**
	 * 分页列表查询
	 *
	 * @param energyMediumManage
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "能介管理-分页列表查询")
	@ApiOperation(value="能介管理-分页列表查询", notes="能介管理-分页列表查询")
	@GetMapping(value = "/rootList")
	public Result<IPage<EnergyMediumManage>> queryPageList(EnergyMediumManage energyMediumManage,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		String hasQuery = req.getParameter("hasQuery");
        if(hasQuery != null && "true".equals(hasQuery)){
            QueryWrapper<EnergyMediumManage> queryWrapper =  QueryGenerator.initQueryWrapper(energyMediumManage, req.getParameterMap());
            List<EnergyMediumManage> list = energyMediumManageService.queryTreeListNoPage(queryWrapper);
            IPage<EnergyMediumManage> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }else{
            Long parentId = energyMediumManage.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = IEnergyMediumManageService.ROOT_PID_VALUE;
            }
            energyMediumManage.setPid(null);
            QueryWrapper<EnergyMediumManage> queryWrapper = QueryGenerator.initQueryWrapper(energyMediumManage, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            Page<EnergyMediumManage> page = new Page<EnergyMediumManage>(pageNo, pageSize);
            IPage<EnergyMediumManage> pageList = energyMediumManageService.page(page, queryWrapper);
            return Result.OK(pageList);
        }
	}

	 /**
	  * 【vue3专用】加载节点的子数据
	  *
	  * @param pid
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeChildren", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeChildren(@RequestParam(name = "pid") Long pid) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = energyMediumManageService.queryListByPid(pid);
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】加载一级节点/如果是同步 则所有数据
	  *
	  * @param async
	  * @param pcode
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeRoot", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeRoot(@RequestParam(name = "async") Boolean async, @RequestParam(name = "pcode") String pcode) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = energyMediumManageService.queryListByCode(pcode);
			 if (!async) {
				 loadAllChildren(ls);
			 }
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】递归求子节点 同步加载用到
	  *
	  * @param ls
	  */
	 private void loadAllChildren(List<SelectTreeModel> ls) {
		 for (SelectTreeModel tsm : ls) {
			 List<SelectTreeModel> temp = energyMediumManageService.queryListByPid(Long.valueOf(tsm.getKey()));
			 if (temp != null && temp.size() > 0) {
				 tsm.setChildren(temp);
				 loadAllChildren(temp);
			 }
		 }
	 }

	 /**
      * 获取子数据
      * @param energyMediumManage
      * @param req
      * @return
      */
	//@AutoLog(value = "能介管理-获取子数据")
	@ApiOperation(value="能介管理-获取子数据", notes="能介管理-获取子数据")
	@GetMapping(value = "/childList")
	public Result<IPage<EnergyMediumManage>> queryPageList(EnergyMediumManage energyMediumManage,HttpServletRequest req) {
		QueryWrapper<EnergyMediumManage> queryWrapper = QueryGenerator.initQueryWrapper(energyMediumManage, req.getParameterMap());
		List<EnergyMediumManage> list = energyMediumManageService.list(queryWrapper);
		IPage<EnergyMediumManage> pageList = new Page<>(1, 10, list.size());
        pageList.setRecords(list);
		return Result.OK(pageList);
	}

    /**
      * 批量查询子节点
      * @param parentIds 父ID（多个采用半角逗号分割）
      * @return 返回 IPage
      * @param parentIds
      * @return
      */
	//@AutoLog(value = "能介管理-批量获取子数据")
    @ApiOperation(value="能介管理-批量获取子数据", notes="能介管理-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            QueryWrapper<EnergyMediumManage> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
            List<EnergyMediumManage> list = energyMediumManageService.list(queryWrapper);
            IPage<EnergyMediumManage> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error("批量查询子节点失败：" + e.getMessage());
        }
    }
	
	/**
	 *   添加
	 *
	 * @param energyMediumManage
	 * @return
	 */
	@AutoLog(value = "能介管理-添加")
	@ApiOperation(value="能介管理-添加", notes="能介管理-添加")
    @RequiresPermissions("bems:energy_medium_manage:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody EnergyMediumManage energyMediumManage) {
		energyMediumManageService.addEnergyMediumManage(energyMediumManage);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param energyMediumManage
	 * @return
	 */
	@AutoLog(value = "能介管理-编辑")
	@ApiOperation(value="能介管理-编辑", notes="能介管理-编辑")
    @RequiresPermissions("bems:energy_medium_manage:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody EnergyMediumManage energyMediumManage) {
		energyMediumManageService.updateEnergyMediumManage(energyMediumManage);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "能介管理-通过id删除")
	@ApiOperation(value="能介管理-通过id删除", notes="能介管理-通过id删除")
    @RequiresPermissions("bems:energy_medium_manage:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		energyMediumManageService.deleteEnergyMediumManage(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "能介管理-批量删除")
	@ApiOperation(value="能介管理-批量删除", notes="能介管理-批量删除")
    @RequiresPermissions("bems:energy_medium_manage:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.energyMediumManageService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "能介管理-通过id查询")
	@ApiOperation(value="能介管理-通过id查询", notes="能介管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<EnergyMediumManage> queryById(@RequestParam(name="id",required=true) String id) {
		EnergyMediumManage energyMediumManage = energyMediumManageService.getById(id);
		if(energyMediumManage==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(energyMediumManage);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param energyMediumManage
    */
    @RequiresPermissions("bems:energy_medium_manage:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, EnergyMediumManage energyMediumManage) {
		return super.exportXls(request, energyMediumManage, EnergyMediumManage.class, "能介管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:energy_medium_manage:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		return super.importExcel(request, response, EnergyMediumManage.class);
    }

}
