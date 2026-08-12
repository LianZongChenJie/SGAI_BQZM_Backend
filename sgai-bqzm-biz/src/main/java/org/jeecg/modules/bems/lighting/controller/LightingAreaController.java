package org.jeecg.modules.bems.lighting.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.dto.LightingSpaceDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 泛光照明-区域
 */
@Slf4j
@Api(tags = "照明-区域")
@RestController
@RequestMapping("/bems/lighting/area")
@AllArgsConstructor
public class LightingAreaController {

    private final ILightingAreaService service;

    /**
     * 区域信息查询
     * @param params 查询参数
     * @return 区域信息
     */
    @ApiOperation("分页查询区域列表")
    @DataPermission
    @GetMapping("/listPage")
    public Result<?> listPage(LightingAreaQueryDto params){
        return Result.ok(service.listPage(params));
    }

    /**
     * 区域信息查询，空间、名称不合并
     * @param params 查询参数
     * @return 区域信息
     */
    @ApiOperation("分页查询区域列表（空间、名称不合并）")
    @DataPermission
    @GetMapping("/listPage1")
    public Result<?> listPage1(LightingAreaQueryDto params){
        return Result.ok(service.listPage1(params));
    }

    /**
     * 导出区域列表Excel（查询条件同 listPage1，不分页）
     * @param params 查询参数
     */
    @ApiOperation("导出区域列表Excel")
    @DataPermission
    @GetMapping("/export")
    public void export(LightingAreaQueryDto params, HttpServletResponse response){
        service.exportExcel(params, response);
    }

    /**
     * 获取所有区域
     */
    @ApiOperation("获取所有区域")
    @GetMapping("/all")
    @DataPermission
    public Result<?> all(){
        return Result.ok(service.list());
    }

    /**
     * 开启
     * @param id 区域id
     */
    @ApiOperation("开启区域照明")
    @PostMapping("/open")
    public Result<String> open(Long id){
        service.open(id);
        return Result.ok();
    }

    /**
     * 关闭
     * @param id 区域id
     */
    @ApiOperation("关闭区域照明")
    @PostMapping("/close")
    public Result<String> close(Long id){
        service.close(id);
        return Result.ok();
    }

    /**
     * 撤回区域MQ下发消息
     * 只删除该区域下发、且未被消费的消息（共享队列不影响其他区域），并把待下发消息数清零
     * @param id 区域id
     */
    @ApiOperation("撤回区域MQ下发消息")
    @PostMapping("/recallMq")
    public Result<String> recallMq(Long id){
        int count = service.recallMqMessages(id);
        return Result.ok("已撤回未消费消息 " + count + " 条");
    }

    /**
     * 获取所有关联名称
     */
    @ApiOperation("获取所有关联名称")
    @GetMapping("/getAllRelName")
    public Result<List<String>> getAllRelName(){
        List<LightingArea> list = service.list();
        return Result.ok(list.stream().filter(area -> area.getRelName() != null).map(LightingArea::getRelName).distinct().toList());
    }

    /**
     * 获取所有空间
     */
    @ApiOperation("获取所有空间")
    @GetMapping("/getAllSpace")
    public Result<?> getAllSpace(){
        List<LightingArea> list = service.list();
        return Result.ok(LightingSpaceDto.convert(list));
    }

    /**
     * 获取区域详情
     * @param id 区域id
     */
    @ApiOperation("获取区域详情")
    @GetMapping("/detail")
    public Result<?> detail(Long id){
        return Result.ok(service.getById(id));
    }

    /**
     * 按区域查询1号馆的所有区域：查询条件写死 space_name='1号馆'，id 参数仅为兼容前端调用
     * @param id 区域id（前端固定传，如 478）
     */
    @ApiOperation("查询1号馆所有区域（查询条件写死 space_name='1号馆'，id 仅为兼容前端传参，如 478）")
    @GetMapping("/listBySpaceName")
    public Result<?> listBySpaceName(Long id){
        return Result.ok(service.listBySpaceName(id));
    }

    /**
     * 按空间名称控制该空间下所有回路的开/关（走1号馆902控制逻辑）
     * @param spaceName 空间名称（如：1号馆）
     * @param operationType 操作类型：开启 / 关闭
     */
    @ApiOperation("按空间名称控制所有回路（走1号馆902控制逻辑）")
    @PostMapping("/controlBySpaceName")
    public Result<?> controlBySpaceName(String spaceName, String operationType){
        boolean type = "开启".equals(operationType);
        service.controlBySpaceName(spaceName, type);
        return Result.ok(operationType+"成功");
    }

    /**
     * 新增区域
     * @param area 区域信息
     */
    @ApiOperation("新增区域")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LightingArea area){
        service.save(area);
        return Result.ok("新增成功");
    }

    /**
     * 编辑区域
     * @param area 区域信息
     */
    @ApiOperation("编辑区域")
    @PostMapping("/update")
    public Result<?> update(@RequestBody LightingArea area){
        service.updateById(area);
        return Result.ok("更新成功");
    }

    /**
     * 删除区域
     * @param id 区域id
     */
    @ApiOperation("删除区域")
    @PostMapping("/delete")
    public Result<?> delete(Long id){
        service.removeById(id);
        return Result.ok("删除成功");
    }

}
