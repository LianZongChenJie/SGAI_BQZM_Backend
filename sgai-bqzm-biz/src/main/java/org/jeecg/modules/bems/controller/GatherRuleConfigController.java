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
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.modules.bems.entity.GatherRuleConfig;
import org.jeecg.modules.bems.service.IGatherRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

 /**
 * @Description: 采集管理-规则标准
 * @Author: jeecg-boot
 * @Date:   2025-02-19
 * @Version: V1.0
 */
@Api(tags="采集管理-规则标准")
@RestController
@RequestMapping("/bems/gatherRuleConfig")
@Slf4j
public class GatherRuleConfigController extends JeecgController<GatherRuleConfig, IGatherRuleConfigService> {
	@Autowired
	private IGatherRuleConfigService gatherRuleConfigService;
	
	/**
	 * 分页列表查询
	 *
	 * @param gatherRuleConfig
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "采集管理-规则标准-分页列表查询")
	@ApiOperation(value="采集管理-规则标准-分页列表查询", notes="采集管理-规则标准-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<GatherRuleConfig>> queryPageList(GatherRuleConfig gatherRuleConfig,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        // 自定义查询规则
        Map<String, QueryRuleEnum> customeRuleMap = new HashMap<>();
        // 自定义多选的查询规则为：LIKE_WITH_OR
        customeRuleMap.put("state", QueryRuleEnum.LIKE_WITH_OR);
        QueryWrapper<GatherRuleConfig> queryWrapper = QueryGenerator.initQueryWrapper(gatherRuleConfig, req.getParameterMap(),customeRuleMap);
		Page<GatherRuleConfig> page = new Page<GatherRuleConfig>(pageNo, pageSize);
		IPage<GatherRuleConfig> pageList = gatherRuleConfigService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param gatherRuleConfig
	 * @return
	 */
	@AutoLog(value = "采集管理-规则标准-添加")
	@ApiOperation(value="采集管理-规则标准-添加", notes="采集管理-规则标准-添加")
	@RequiresPermissions("bems:gather_rule_config:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody GatherRuleConfig gatherRuleConfig) {
		gatherRuleConfigService.save(gatherRuleConfig);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param gatherRuleConfig
	 * @return
	 */
	@AutoLog(value = "采集管理-规则标准-编辑")
	@ApiOperation(value="采集管理-规则标准-编辑", notes="采集管理-规则标准-编辑")
	@RequiresPermissions("bems:gather_rule_config:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody GatherRuleConfig gatherRuleConfig) {
		gatherRuleConfigService.updateById(gatherRuleConfig);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "采集管理-规则标准-通过id删除")
	@ApiOperation(value="采集管理-规则标准-通过id删除", notes="采集管理-规则标准-通过id删除")
	@RequiresPermissions("bems:gather_rule_config:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		gatherRuleConfigService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "采集管理-规则标准-批量删除")
	@ApiOperation(value="采集管理-规则标准-批量删除", notes="采集管理-规则标准-批量删除")
	@RequiresPermissions("bems:gather_rule_config:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.gatherRuleConfigService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "采集管理-规则标准-通过id查询")
	@ApiOperation(value="采集管理-规则标准-通过id查询", notes="采集管理-规则标准-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<GatherRuleConfig> queryById(@RequestParam(name="id",required=true) String id) {
		GatherRuleConfig gatherRuleConfig = gatherRuleConfigService.getById(id);
		if(gatherRuleConfig==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(gatherRuleConfig);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param gatherRuleConfig
    */
    @RequiresPermissions("bems:gather_rule_config:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, GatherRuleConfig gatherRuleConfig) {
        return super.exportXls(request, gatherRuleConfig, GatherRuleConfig.class, "采集管理-规则标准");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:gather_rule_config:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, GatherRuleConfig.class);
    }

}
