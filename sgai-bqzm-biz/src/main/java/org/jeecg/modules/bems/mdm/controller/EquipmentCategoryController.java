package org.jeecg.modules.bems.mdm.controller;

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
import org.jeecg.modules.bems.mdm.entity.EquipmentCategory;
import org.jeecg.modules.bems.mdm.service.IEquipmentCategoryService;
import org.jeecg.modules.bems.mdm.vo.PermissionEquipmentCategoryTreeModel;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;
import org.jeecg.modules.bems.permission.service.RoleDataPermissionService;
import org.jeecg.modules.bems.permission.vo.UserDataScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @Description: 设备类别
 * @Author: jeecg-boot
 * @Date: 2025-02-20
 * @Version: V1.0
 */
@Api(tags = "设备类别")
@RestController
@RequestMapping("/bems/equipmentCategory")
@Slf4j
public class EquipmentCategoryController extends JeecgController<EquipmentCategory, IEquipmentCategoryService> {
    @Autowired
    private IEquipmentCategoryService equipmentCategoryService;

    @Autowired
    private RoleDataPermissionService roleDataPermissionService;

    /**
     * 树查询
     *
     * @return
     */
    @ApiOperation(value = "设备类别-树", notes = "设备类别-树")
    @GetMapping("/getTree")
    public Result<List<SelectTreeModel>> getTree() {
        return Result.OK(equipmentCategoryService.buildTree());
    }

    /**
     * 仪表类别树
     *
     * @return
     */
    @GetMapping("/measuring/getTree")
    public Result<List<SelectTreeModel>> getTreeForMeasuring() {
        return Result.OK(equipmentCategoryService.buildTree(EquipmentCategory.TYPE_MEASURING));
    }

    /**
     * 设备类别树
     *
     * @return
     */
    @GetMapping("/equipment/getTree")
    public Result<List<SelectTreeModel>> getTreeForEquipment() {
        return Result.OK(equipmentCategoryService.buildTree(EquipmentCategory.TYPE_EQUIPMENT));
    }

    /**
     * 仪表类别权限树（根据用户数据权限过滤）
     *
     * @return 包含权限标记的仪表类别树
     */
    @ApiOperation(value = "设备类别-仪表权限树", notes = "根据当前用户数据权限查询仪表类别树，父级节点会被标记为不在权限范围内")
    @GetMapping("/measuring/getPermissionTree")
    public Result<List<PermissionEquipmentCategoryTreeModel>> getPermissionTreeForMeasuring() {
        return getPermissionTree(EquipmentCategory.TYPE_MEASURING);
    }

    /**
     * 设备类别权限树（根据用户数据权限过滤）
     *
     * @return 包含权限标记的设备类别树
     */
    @ApiOperation(value = "设备类别-设备权限树", notes = "根据当前用户数据权限查询设备类别树，父级节点会被标记为不在权限范围内")
    @GetMapping("/equipment/getPermissionTree")
    public Result<List<PermissionEquipmentCategoryTreeModel>> getPermissionTreeForEquipment() {
        return getPermissionTree(EquipmentCategory.TYPE_EQUIPMENT);
    }

    /**
     * 类别权限树（根据用户数据权限过滤）
     *
     * @return 包含权限标记的类别树
     */
    @ApiOperation(value = "类别-权限树", notes = "根据当前用户数据权限查询类别树，父级节点会被标记为不在权限范围内")
    @GetMapping("/getPermissionTree")
    public Result<List<PermissionEquipmentCategoryTreeModel>> getPermissionTree(@RequestParam(required = false) String type){
        // 1. 获取当前登录用户的专业权限范围
        UserDataScope dataScope = roleDataPermissionService.getCurrentUserDataScope();
        Set<Long> categoryIds = dataScope.getPermissionIds(RoleDataPermission.TYPE_CATEGORY);

        // 3. 如果没有权限，返回空树
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Result.OK(new ArrayList<>());
        }

        // 4. 构建类别权限树
        List<PermissionEquipmentCategoryTreeModel> tree = equipmentCategoryService.buildPermissionTree(
                categoryIds, type);

        return Result.OK(tree);
    }

    @ApiOperation(value = "设备类别-设备分页列表查询", notes = "设备类别-设备分页列表查询")
    @GetMapping("/equipment/rootList")
    public Result<IPage<EquipmentCategory>> queryPageListForEquipment(EquipmentCategory equipmentCategory,
                                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                      HttpServletRequest req) {
        equipmentCategory.setType(EquipmentCategory.TYPE_EQUIPMENT);
        return queryPageList(equipmentCategory, pageNo, pageSize, req);
    }

    @ApiOperation(value = "设备类别-仪表分页列表查询", notes = "设备类别-仪表分页列表查询")
    @GetMapping(value = "/measuring/rootList")
    public Result<IPage<EquipmentCategory>> queryPageListForMeasuring(EquipmentCategory equipmentCategory,
                                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                      HttpServletRequest req) {
        equipmentCategory.setType(EquipmentCategory.TYPE_MEASURING);
        return queryPageList(equipmentCategory, pageNo, pageSize, req);
    }

    /**
     * 分页列表查询
     *
     * @param equipmentCategory
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "设备类别-分页列表查询")
    @ApiOperation(value = "设备类别-分页列表查询", notes = "设备类别-分页列表查询")
    @GetMapping(value = "/rootList")
    public Result<IPage<EquipmentCategory>> queryPageList(EquipmentCategory equipmentCategory,
                                                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                          HttpServletRequest req) {
        String hasQuery = req.getParameter("hasQuery");
        if (hasQuery != null && "true".equals(hasQuery)) {
            QueryWrapper<EquipmentCategory> queryWrapper = QueryGenerator.initQueryWrapper(equipmentCategory, req.getParameterMap());
            List<EquipmentCategory> list = equipmentCategoryService.queryTreeListNoPage(queryWrapper);
            IPage<EquipmentCategory> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } else {
            Long parentId = equipmentCategory.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = IEquipmentCategoryService.ROOT_PID_VALUE;
            }
            equipmentCategory.setPid(null);
            QueryWrapper<EquipmentCategory> queryWrapper = QueryGenerator.initQueryWrapper(equipmentCategory, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            Page<EquipmentCategory> page = new Page<EquipmentCategory>(pageNo, pageSize);
            IPage<EquipmentCategory> pageList = equipmentCategoryService.page(page, queryWrapper);
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
            List<SelectTreeModel> ls = equipmentCategoryService.queryListByPid(pid);
            result.setResult(ls);
            result.setSuccess(true);
        } catch (Exception e) {
            e.printStackTrace();
            result.setMessage(e.getMessage());
            result.setSuccess(false);
        }
        return result;
    }

    @GetMapping("/equipment/loadTreeRoot")
    public Result<List<SelectTreeModel>> loadTreeRootForEquipment(@RequestParam(name = "async") Boolean async,
                                                                  @RequestParam(name = "pcode") String pcode) {
        return loadTreeRoot(async, pcode, EquipmentCategory.TYPE_EQUIPMENT);
    }

    @GetMapping("/measuring/loadTreeRoot")
    public Result<List<SelectTreeModel>> loadTreeRootForMeasuring(@RequestParam(name = "async") Boolean async,
                                                                  @RequestParam(name = "pcode") String pcode) {
        return loadTreeRoot(async, pcode, EquipmentCategory.TYPE_MEASURING);
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
        return loadTreeRoot(async, pcode, null);
    }

    private Result<List<SelectTreeModel>> loadTreeRoot(Boolean async, String pcode, String type) {
        Result<List<SelectTreeModel>> result = new Result<>();
        try {
            List<SelectTreeModel> ls = equipmentCategoryService.queryListByTypeAndCode(type, pcode);
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
            List<SelectTreeModel> temp = equipmentCategoryService.queryListByPid(Long.valueOf(tsm.getKey()));
            if (temp != null && temp.size() > 0) {
                tsm.setChildren(temp);
                loadAllChildren(temp);
            }
        }
    }

    /**
     * 获取子数据
     *
     * @param equipmentCategory
     * @param req
     * @return
     */
    //@AutoLog(value = "设备类别-获取子数据")
    @ApiOperation(value = "设备类别-获取子数据", notes = "设备类别-获取子数据")
    @GetMapping(value = "/childList")
    public Result<IPage<EquipmentCategory>> queryPageList(EquipmentCategory equipmentCategory, HttpServletRequest req) {
        QueryWrapper<EquipmentCategory> queryWrapper = QueryGenerator.initQueryWrapper(equipmentCategory, req.getParameterMap());
        List<EquipmentCategory> list = equipmentCategoryService.list(queryWrapper);
        IPage<EquipmentCategory> pageList = new Page<>(1, 10, list.size());
        pageList.setRecords(list);
        return Result.OK(pageList);
    }

    /**
     * 批量查询子节点
     *
     * @param parentIds 父ID（多个采用半角逗号分割）
     * @param parentIds
     * @return 返回 IPage
     * @return
     */
    //@AutoLog(value = "设备类别-批量获取子数据")
    @ApiOperation(value = "设备类别-批量获取子数据", notes = "设备类别-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            QueryWrapper<EquipmentCategory> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
            List<EquipmentCategory> list = equipmentCategoryService.list(queryWrapper);
            IPage<EquipmentCategory> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error("批量查询子节点失败：" + e.getMessage());
        }
    }

    /**
     * 添加
     *
     * @param equipmentCategory
     * @return
     */
    @AutoLog(value = "设备类别-设备添加")
    @ApiOperation(value = "设备类别-设备添加", notes = "设备类别-设备添加")
    @RequiresPermissions("bems:equipment_category:equipment:add")
    @PostMapping(value = "/equipment/add")
    public Result<String> addForEquipment(@RequestBody EquipmentCategory equipmentCategory) {
        equipmentCategory.setType(EquipmentCategory.TYPE_EQUIPMENT);
        equipmentCategoryService.addEquipmentCategory(equipmentCategory);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "设备类别-仪表添加")
    @ApiOperation(value = "设备类别-仪表添加", notes = "设备类别-仪表添加")
    @RequiresPermissions("bems:equipment_category:measuring:add")
    @PostMapping("/measuring/add")
    public Result<String> addForMeasuring(@RequestBody EquipmentCategory equipmentCategory) {
        equipmentCategory.setType(EquipmentCategory.TYPE_MEASURING);
        equipmentCategoryService.addEquipmentCategory(equipmentCategory);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param equipmentCategory
     * @return
     */
    @AutoLog(value = "设备类别-编辑")
    @ApiOperation(value = "设备类别-编辑", notes = "设备类别-编辑")
    @RequiresPermissions("bems:equipment_category:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody EquipmentCategory equipmentCategory) {
        equipmentCategoryService.updateEquipmentCategory(equipmentCategory);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "设备类别-通过id删除")
    @ApiOperation(value = "设备类别-通过id删除", notes = "设备类别-通过id删除")
    @RequiresPermissions("bems:equipment_category:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        equipmentCategoryService.deleteEquipmentCategory(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "设备类别-批量删除")
    @ApiOperation(value = "设备类别-批量删除", notes = "设备类别-批量删除")
    @RequiresPermissions("bems:equipment_category:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.equipmentCategoryService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "设备类别-通过id查询")
    @ApiOperation(value = "设备类别-通过id查询", notes = "设备类别-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<EquipmentCategory> queryById(@RequestParam(name = "id", required = true) String id) {
        EquipmentCategory equipmentCategory = equipmentCategoryService.getById(id);
        if (equipmentCategory == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(equipmentCategory);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param equipmentCategory
     */
    @RequiresPermissions("bems:equipment_category:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, EquipmentCategory equipmentCategory) {
        return super.exportXls(request, equipmentCategory, EquipmentCategory.class, "设备类别");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("bems:equipment_category:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, EquipmentCategory.class);
    }

}
