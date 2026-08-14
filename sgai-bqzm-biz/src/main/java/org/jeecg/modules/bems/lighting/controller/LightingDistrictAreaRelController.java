package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictAreaRel;
import org.jeecg.modules.bems.lighting.entity.LightingDistrictGroupVo;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictAreaRelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 照明-片区-分组-区域关联（片区 → 分组 → 区域）
 * 一个片区下可建多个分组（group_name），一个分组下可挂多个区域（area_id）
 */
@Slf4j
@Api(tags = "照明-片区分组区域关联")
@RestController
@RequestMapping("/bems/lighting/districtAreaRel")
@AllArgsConstructor
public class LightingDistrictAreaRelController {

    private final ILightingDistrictAreaRelService service;

    /**
     * 分页查询关联列表
     */
    @ApiOperation("分页查询片区-分组-区域关联列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", defaultValue = "1", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页条数", defaultValue = "10", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "districtId", value = "片区ID", paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "groupName", value = "分组名称", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "areaId", value = "区域ID", paramType = "query", dataType = "long")
    })
    @GetMapping("/listPage")
    public Result<IPage<LightingDistrictAreaRel>> listPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           Long districtId,
                                                           String groupName,
                                                           Long areaId) {
        LambdaQueryWrapper<LightingDistrictAreaRel> queryWrapper = new LambdaQueryWrapper<LightingDistrictAreaRel>()
                .eq(districtId != null, LightingDistrictAreaRel::getDistrictId, districtId)
                .like(StringUtils.isNotEmpty(groupName), LightingDistrictAreaRel::getGroupName, groupName)
                .eq(areaId != null, LightingDistrictAreaRel::getAreaId, areaId)
                .orderByAsc(LightingDistrictAreaRel::getDistrictId)
                .orderByAsc(LightingDistrictAreaRel::getGroupName)
                .orderByAsc(LightingDistrictAreaRel::getSort)
                .orderByAsc(LightingDistrictAreaRel::getId);
        return Result.ok(service.page(new Page<>(pageNo, pageSize), queryWrapper));
    }

    /**
     * 获取关联列表（不分页）
     */
    @ApiOperation("获取片区-分组-区域关联列表")
    @GetMapping("/list")
    public Result<List<LightingDistrictAreaRel>> list(Long districtId, String groupName) {
        LambdaQueryWrapper<LightingDistrictAreaRel> queryWrapper = new LambdaQueryWrapper<LightingDistrictAreaRel>()
                .eq(districtId != null, LightingDistrictAreaRel::getDistrictId, districtId)
                .eq(StringUtils.isNotEmpty(groupName), LightingDistrictAreaRel::getGroupName, groupName)
                .orderByAsc(LightingDistrictAreaRel::getSort)
                .orderByAsc(LightingDistrictAreaRel::getId);
        return Result.ok(service.list(queryWrapper));
    }

    /**
     * 按片区查询分组-区域树
     */
    @ApiOperation("按片区查询分组-区域树")
    @GetMapping("/listByDistrict")
    public Result<List<LightingDistrictGroupVo>> listByDistrict(Long id) {
        return Result.ok(service.listByDistrict(id));
    }

    /**
     * 获取关联详情
     */
    @ApiOperation("获取片区-分组-区域关联详情")
    @GetMapping("/detail")
    public Result<LightingDistrictAreaRel> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增关联
     */
    @ApiOperation("新增片区-分组-区域关联")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingDistrictAreaRel rel) {
        service.save(rel);
        return Result.ok("新增成功");
    }

    /**
     * 编辑关联
     */
    @ApiOperation("编辑片区-分组-区域关联")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingDistrictAreaRel rel) {
        service.updateById(rel);
        return Result.ok("更新成功");
    }

    /**
     * 删除关联
     */
    @ApiOperation("删除片区-分组-区域关联")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 新增分组：把一个片区的多个区域一次性挂到同一分组下
     */
    @ApiOperation("新增分组（批量把多个区域加入分组）")
    @PostMapping("/addGroup")
    public Result<?> addGroup(@RequestBody AddGroupRequest req) {
        service.addGroup(req.getDistrictId(), req.getGroupName(), req.getAreaIds(), req.getRemark());
        return Result.ok("新增成功");
    }

    /**
     * 删除分组：按片区+分组名删除该分组下的所有关联
     */
    @ApiOperation("删除分组（删除该分组下所有区域关联）")
    @PostMapping("/deleteGroup")
    public Result<?> deleteGroup(Long districtId, String groupName) {
        service.deleteGroup(districtId, groupName);
        return Result.ok("删除成功");
    }

    /**
     * 分组重命名
     */
    @ApiOperation("分组重命名")
    @PostMapping("/renameGroup")
    public Result<?> renameGroup(Long districtId, String oldName, String newName) {
        service.renameGroup(districtId, oldName, newName);
        return Result.ok("重命名成功");
    }

    /**
     * 新增分组请求体
     */
    @Data
    public static class AddGroupRequest {
        @ApiModelProperty(value = "片区ID")
        private Long districtId;

        @ApiModelProperty(value = "分组名称")
        private String groupName;

        @ApiModelProperty(value = "区域ID集合")
        private List<Long> areaIds;

        @ApiModelProperty(value = "备注")
        private String remark;
    }
}
