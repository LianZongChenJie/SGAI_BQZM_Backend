package org.jeecg.modules.bems.wd.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.wd.dto.ScreenFireControlRoomDto;
import org.jeecg.modules.bems.wd.dto.SituationStatisticDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Data
public class WdService {

    @Value("${sync.host}")
    private String host;
    @Value("${sync.apiKey}")
    private String apiKey;
    @Value("${sync.systemCode}")
    private String systemCode;

    /**
     * 查询火警处理及时率、异常处理及时率、异常处置情况
     */
    private static final String situationStatisticUrl = "/api/api-xf-fire/auth-api/singleProject/getSituationStatistic";

    /**
     * 查询项目中得消控室值守人员和维保人员的统计数据和人员得取证情况
     */
    private static final String screenFireControlRoomUrl = "/api/api-base-data/auth-api/platform/queryScreenFireControlRoom";

    /**
     * 查询火警处理及时率、异常处理及时率、异常处置情况
     */
    public SituationStatisticDto getSituationStatistic() {
        String body = HttpUtil.createGet(host + situationStatisticUrl)
                .addHeaders(getHeaders())
                .execute()
                .body();
        return JSONObject.parseObject(body).getObject("data", SituationStatisticDto.class);
    }

    /**
     * 查询项目中得消控室值守人员和维保人员的统计数据和人员得取证情况
     */
    public ScreenFireControlRoomDto getScreenFireControlRoom() {
        String body = HttpUtil.createGet(host + screenFireControlRoomUrl)
                .addHeaders(getHeaders())
                .execute()
                .body();
        return JSONObject.parseObject(body).getObject("data", ScreenFireControlRoomDto.class);
    }


    private Map<String, String> getHeaders() {
        return new HashMap<String, String>() {{
            put("apiKey", apiKey);
            put("systemCode", systemCode);
        }};
    }

}
