package org.jeecg.modules.bems.lighting.mq.listener;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.annotation.RabbitComponent;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.jeecg.modules.bems.lighting.mq.message.PowerBoxData;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.List;

@RabbitComponent(value = "LightingMsgListener")
@Slf4j
@AllArgsConstructor
public class LightingMsgListener {

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    private final ILightingPlanService planService;

    private final LightingSendService sendService;

    private final RedisUtil redisUtil;

    @RabbitListener(queues = LightingMqConstant.QUEUE_LIGHTING_LISTENER, ackMode = "AUTO")
    public void lightingDataListener(Message message){
        String body = new String(message.getBody());
        try {
            log.info("mq消息消费成功。queue:{}，message:{}", LightingMqConstant.QUEUE_LIGHTING_LISTENER, body);
            List<LightInfoUpdateLoad> list = JSONObject.parseArray(body, LightInfoUpdateLoad.class);
            for(LightInfoUpdateLoad item : list){
                // 判断是什么类型
                String dataType = item.getDataType();
                if(StringUtils.isEmpty(dataType) || StringUtils.isEmpty(item.getValue())){
                    continue;
                }
                switch(dataType){
                    case "3":
                        // 场景,9：开，4：关
                        areaService.mqControl(item.getGatewayCode(),String.valueOf(item.getAreaID()),item.getValue());
                        break;
                    case "0":
                        // 回路开关
                        String value = item.getValue();
                        // 判断value是否为数字，如果不是数字则将value设置为-1，如果是数字并且在0-100之间，则将value转换为数字
                        boolean isValidNumber = false;
                        try {
                            int numValue = Integer.parseInt(value);
                            isValidNumber = numValue >= 0 && numValue <= 100;
                            if(numValue == 0){
                                value = "0";
                            }else if(numValue > 0 && numValue <= 100){
                                value = "100";
                            }
                        } catch (NumberFormatException e) {
                            isValidNumber = false;
                        }

                        if(isValidNumber){
                            circuitService.mqControl(item.getGatewayCode(),String.valueOf(item.getAreaID()),String.valueOf(item.getCircuitCode()),LightingCircuit.STATUS_MAP.get(value));
                            // 更新通讯状态
                            circuitService.updateComstat(item.getGatewayCode(),String.valueOf(item.getAreaID()),String.valueOf(item.getCircuitCode()), LightingCircuit.COMSTAT_ONLINE);
                            // 发送消息
                            sendService.sendLightingCircuitComstat(item.getGatewayCode(),String.valueOf(item.getAreaID()),String.valueOf(item.getCircuitCode()));
                        }else{
                            // 状态错误，设置为离线
                            circuitService.updateComstat(item.getGatewayCode(),String.valueOf(item.getAreaID()),String.valueOf(item.getCircuitCode()), LightingCircuit.COMSTAT_OFFLINE);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            log.error("mq消息消费失败。queue:{}，message:{}", LightingMqConstant.QUEUE_LIGHTING_LISTENER, body, e);
        }

    }


    @RabbitListener(queues = LightingMqConstant.QUEUE_LIGHTING_PLAN, ackMode = "AUTO")
    public void planListener(Message message){
        String body = new String(message.getBody());
        try{
            JSONObject jsonObject = JSONObject.parseObject(body);
            Long planId = jsonObject.getLong("planId");
            String version = jsonObject.getString("version");
            planService.execution(planId,version);
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}", LightingMqConstant.QUEUE_LIGHTING_PLAN, body, e);
        }
    }

    @RabbitListener(queues = LightingMqConstant.QUEUE_LIGHTING_CIRCUIT_COMSTAT, ackMode = "AUTO")
    public void comstatListener(Message message){
        String body = new String(message.getBody());
        try {
            JSONObject jsonObject = JSONObject.parseObject(body);
            String space = jsonObject.getString("space");
            String areaCode = jsonObject.getString("areaCode");
            String circuitCode = jsonObject.getString("circuitCode");
            String comstat = jsonObject.getString("comstat");
            Object o = redisUtil.get("lighting:" + space + ":" + areaCode + ":" + circuitCode);
            if(o != null){
                return;
            }
            circuitService.updateComstat(space, areaCode, circuitCode, comstat);
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}", LightingMqConstant.QUEUE_LIGHTING_PLAN, body, e);
        }
    }

    /**
     * 四高炉灯控专用-小程序同步状态消息监听
     * 接收电箱控制小程序同步过来的泛光电箱状态，更新 lighting_area 表
     * deviceSn → area_code
     * devicestate → status
     * 单独处理，不走老的KNX那套逻辑
     */
    @RabbitListener(queues = LightingMqConstant.QUEUE_SGF_LIGHTING_STATUS_SYNC, ackMode = "AUTO")
    public void sgfStatusSyncListener(Message message){
        String body = new String(message.getBody());
        try {
            log.info("【四高炉灯控】收到小程序同步电箱状态消息：{}", body);
            List<PowerBoxData> powerBoxList = JSONObject.parseArray(body, PowerBoxData.class);

            if(powerBoxList == null || powerBoxList.isEmpty()){
                log.warn("【四高炉灯控】收到的电箱列表为空");
                return;
            }

            int updateCount = 0;
            for(PowerBoxData box : powerBoxList){
                // 根据 area_code（deviceSn）查询区域
                LambdaQueryWrapper<LightingArea> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(LightingArea::getAreaCode, box.getDeviceSn());
                LightingArea area = areaService.getOne(wrapper);

                if(area == null){
                    log.warn("【四高炉灯控】未找到对应的区域，area_code={}", box.getDeviceSn());
                    continue;
                }

                // 更新状态（存文字）
                area.setStatus(convertStateToText(box.getDevicestate()));
                // TODO: 如果以后改回存数字，用下面这行
                // area.setStatus(String.valueOf(box.getDevicestate()));
                areaService.updateById(area);
                updateCount++;

                log.info("【四高炉灯控】更新区域状态：area_code={}, areaName={}, status={}",
                        box.getDeviceSn(), area.getAreaName(), box.getDevicestate());
            }

            log.info("【四高炉灯控】小程序同步电箱状态处理完成，共 {} 个电箱，更新 {} 个", powerBoxList.size(), updateCount);
        } catch (Exception e) {
            log.error("【四高炉灯控】小程序同步电箱状态消息处理异常", e);
        }
    }

    /**
     * 泛光电箱状态转文字
     * 1→开启，0→关闭，2→离线
     */
    private String convertStateToText(Integer deviceState) {
        if (deviceState == null) {
            return "离线";
        }
        switch (deviceState) {
            case 1:
                return "开启";
            case 0:
                return "关闭";
            case 2:
            default:
                return "离线";
        }
    }
}