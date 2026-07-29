package org.jeecg.modules.bems.lighting.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.dto.LightingSpaceDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.permission.annotation.DataPermission;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 泛光照明-区域
 */
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
    @DataPermission
    @GetMapping("/listPage1")
    public Result<?> listPage1(LightingAreaQueryDto params){
        return Result.ok(service.listPage1(params));
    }

    /**
     * 获取所有区域
     */
    @GetMapping("/all")
    @DataPermission
    public Result<?> all(){
        return Result.ok(service.list());
    }

    /**
     * 开启
     * @param id 区域id
     */
    @PostMapping("/open")
    public Result<String> open(Long id){
        service.open(id);
        return Result.ok();
    }

    /**
     * 关闭
     * @param id 区域id
     */
    @PostMapping("/close")
    public Result<String> close(Long id){
        service.close(id);
        return Result.ok();
    }

    /**
     * 获取所有关联名称
     */
    @GetMapping("/getAllRelName")
    public Result<List<String>> getAllRelName(){
        List<LightingArea> list = service.list();
        return Result.ok(list.stream().filter(area -> area.getRelName() != null).map(LightingArea::getRelName).distinct().toList());
    }

    /**
     * 获取所有空间
     */
    @GetMapping("/getAllSpace")
    public Result<?> getAllSpace(){
        List<LightingArea> list = service.list();
        return Result.ok(LightingSpaceDto.convert(list));
    }

}
