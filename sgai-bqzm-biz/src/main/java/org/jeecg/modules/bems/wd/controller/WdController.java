package org.jeecg.modules.bems.wd.controller;

import lombok.AllArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.bems.wd.dto.ScreenFireControlRoomDto;
import org.jeecg.modules.bems.wd.dto.SituationStatisticDto;
import org.jeecg.modules.bems.wd.service.WdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/bems/wd")
public class WdController {

    private final WdService service;

    /**
     * 查询火警处理及时率、异常处理及时率、异常处置情况
     */
    @GetMapping("/getSituationStatistic")
    public Result<SituationStatisticDto> getSituationStatistic(){
        return Result.OK(service.getSituationStatistic());
    }

    /**
     * 查询项目中得消控室值守人员和维保人员的统计数据和人员得取证情况
     */
    @GetMapping("/getScreenFireControlRoom")
    public Result<ScreenFireControlRoomDto> getScreenFireControlRoom(){
        return Result.OK(service.getScreenFireControlRoom());
    }

}
