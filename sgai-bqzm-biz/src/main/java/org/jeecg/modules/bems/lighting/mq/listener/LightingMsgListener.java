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
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RabbitComponent(value = "LightingMsgListener")
@Slf4j
@AllArgsConstructor
public class LightingMsgListener {

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    private final ILightingPlanService planService;

    private final LightingSendService sendService;

    private final ILightingPlanExecuteLogService planExecuteLogService;

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
        Long planId = null;
        String version = null;
        String executeDate = null;
        try{
            JSONObject jsonObject = JSONObject.parseObject(body);
            planId = jsonObject.getLong("planId");
            version = jsonObject.getString("version");
            executeDate = jsonObject.getString("executeDate");
            boolean success = planService.execution(planId,version);
            planExecuteLogService.markConsumed(planId, version, executeDate, success,
                    success ? null : "计划执行失败（计划停用/版本不匹配/时间偏差超限等）");
        }catch (Exception e){
            log.error("mq消息消费失败。queue:{}，message:{}", LightingMqConstant.QUEUE_LIGHTING_PLAN, body, e);
            // 消费异常：记录执行失败，供日历展示
            planExecuteLogService.markConsumed(planId, version, executeDate, false, "MQ消息消费异常: " + e.getMessage());
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
//            log.info("【四高炉灯控】收到小程序同步电箱状态消息：{}", body);
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

//                log.info("【四高炉灯控】更新区域状态：area_code={}, areaName={}, status={}",
//                        box.getDeviceSn(), area.getAreaName(), box.getDevicestate());
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

    /**
     * 1号馆状态消息监听
     * 接收MQ转发小程序从181服务器转过来的状态消息，更新回路状态
     * 单独处理，不走老的KNX那套逻辑
     *
     * 消息格式：{"GatewayAdr":"10.22.133.31","KnxAdr":"2/2/1","value":"true","CollectionTime":"2026-08-08 19:34:34","Remark":null}
     * 对应回路 circuit_code 格式：Type=IpTunneling;HostAddress=10.22.133.31-3.1.22-2/2/1（网关+设备地址+KNX地址）
     * value：true/1/非0数字=开启，false/0=关闭
     */
    @RabbitListener(queues = LightingMqConstant.QUEUE_LIGHTING_LISTENER_1HG, ackMode = "AUTO")
    public void hg1StatusListener(Message message){
        String body = new String(message.getBody());
        try {
            log.info("【1号馆】收到状态消息：{}", body);
            JSONObject msg = JSONObject.parseObject(body);

            String gatewayAdr = msg.getString("GatewayAdr");
            String knxAdr = msg.getString("KnxAdr");
            String value = msg.getString("value");
            String collectionTime = msg.getString("CollectionTime");

            if(StringUtils.isEmpty(gatewayAdr) || StringUtils.isEmpty(knxAdr) || StringUtils.isEmpty(value)){
                log.warn("【1号馆】状态消息参数不完整，跳过");
                return;
            }

            // 1. 根据 GatewayAdr + KnxAdr 匹配回路
            //    老格式：circuit_code = "10.22.133.31-2/2/1"（精确匹配）
            //    新格式：circuit_code = "Type=IpTunneling;HostAddress=10.22.133.31-3.1.22-2/2/1"（模糊匹配）
            LightingCircuit circuit = null;

            LambdaQueryWrapper<LightingCircuit> exactWrapper = new LambdaQueryWrapper<>();
            exactWrapper.eq(LightingCircuit::getCircuitCode, gatewayAdr + "-" + knxAdr);
            circuit = circuitService.getOne(exactWrapper, false);

            if(circuit == null){
                LambdaQueryWrapper<LightingCircuit> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.like(LightingCircuit::getCircuitCode, "HostAddress=" + gatewayAdr + "-")
                        .like(LightingCircuit::getCircuitCode, "-" + knxAdr);
                List<LightingCircuit> circuitList = circuitService.list(likeWrapper);
                if(circuitList != null && !circuitList.isEmpty()){
                    circuit = circuitList.get(0);
                    if(circuitList.size() > 1){
                        log.warn("【1号馆】回路匹配到多条，取第一条：gatewayAdr={}, knxAdr={}, size={}", gatewayAdr, knxAdr, circuitList.size());
                    }
                }
            }

            if(circuit == null){
                log.warn("【1号馆】未找到对应的回路，gatewayAdr={}, knxAdr={}", gatewayAdr, knxAdr);
                return;
            }

            // 2. 解析开关状态：true/1/非0数字=开启，false/0=关闭，无法识别返回 null
            String status = parseOnOffStatus(value);

            if(status != null){
                // 最后在线时间（CollectionTime）
                if(StringUtils.isNotEmpty(collectionTime)){
                    try {
                        circuit.setLastOnlineTime(LocalDateTime.parse(collectionTime.trim(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        log.warn("【1号馆】CollectionTime 解析失败，忽略：{}", collectionTime);
                    }
                }
                // 更新回路状态（同时维护开启/关闭时间、开启总时长）
                circuitService.applyStatus(circuit, status);

                // 通过 area_id 查区域，拿到 space
                LightingArea area = areaService.getById(circuit.getAreaId());
                String space = area != null ? area.getSpace() : "";

                // 更新通讯状态为在线
                circuitService.updateComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode(), LightingCircuit.COMSTAT_ONLINE);

                // 发送离线延迟消息
                sendService.sendLightingCircuitComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode());

                log.info("【1号馆】更新回路状态：circuit_code={}, status={}", circuit.getCircuitCode(), status);
            }else{
                // 状态异常，设置为离线
                LightingArea area = areaService.getById(circuit.getAreaId());
                String space = area != null ? area.getSpace() : "";
                circuitService.updateComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode(), LightingCircuit.COMSTAT_OFFLINE);
                log.warn("【1号馆】状态值异常，设置为离线：circuit_code={}, value={}", circuit.getCircuitCode(), value);
            }
        } catch (Exception e) {
            log.error("【1号馆】状态消息处理异常", e);
        }
    }

    /**
     * 开关状态值解析：true/1/非0数字=开启，false/0=关闭，无法识别返回 null
     */
    private String parseOnOffStatus(String value) {
        if(StringUtils.isEmpty(value)){
            return null;
        }
        String v = value.trim();
        if("true".equalsIgnoreCase(v)){
            return LightingCircuit.STATUS_ON;
        }
        if("false".equalsIgnoreCase(v)){
            return LightingCircuit.STATUS_OFF;
        }
        try {
            int numValue = Integer.parseInt(v);
            if(numValue == 0){
                return LightingCircuit.STATUS_OFF;
            }
            if(numValue > 0){
                return LightingCircuit.STATUS_ON;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 北区（space=903）状态消息监听
     * 接收MQ转发小程序从181服务器转过来的状态消息，更新回路数据
     * 消息按 DataType 分流：
     * - DataType=0：value 是回路开关状态（0=关，0-100之间=开），更新回路状态
     * - DataType=2：value 是电流信息（A），更新回路电流字段 electric_current
     * - DataType 缺省：兼容老消息，按回路状态处理
     */
    @RabbitListener(queues = LightingMqConstant.QUEUE_LIGHTING_LISTENER_BQ, ackMode = "AUTO")
    public void bqStatusListener(Message message){
        String body = new String(message.getBody());
        try {
//            log.info("【北区】收到状态消息：{}", body);
            JSONObject msg = JSONObject.parseObject(body);

            String gatewayCode = msg.getString("GatewayCode");
            String circuitCode = msg.getString("CircuitCode");
            String value = msg.getString("Value");
            String dataType = msg.getString("DataType");

            if(StringUtils.isEmpty(gatewayCode) || StringUtils.isEmpty(circuitCode) || StringUtils.isEmpty(value)){
                log.warn("【北区】状态消息参数不完整，跳过");
                return;
            }

            // 拼成 circuit_code：GatewayCode + "-" + CircuitCode
            String fullCircuitCode = gatewayCode + "-" + circuitCode;

            // 根据 circuit_code 查询回路
            LambdaQueryWrapper<LightingCircuit> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LightingCircuit::getCircuitCode, fullCircuitCode);
            LightingCircuit circuit = circuitService.getOne(wrapper);

            if(circuit == null){
//                log.warn("【北区】未找到对应的回路，circuit_code={}", fullCircuitCode);
                return;
            }

            // DataType=1：value 是电流信息，只更新电流字段
            if("1".equals(dataType)){
                try {
                    double current = Double.parseDouble(value.trim());
                    circuit.setElectricCurrent(current);
                    circuitService.updateById(circuit);
//                    log.info("【北区】更新回路电流：circuit_code={}, current={}A", fullCircuitCode, current);
                } catch (NumberFormatException e) {
                    log.warn("【北区】电流值异常，未更新：circuit_code={}, value={}", fullCircuitCode, value);
                }
                return;
            }

            // DataType=0 或缺省：value 是回路开关状态，和老逻辑一致：0=关，0-100之间=开（统一设为100）
            boolean isValidNumber = false;
            String status = "";
            try {
                int numValue = Integer.parseInt(value.trim());
                isValidNumber = numValue >= 0 && numValue <= 100;
                if(numValue == 0){
                    status = LightingCircuit.STATUS_OFF;
                }else if(numValue > 0 && numValue <= 100){
                    status = LightingCircuit.STATUS_ON;
                }
            } catch (NumberFormatException e) {
                isValidNumber = false;
            }

            if(isValidNumber){
                // 更新回路状态（同时维护开启/关闭时间、开启总时长）
                circuitService.applyStatus(circuit, status);

                // 通过 area_id 查区域，拿到 space
                LightingArea area = areaService.getById(circuit.getAreaId());
                String space = area != null ? area.getSpace() : "";

                // 更新通讯状态为在线
                circuitService.updateComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode(), LightingCircuit.COMSTAT_ONLINE);

                // 发送离线延迟消息
                sendService.sendLightingCircuitComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode());

//                log.info("【北区】更新回路状态：circuit_code={}, status={}", fullCircuitCode, status);
            }else{
                // 状态异常，设置为离线
                LightingArea area = areaService.getById(circuit.getAreaId());
                String space = area != null ? area.getSpace() : "";
                circuitService.updateComstat(space, String.valueOf(circuit.getAreaId()), circuit.getCircuitCode(), LightingCircuit.COMSTAT_OFFLINE);
                log.warn("【北区】状态值异常，设置为离线：circuit_code={}, value={}", fullCircuitCode, value);
            }
        } catch (Exception e) {
            log.error("【北区】状态消息处理异常", e);
        }
    }
}
