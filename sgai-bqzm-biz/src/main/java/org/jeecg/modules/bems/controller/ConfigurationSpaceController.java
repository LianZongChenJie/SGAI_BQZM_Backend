package org.jeecg.modules.bems.controller;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.dto.ConfigurationSpaceDto;
import org.jeecg.modules.bems.service.IBusinessConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bems/configurationSpace")
@AllArgsConstructor
public class ConfigurationSpaceController {

    private final IBusinessConfigService businessConfigService;

    @GetMapping("/get")
    public Result<List<ConfigurationSpaceDto>> get(){
        String valueByKey = businessConfigService.getValueByKey("configuration_space");
        List<ConfigurationSpaceDto> result = JSONArray.parseArray(valueByKey, ConfigurationSpaceDto.class);
        return Result.ok(result);
    }
}
