package org.jeecg.modules.bems.patterned.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.bems.patterned.entity.LinkageStrategy;
import org.jeecg.modules.bems.patterned.service.ILinkageStrategyService;
import org.springframework.web.bind.annotation.*;

/**
 * 联动控制
 */
@RestController
@RequestMapping("/bems/linkageStrategy")
@AllArgsConstructor
public class LinkageStrategyController {

    private final ILinkageStrategyService service;

    @AutoLog(value = "联动策略-新增")
    @RequiresPermissions("bems:linkageStrategy:add")
    @PostMapping("/add")
    public Result<String> add(@RequestBody LinkageStrategy param){
        service.save(param);
        return Result.ok();
    }

    @AutoLog(value = "联动策略-编辑")
    @RequiresPermissions("bems:linkageStrategy:edit")
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody LinkageStrategy param){
        service.updateById(param);
        return Result.ok();
    }

    @AutoLog(value = "联动策略-删除")
    @RequiresPermissions("bems:linkageStrategy:delete")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id){
        service.removeById(id);
        return Result.ok();
    }

    /**
     * 获取联动策略详情
     * @param id 联动策略id
     * @return 联动策略信息、前置点位、后置点位
     */
    @GetMapping("/getDetailById")
    public Result<LinkageStrategy> getDetailById(@RequestParam Long id){
        return Result.ok(service.getDetailById(id));
    }

    @AutoLog(value = "联动策略-启用")
    @RequiresPermissions("bems:linkageStrategy:startStrategy")
    @PostMapping("/startStrategy")
    public Result<String> startStrategy(@RequestParam Long id){
        service.startStrategy(id);
        return Result.ok();
    }

    @AutoLog(value = "联动策略-禁用")
    @RequiresPermissions("bems:linkageStrategy:stopStrategy")
    @PostMapping("/stopStrategy")
    public Result<String> stopStrategy(@RequestParam Long id){
        service.stopStrategy(id);
        return Result.ok();
    }


    @GetMapping("/listPage")
    public Result<IPage<LinkageStrategy>> listPage(LinkageStrategy param){
        return Result.ok(service.listPage(param));
    }

}
