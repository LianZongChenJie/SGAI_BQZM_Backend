package org.jeecg.modules.bems.controller;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.dto.DataAmendParamDto;
import org.jeecg.modules.bems.service.IDataAmendLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 计量仪表小时数据修正
 */
@RestController
@RequestMapping("/bems/dataAmendLog")
@AllArgsConstructor
public class DataAmendLogController {

    private final IDataAmendLogService service;

    @GetMapping("/listPage")
    public Result<?> listPage(DataAmendParamDto params){
        List<Long> spaceIdList = StringUtils.isEmpty(params.getSpaceIds()) ? null : new ArrayList<>(Arrays.asList(params.getSpaceIds().split(","))).stream().map(Long::valueOf).collect(Collectors.toList());
        params.setSpaceIdList(spaceIdList);
        return Result.ok(service.listPage(params));
    }

}
