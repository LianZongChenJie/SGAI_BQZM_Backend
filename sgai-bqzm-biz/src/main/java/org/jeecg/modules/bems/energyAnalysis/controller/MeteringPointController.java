package org.jeecg.modules.bems.energyAnalysis.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.energyAnalysis.dto.MeasureRuleDto;
import org.jeecg.modules.bems.energyAnalysis.dto.MeteringPointDto;
import org.jeecg.modules.bems.energyAnalysis.entity.MeteringPoint;
import org.jeecg.modules.bems.energyAnalysis.service.IMeteringPointService;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointTreeVo;
import org.jeecg.modules.bems.energyAnalysis.vo.MeteringPointVo;
import org.jeecg.modules.bems.energyAnalysis.vo.PermissionMeteringPointTreeModel;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bems/meteringPoint")
@AllArgsConstructor
@Api(tags="计量点位")
@Slf4j
public class MeteringPointController {

    private final IMeteringPointService service;

    /**
     *   添加
     *
     * @param params
     * @return
     */
    @AutoLog(value = "计量点位-添加")
    @ApiOperation(value="计量点位-添加", notes="计量点位-添加")
    @RequiresPermissions("bems:metering_point:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody MeteringPoint params) {
        service.save(params);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "计量点位-编辑")
    @ApiOperation(value="计量点位-编辑", notes="计量点位-编辑")
    @RequiresPermissions("bems:metering_point:edit")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody MeteringPoint params){
        service.updateById(params);
        return Result.ok();
    }

    @AutoLog(value = "计量点位-通过id删除")
    @ApiOperation(value="计量点位-通过id删除", notes="计量点位-通过id删除")
    @RequiresPermissions("bems:metering_point:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.deleteById(id);
        return Result.ok();
    }

    @ApiOperation(value="计量点位-树",notes="计量点位-树")
    @GetMapping("/getTree")
    public Result<List<MeteringPointTreeVo>> getTree(String type){
        return Result.OK(service.getTree(type));
    }

    @AutoLog(value = "计量点位-权限树")
    @ApiOperation(value = "计量点位-权限树", notes = "根据当前用户数据权限查询计量点位树，父级节点会被标记为不在权限范围内")
    @GetMapping("/getPermissionTree")
    public Result<List<PermissionMeteringPointTreeModel>> getPermissionTree(@RequestParam(required = false) String type){
        return Result.OK(service.getPermissionTree(type));
    }

    @GetMapping("/list")
    @ApiOperation(value="计量点位-列表",notes="计量点位-列表")
    @DataPermission
    public Result<IPage<MeteringPointTreeVo>> list(MeasureRuleDto params){
        if(StringUtils.isEmpty(params.getType())){
            return Result.ok(new Page<>());
        }
        return Result.ok(service.listPage(params).convert(MeteringPointTreeVo::convert));
    }

    @ApiOperation(value = "计量点位-解析公式", notes = "计量点位-解析公式")
    @PostMapping("/analyticFormula")
    public Result<String> analyticFormula(@RequestBody MeteringPoint rule) {
        return Result.OK("",service.analyticFormula(rule.getFormula()));
    }

    @ApiOperation(value = "计量点位-保存公式", notes = "计量点位-保存公式")
    @PostMapping("/saveFormula")
    @AutoLog(value = "计量点位-保存公式")
    public Result<Boolean> saveFormula(@RequestBody MeteringPoint rule){
        return Result.ok(service.saveFormula(rule));
    }

    /**
     * 包含设备信息和点位信息
     */
    @GetMapping("/listPoint")
    @ApiOperation(value="计量点位信息-列表",notes="计量点位信息-列表")
    public Result<IPage<MeteringPointVo>> listPoint(MeteringPointDto params){
        return Result.ok(service.listPoint(params));
    }

    /**
     * 获取全部计量规则数
     * @return 树
     */
    @GetMapping("/getAllTree")
    public Result<List<MeteringPointTreeVo>> getAllTree(){
        return Result.ok(service.getAllTree());
    }

    @GetMapping("/getById")
    public Result<MeteringPoint> getById(@RequestParam Long id){
        return Result.ok(service.getById(id));
    }
}
