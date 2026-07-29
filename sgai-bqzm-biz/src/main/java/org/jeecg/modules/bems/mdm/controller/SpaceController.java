package org.jeecg.modules.bems.mdm.controller;

import cn.hutool.core.util.StrUtil;
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
import org.jeecg.modules.bems.mdm.entity.Device;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.IDeviceService;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.jeecg.modules.bems.mdm.vo.PermissionSpaceTreeModel;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 空间位置
 * @Author: jeecg-boot
 * @Date:   2025-02-20
 * @Version: V1.0
 */
@Api(tags="空间位置")
@RestController
@RequestMapping("/bems/space")
@Slf4j
public class SpaceController extends JeecgController<Space, ISpaceService>{
	@Autowired
	private ISpaceService spaceService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private RoleDataPermissionService roleDataPermissionService;

	 /**
	  * 树查询
	  * @return
	  */
	 @ApiOperation(value = "空间位置-树",notes = "空间位置-树")
	 @GetMapping("/getTree")
	 public Result<List<SelectTreeModel>> getTree(){
		 return Result.OK(spaceService.buildTree());
	 }

	 /**
	  * 根据数据权限查询空间树
	  * @return 包含权限标记的空间树
	  */
	 @ApiOperation(value = "空间位置-权限树", notes = "根据当前用户数据权限查询空间树，父级节点会被标记为不在权限范围内")
	 @GetMapping("/getPermissionTree")
	 public Result<List<PermissionSpaceTreeModel>> getPermissionTree(){
		 // 1. 获取当前登录用户的空间权限范围
		 UserDataScope dataScope = roleDataPermissionService.getCurrentUserDataScope();
		 Set<Long> spaceIds = dataScope.getPermissionIds(RoleDataPermission.TYPE_SPACE);

		 // 3. 如果没有权限，返回空树
		 if (spaceIds == null || spaceIds.isEmpty()) {
			 return Result.OK(new ArrayList<>());
		 }

		 // 4. 构建权限树
		 List<PermissionSpaceTreeModel> tree = spaceService.buildPermissionTree(spaceIds);

		 return Result.OK(tree);
	 }

	 /**
	  * 树查询（只查询存在设备的树）
	  * @param deviceType 设备类别
	  * @param categoryIds 设备分类ID列表（逗号分隔）
	  * @return 空间树
	  */
	 @GetMapping("/getTreeByDeviceType")
	 public Result<List<SelectTreeModel>> getTree(String deviceType,@RequestParam(required = false) String categoryIds){
		 List<Device> devices = deviceService.findByType(deviceType);
		 if(StrUtil.isNotEmpty(categoryIds)){
			 List<Long> split = Arrays.stream(categoryIds.split(",")).map(Long::valueOf).toList();
			 devices = devices.stream().filter(device -> split.contains(device.getCategoryId())).toList();
		 }
		 return Result.ok(spaceService.buildTree(devices
				 .stream()
				 .map(Device::getSpaceId)
				 .collect(Collectors.toSet())));
	 }

	 /**
	  * 树查询（只查询存在设备的树，并根据用户数据权限过滤）
	  * @param deviceType 设备类别
	  * @param categoryIds 设备分类ID列表（逗号分隔）
	  * @return 空间树（只包含用户有权限且有设备的空间，包含disableCheckbox权限标记）
	  */
	 @GetMapping("/getPermissionTreeByDeviceType")
	 @ApiOperation(value = "空间位置-设备权限树", notes = "根据设备类型和当前用户数据权限查询空间树，包含权限标记")
	 public Result<List<PermissionSpaceTreeModel>> getPermissionTreeByDeviceType(
			 @RequestParam String deviceType,
			 @RequestParam(required = false) String categoryIds){
		 List<Device> devices = deviceService.findByType(deviceType);
		 if(StrUtil.isNotEmpty(categoryIds)){
			 List<Long> split = Arrays.stream(categoryIds.split(",")).map(Long::valueOf).toList();
			 devices = devices.stream().filter(device -> split.contains(device.getCategoryId())).toList();
		 }

		 // 获取设备所在的空间ID集合
		 Set<Long> deviceSpaceIds = devices.stream()
				 .map(Device::getSpaceId)
				 .collect(Collectors.toSet());

		 // 获取当前用户的数据权限范围
		 UserDataScope dataScope = roleDataPermissionService.getCurrentUserDataScope();
		 Set<Long> permissionSpaceIds = dataScope.getPermissionIds(RoleDataPermission.TYPE_SPACE);

		 // 如果用户没有空间权限，返回空树
		 if (permissionSpaceIds == null || permissionSpaceIds.isEmpty()) {
			 return Result.ok(new ArrayList<>());
		 }

		 // 取交集：只保留用户有权限且有设备的空间
		 deviceSpaceIds.retainAll(permissionSpaceIds);

		 return Result.ok(spaceService.buildPermissionTree(deviceSpaceIds));
	 }


	/**
	 * 分页列表查询
	 *
	 * @param space
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "空间位置-分页列表查询")
	@ApiOperation(value="空间位置-分页列表查询", notes="空间位置-分页列表查询")
	@GetMapping(value = "/rootList")
	public Result<IPage<Space>> queryPageList(Space space,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		String hasQuery = req.getParameter("hasQuery");
        if("true".equals(hasQuery)){
            QueryWrapper<Space> queryWrapper =  QueryGenerator.initQueryWrapper(space, req.getParameterMap());
            List<Space> list = spaceService.queryTreeListNoPage(queryWrapper);
            IPage<Space> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }else{
            Long parentId = space.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = ISpaceService.ROOT_PID_VALUE;
            }
            space.setPid(null);
            QueryWrapper<Space> queryWrapper = QueryGenerator.initQueryWrapper(space, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            Page<Space> page = new Page<Space>(pageNo, pageSize);
            IPage<Space> pageList = spaceService.page(page, queryWrapper);
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
			 List<SelectTreeModel> ls = spaceService.queryListByPid(pid);
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
			 List<SelectTreeModel> ls = spaceService.queryListByCode(pcode);
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
			 List<SelectTreeModel> temp = spaceService.queryListByPid(Long.valueOf(tsm.getKey()));
			 if (temp != null && !temp.isEmpty()) {
				 tsm.setChildren(temp);
				 loadAllChildren(temp);
			 }
		 }
	 }

	 /**
      * 获取子数据
      * @param space
      * @param req
      * @return
      */
	//@AutoLog(value = "空间位置-获取子数据")
	@ApiOperation(value="空间位置-获取子数据", notes="空间位置-获取子数据")
	@GetMapping(value = "/childList")
	public Result<IPage<Space>> queryPageList(Space space,HttpServletRequest req) {
		QueryWrapper<Space> queryWrapper = QueryGenerator.initQueryWrapper(space, req.getParameterMap());
		List<Space> list = spaceService.list(queryWrapper);
		IPage<Space> pageList = new Page<>(1, 10, list.size());
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
	//@AutoLog(value = "空间位置-批量获取子数据")
    @ApiOperation(value="空间位置-批量获取子数据", notes="空间位置-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
            List<Space> list = spaceService.list(queryWrapper);
            IPage<Space> pageList = new Page<>(1, 10, list.size());
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
	 * @param space
	 * @return
	 */
	@AutoLog(value = "空间位置-添加")
	@ApiOperation(value="空间位置-添加", notes="空间位置-添加")
    @RequiresPermissions("bems:space:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody Space space) {
		spaceService.addSpace(space);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param space
	 * @return
	 */
	@AutoLog(value = "空间位置-编辑")
	@ApiOperation(value="空间位置-编辑", notes="空间位置-编辑")
    @RequiresPermissions("bems:space:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody Space space) {
		spaceService.updateSpace(space);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "空间位置-通过id删除")
	@ApiOperation(value="空间位置-通过id删除", notes="空间位置-通过id删除")
    @RequiresPermissions("bems:space:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		spaceService.deleteSpace(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "空间位置-批量删除")
	@ApiOperation(value="空间位置-批量删除", notes="空间位置-批量删除")
    @RequiresPermissions("bems:space:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.spaceService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "空间位置-通过id查询")
	@ApiOperation(value="空间位置-通过id查询", notes="空间位置-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<Space> queryById(@RequestParam(name="id",required=true) String id) {
		Space space = spaceService.getById(id);
		if(space==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(space);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param space
    */
    @RequiresPermissions("bems:space:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Space space) {
		return super.exportXls(request, space, Space.class, "空间位置");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("bems:space:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		return super.importExcel(request, response, Space.class);
    }

}
