package org.jeecg.modules.bems.lighting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;
import org.jeecg.modules.bems.lighting.service.ILightingDistrictService;
import org.springframework.web.bind.annotation.*;

/**
 * 照明-片区管理（片区与区域一对多，区域通过 districtId 关联片区）
 */
@Slf4j
@Api(tags = "照明-片区")
@RestController
@RequestMapping("/bems/lighting/district")
@AllArgsConstructor
public class LightingDistrictController {

    private final ILightingDistrictService service;

    /**
     * 分页查询片区列表
     */
    @ApiOperation("分页查询片区列表")
    @GetMapping("/listPage")
    public Result<IPage<LightingDistrict>> listPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    String districtName,
                                                    String districtCode,
                                                    String status) {
        LambdaQueryWrapper<LightingDistrict> queryWrapper = new LambdaQueryWrapper<LightingDistrict>()
                .like(StringUtils.isNotEmpty(districtName), LightingDistrict::getDistrictName, districtName)
                .like(StringUtils.isNotEmpty(districtCode), LightingDistrict::getDistrictCode, districtCode)
                .eq(StringUtils.isNotEmpty(status), LightingDistrict::getStatus, status)
                .orderByAsc(LightingDistrict::getSort);
        return Result.ok(service.page(new Page<>(pageNo, pageSize), queryWrapper));
    }

    /**
     * 获取所有片区
     */
    @ApiOperation("获取所有片区")
    @GetMapping("/all")
    public Result<?> all() {
        return Result.ok(service.list(new LambdaQueryWrapper<LightingDistrict>().orderByAsc(LightingDistrict::getSort)));
    }

    /**
     * 获取片区详情
     */
    @ApiOperation("获取片区详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id) {
        return Result.ok(service.getById(id));
    }

    /**
     * 新增片区
     */
    @ApiOperation("新增片区")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingDistrict district) {
        service.save(district);
        return Result.ok("新增成功");
    }

    /**
     * 编辑片区
     */
    @ApiOperation("编辑片区")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingDistrict district) {
        service.updateById(district);
        return Result.ok("更新成功");
    }

    /**
     * 删除片区
     */
    @ApiOperation("删除片区")
    @PostMapping("/delete")
    public Result<?> delete(Long id) {
        service.removeById(id);
        return Result.ok("删除成功");
    }
}
