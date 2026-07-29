package org.jeecg.modules.bems.event.service;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.event.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Data
public class EventService {
    /**
     * 事件工单系统地址
     */
    @Value("${event.path}")
    private String path;

    @Value("${event.phone}")
    private String phone;

    @Value("${event.spaceName}")
    private String spaceName;

    /**
     * 获取token
     */
    private final String get_token_url = "/sys/phone/login";

    /**
     * 创建事件
     */
    private final String create_event_url = "/jeecg-demo/event/event/transferData";

    /**
     * 获取事件详情
     */
    private final String event_detail_url = "/jeecg-demo/event/event/detail";

    /**
     * 获取事件记录
     */
    private final String event_record_url = "/jeecg-demo/event/event/record";

    /**
     * 获取空间信息
     */
    private final String space_url = "/jeecg-demo/mdm/space/getTree";

    /**
     * 事件分布
     */
    private final String event_distribution_url = "/jeecg-demo/event/screen/eventDistribution";
    /**
     * 工单分布
     */
    private final String order_distribution_url = "/jeecg-demo/event/screen/orderDistribution";

    @Autowired
    private final RedisTemplate<String,String> redisTemplate;

    private String getToken(){
        String token = redisTemplate.opsForValue().get("event:token");
        if(StringUtils.isNotEmpty(token)){
            return token;
        }
        String body = HttpUtil.createPost(path + get_token_url)
                .form("phone",phone)
                .execute()
                .body();
        JSONObject result = JSONObject.parseObject(body);
        token = result.getJSONObject("result").getString("token");
        redisTemplate.opsForValue().set("event:token",token,24, TimeUnit.HOURS);
        return token;
    }

    /**
     * 创建事件
     * @param businessData
     *      联系人、联系电话、空间信息、区域信息、描述
     * @return 事件id
     */
    public String createEvent(BusinessDataDto businessData){
        businessData.setSpaceName(spaceName);
        String token = getToken();
        CreateEventDto body = new CreateEventDto();
        body.setBusinessData(businessData);
        String result = HttpUtil.createPost(path + create_event_url)
                .header("x-access-token", token)
                .body(JSONObject.toJSONString(body), ContentType.JSON.toString())
                .execute()
                .body();
        return JSONObject.parseObject(result).getString("result");
    }

    /**
     * 获取事件基本信息
     * @param eventId 事件id
     * @return 事件信息
     */
    public Event getEventDetail(String eventId){
        String token = getToken();
        String result = HttpUtil.createGet(path + event_detail_url)
                .header("x-access-token", token)
                .form("id", eventId)
                .execute()
                .body();
        return JSONObject.parseObject(result).getObject("result", Event.class);
    }

    /**
     * 获取事件记录
     * @param eventId 事件id
     * @return 事件记录
     */
    public List<EventOperateRecord> getEventRecord(String eventId){
        String token = getToken();
        String result = HttpUtil.createGet(path + event_record_url)
                .header("x-access-token", token)
                .form("orderId", eventId)
                .execute()
                .body();
        return JSONObject.parseObject(result).getJSONArray("result").toJavaList(EventOperateRecord.class);
    }

    /**
     * 获取空间信息
     * @return 空间信息
     */
    public List<EventSpace> getEventSpace(){
        String token = getToken();
        String result = HttpUtil.createGet(path + space_url)
                .header("x-access-token", token)
                .execute()
                .body();
        return JSONObject.parseObject(result).getJSONArray("result").toJavaList(EventSpace.class);
    }

    /**
     * 事件分布
     */
    public List<EventDistribution> eventDistribution(){
        String token = getToken();
        String result = HttpUtil.createGet(path + event_distribution_url)
                .header("x-access-token", token)
                .execute().body();
        return JSONObject.parseObject(result).getJSONArray("result").toJavaList(EventDistribution.class);
    }

    /**
     * 工单分布
     */
    public List<EventDistribution> orderDistribution(){
        String token = getToken();
        String result = HttpUtil.createGet(path + order_distribution_url)
                .header("x-access-token", token)
                .execute().body();
        return JSONObject.parseObject(result).getJSONArray("result").toJavaList(EventDistribution.class);
    }

}
