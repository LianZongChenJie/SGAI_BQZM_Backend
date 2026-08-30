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
import org.jeecg.modules.bems.lighting.entity.LightingEnergyRead;
import org.jeecg.modules.bems.lighting.mq.constant.LightingMqConstant;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.jeecg.modules.bems.lighting.mq.message.PowerBoxData;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingBoxTelemetryService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingEnergyReadService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanExecuteLogService;
import org.jeecg.modules.bems.lighting.service.ILightingPlanService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RabbitComponent(value = "LightingMsgListener")
@Slf4j
@AllArgsConstructor
public class LightingMsgListener {

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    private final ILightingPlanService planService;

    private final LightingSendService sendService;

    private final ILightingPlanExecuteLogService planExecuteLogService;

    private final ILightingEnergyReadService energyReadService;

    private final ILightingBoxTelemetryService boxTelemetryService;

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
            log.info("【公区】收到状态消息：{}", body);
            // 新消息格式为数组：[{"DataType":"1","CircuitCode":"1","Value":"0","GatewayCode":"15"}, ...]，兼容单对象老消息
            List<JSONObject> msgList = new java.util.ArrayList<>();
            String trimmed = body.trim();
            if(trimmed.startsWith("[")){
                msgList = JSONObject.parseArray(trimmed, JSONObject.class);
            } else {
                msgList.add(JSONObject.parseObject(trimmed));
            }
            if(msgList == null || msgList.isEmpty()){
                log.warn("【公区】状态消息数组为空，跳过");
                return;
            }
            for(JSONObject msg : msgList){
                try {
                    handleBqStatusMessage(msg);
                } catch (Exception e) {
                    log.error("【公区】单条状态消息处理异常，继续下一条：msg={}", msg.toJSONString(), e);
                }
            }
        } catch (Exception e) {
            log.error("【公区】状态消息处理异常", e);
        }
    }

    /**
     * 处理单条北区（space=903）状态消息
     * 消息按 DataType 分流：
     * - DataType=1：value 是电流信息（A），更新回路电流字段 electric_current
     * - DataType=0 或缺省：value 是回路开关状态（0=关，0-100之间=开），更新回路状态
     */
    private void handleBqStatusMessage(JSONObject msg){
        String gatewayCode = msg.getString("GatewayCode");
        String circuitCode = msg.getString("CircuitCode");
        String value = msg.getString("Value");
        String dataType = msg.getString("DataType");

        // 特殊处理：GatewayCode=54 + DataType=3（904新灯控的区域整体开关状态消息，无 CircuitCode），只更新区域自身状态
        if("54".equals(gatewayCode) && "3".equals(dataType)){
            handleBqAreaStatus(msg);
            return;
        }

        // 特殊处理：GatewayCode=54 + DataType=0（904新灯控的回路状态消息），按 space=904 + AreaID(area_code) + CircuitCode 匹配回路
        if("54".equals(gatewayCode) && "0".equals(dataType)){
            handleBqCircuitStatus(msg);
            return;
        }

        // 特殊处理：GatewayCode=154.100 + DataType=0（905新灯控的状态消息，无 CircuitCode）
        // 消息格式：{"DataType":"0","AreaID":"42","Value":"1","GatewayCode":"154.100"}
        // AreaID=回路编码(circuit_code)；Value：0=开、1=关
        if("154.100".equals(gatewayCode) && "0".equals(dataType)){
            handle905StatusMessage(msg);
            return;
        }

        // 特殊处理：GatewayCode=154.2 + DataType=0（906新灯控的状态消息，无 CircuitCode）
        // 消息格式：{"DataType":"0","AreaID":"2","Value":"1","GatewayCode":"154.2"}
        // AreaID=回路编码(circuit_code)；Value：1=开、2=关
        if("154.2".equals(gatewayCode) && "0".equals(dataType)){
            handle906StatusMessage(msg);
            return;
        }

        // DataType=6 电量累计值（kWh）：累计电量已由 DataType=7 箱子遥测(TotalEnergy)承担，
        // lighting_energy_read 表不再写入（历史数据保留），此处直接跳过，避免重复采集
        if("6".equals(dataType)){
            log.debug("【能耗】DataType=6 电量消息已由箱子遥测(DataType=7)承担，跳过写入 lighting_energy_read，gatewayCode={}", gatewayCode);
            return;
        }

        // DataType=7：箱子(电箱)遥测数据（交流电压/电流/功率/电量）
        // 箱子级数据无 CircuitCode/Value，独立处理，不影响其他 DataType 的分流
        if("7".equals(dataType)){
            try {
                boxTelemetryService.saveTelemetry(msg);
            } catch (Exception e) {
                log.error("【箱子遥测】处理失败: {}", msg.toJSONString(), e);
            }
            return;
        }

        if(StringUtils.isEmpty(gatewayCode) || StringUtils.isEmpty(circuitCode) || StringUtils.isEmpty(value)){
            log.warn("【公区】状态消息参数不完整，跳过");
            return;
        }

        // 拼成 circuit_code：GatewayCode + "-" + CircuitCode
        String fullCircuitCode = gatewayCode + "-" + circuitCode;

        // 查询回路：
        // 1) GatewayCode 在 11-44 范围：先按 space=903 + area_code=10.22.160.{GatewayCode} 查区域，
        //    再用 area_id + circuit_code=GatewayCode-CircuitCode 查回路
        // 2) 其他网关：直接用 circuit_code 全局匹配（老逻辑）
        LightingCircuit circuit = null;
        int gw = -1;
        try {
            gw = Integer.parseInt(gatewayCode.trim());
        } catch (NumberFormatException ignored) {
        }
        if (gw >= 11 && gw <= 44) {
            LightingArea area = areaService.getByCode("903", "10.22.160." + gatewayCode);
            if (area == null) {
                log.warn("【公区】未找到对应的区域，space=903, area_code=10.22.160.{}", gatewayCode);
                return;
            }
            circuit = circuitService.getOne(new LambdaQueryWrapper<LightingCircuit>()
                    .eq(LightingCircuit::getAreaId, area.getId())
                    .eq(LightingCircuit::getCircuitCode, fullCircuitCode));
        } else {
            circuit = circuitService.getOne(new LambdaQueryWrapper<LightingCircuit>()
                    .eq(LightingCircuit::getCircuitCode, fullCircuitCode));
        }

        if(circuit == null){
                log.warn("【公区】未找到对应的回路，circuit_code={}", fullCircuitCode);
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

            // 通过 area_id 查区域，拿到 space 和 area_code
            LightingArea area = areaService.getById(circuit.getAreaId());
            String space = area != null ? area.getSpace() : "";
            // updateComstat 内部按 area_code 字段匹配区域，故传入区域编码（不能传主键 id）
            String areaCode = area != null && area.getAreaCode() != null ? area.getAreaCode() : String.valueOf(circuit.getAreaId());

            // 更新通讯状态为在线
            circuitService.updateComstat(space, areaCode, circuit.getCircuitCode(), LightingCircuit.COMSTAT_ONLINE);

            // 发送离线延迟消息（areaCode 须传真实区域编码，与上方 updateComstat 一致，否则离线消费时按编码匹配不到区域）
            sendService.sendLightingCircuitComstat(space, areaCode, circuit.getCircuitCode());

//                log.info("【北区】更新回路状态：circuit_code={}, status={}", fullCircuitCode, status);
        }else{
            // 状态异常，设置为离线
            LightingArea area = areaService.getById(circuit.getAreaId());
            String space = area != null ? area.getSpace() : "";
            // updateComstat 内部按 area_code 字段匹配区域，故传入区域编码（不能传主键 id）
            String areaCode = area != null && area.getAreaCode() != null ? area.getAreaCode() : String.valueOf(circuit.getAreaId());
            circuitService.updateComstat(space, areaCode, circuit.getCircuitCode(), LightingCircuit.COMSTAT_OFFLINE);
            log.warn("【北区】状态值异常，设置为离线：circuit_code={}, value={}", fullCircuitCode, value);
        }
    }

    /**
     * 特殊处理：DataType=6 电量累计值（kWh），每分钟一条，存入能耗分钟表 lighting_energy_read
     * 消息格式：{"GatewayCode":"12","CircuitCode":"1","Value":"100.5","DataType":"6"}
     * CircuitCode 可缺省（=网关级总表读数）；尽量解析到回路/区域，解析不到也照存（归入"待确认映射"）
     */
    private void handleEnergyMessage(JSONObject msg){
        String gatewayCode = msg.getString("GatewayCode");
        String circuitCode = msg.getString("CircuitCode");
        String areaIdStr = msg.getString("AreaID");
        String value = msg.getString("Value");
        if(StringUtils.isEmpty(gatewayCode) || StringUtils.isEmpty(value)){
            log.warn("【能耗】电量消息参数不完整，跳过：msg={}", msg.toJSONString());
            return;
        }
        BigDecimal reading;
        try {
            reading = new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("【能耗】电量值异常，跳过：msg={}", msg.toJSONString());
            return;
        }
        if(reading.signum() < 0){
            log.warn("【能耗】电量值为负，跳过：msg={}", msg.toJSONString());
            return;
        }

        // 电量按区域归属（无回路）：GatewayCode 取 lighting_area.area_code 小数点后的末段值
        // 目前只有 space=903 的区域有数值（如 area_code=xxx.12 → GatewayCode=12）
        LightingArea area = resolveEnergyAreaByGateway(gatewayCode);
        Long areaId = null;
        String areaCode = null;
        String space = null;
        if(area != null){
            areaId = area.getId();
            areaCode = area.getAreaCode();
            space = area.getSpace();
        }

        LightingEnergyRead read = new LightingEnergyRead();
        read.setGatewayCode(gatewayCode);
        read.setCircuitCode(null);
        read.setAreaId(areaId);
        read.setAreaCode(areaCode);
        read.setSpace(space);
        read.setValue(reading);
        read.setReadTime(LocalDateTime.now());
        energyReadService.save(read);
    }

    /**
     * 按网关号解析电量所属区域：
     * GatewayCode 取 lighting_area.area_code 小数点后的末段值（如 area_code=xxx.12 → GatewayCode=12），
     * 目前只有 space=903 的区域有该格式的数值；area_code 无小数点时兜底按整体等于网关号匹配。
     */
    private LightingArea resolveEnergyAreaByGateway(String gatewayCode){
        if(StringUtils.isEmpty(gatewayCode)){
            return null;
        }
        List<LightingArea> areaList = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getSpace, "903")
                .isNotNull(LightingArea::getAreaCode));
        if(areaList == null || areaList.isEmpty()){
            return null;
        }
        LightingArea match = null;
        for(LightingArea area : areaList){
            String code = area.getAreaCode();
            if(StringUtils.isEmpty(code)){
                continue;
            }
            int dot = code.lastIndexOf('.');
            String tail = dot >= 0 ? code.substring(dot + 1).trim() : code.trim();
            if(gatewayCode.equals(tail)){
                match = area;
                break;
            }
        }
        if(match == null){
            log.warn("【能耗】网关 {} 未匹配到 903 区域（area_code 小数点后末段无对应）", gatewayCode);
        }
        return match;
    }

    /**
     * 特殊处理：GatewayCode=54 + DataType=0 的904回路状态消息（带 CircuitCode 字段）
     * 消息格式：{"GatewayCode":"54","DataType":"0","AreaID":"107","CircuitCode":"5","Value":"100"}
     * 按 space=904 + AreaID(area_code) + CircuitCode 精确匹配回路；
     * value：100=开，0=关；更新回路状态（同时维护开关时间、开启总时长）及通讯状态
     */
    private void handleBqCircuitStatus(JSONObject msg){
        String gatewayCode = msg.getString("GatewayCode");
        String areaIdStr = msg.getString("AreaID");
        String circuitCode = msg.getString("CircuitCode");
        String value = msg.getString("Value");
        if(StringUtils.isEmpty(areaIdStr) || StringUtils.isEmpty(circuitCode) || StringUtils.isEmpty(value)){
            log.warn("【公区904】回路状态消息参数不完整，跳过：msg={}", msg.toJSONString());
            return;
        }
        // 解析状态：100=开，0=关
        String status;
        try {
            int numValue = Integer.parseInt(value.trim());
            if(numValue == 100){
                status = LightingCircuit.STATUS_ON;
            }else if(numValue == 0){
                status = LightingCircuit.STATUS_OFF;
            }else{
                log.warn("【公区904】回路状态值异常，跳过：msg={}", msg.toJSONString());
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("【公区904】回路状态值异常，跳过：msg={}", msg.toJSONString());
            return;
        }

        // 按 space=904 + AreaID(area_code) 查区域（904 消息的 AreaID 是 area_code 而非主键）
        LightingArea area = areaService.getByCode("904", areaIdStr);
        if(area == null){
            log.warn("【公区904】未找到对应的区域，areaId={}, gatewayCode={}", areaIdStr, gatewayCode);
            return;
        }
        // 按 areaId + CircuitCode 查回路
        LightingCircuit circuit = circuitService.getOne(new LambdaQueryWrapper<LightingCircuit>()
                .eq(LightingCircuit::getAreaId, area.getId())
                .eq(LightingCircuit::getCircuitCode, circuitCode));
        if(circuit == null){
            log.warn("【公区904】未找到对应的回路，areaId={}, circuitCode={}", areaIdStr, circuitCode);
            return;
        }

        // 更新回路状态（同时维护开启/关闭时间、开启总时长）
        circuitService.applyStatus(circuit, status);

        // 更新通讯状态为在线
        circuitService.updateComstat("904", areaIdStr, circuitCode, LightingCircuit.COMSTAT_ONLINE);

        // 发送离线延迟消息
        sendService.sendLightingCircuitComstat("904", areaIdStr, circuitCode);

        log.info("【公区904】回路状态更新完成：areaId={}, circuitCode={}, status={}", areaIdStr, circuitCode, status);
    }

    /**
     * 特殊处理：GatewayCode=154.100 + DataType=0 的905新灯控状态消息（无 CircuitCode 字段）
     * 消息格式：{"DataType":"0","AreaID":"42","Value":"1","GatewayCode":"154.100"}
     * AreaID=区域下唯一回路的回路编码（circuit_code）；Value：0=开、1=关
     * 先按 area_code=GatewayCode 查询区域得到 id 集合，再按 area_id IN (id集合) + circuit_code=AreaID
     * 匹配回路，更新回路状态（同时维护开关时间、开启总时长）及通讯状态
     */
    private void handle905StatusMessage(JSONObject msg){
        String gatewayCode = msg.getString("GatewayCode");
        String areaIdStr = msg.getString("AreaID");
        String value = msg.getString("Value");
        if(StringUtils.isEmpty(areaIdStr) || StringUtils.isEmpty(value)){
            log.warn("【公区905】状态消息参数不完整，跳过：msg={}", msg.toJSONString());
            return;
        }
        // 解析状态：0=开、1=关（905 控制值映射：0=全开、1=全关）
        String status;
        try {
            int numValue = Integer.parseInt(value.trim());
            if(numValue == 0){
                status = LightingCircuit.STATUS_ON;
            }else if(numValue == 1){
                status = LightingCircuit.STATUS_OFF;
            }else{
                log.warn("【公区905】状态值异常，跳过：msg={}", msg.toJSONString());
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("【公区905】状态值异常，跳过：msg={}", msg.toJSONString());
            return;
        }

        // 用 gatewayCode(154.100) 作为 area_code 查询区域列表，拿到 id 集合
        List<LightingArea> areaList = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getAreaCode, gatewayCode));
        if(areaList == null || areaList.isEmpty()){
            log.warn("【公区905】未找到 area_code={} 对应的区域，跳过：msg={}", gatewayCode, msg.toJSONString());
            return;
        }
        List<Long> areaIds = areaList.stream().map(LightingArea::getId).collect(Collectors.toList());

        // AreaID 是回路编码，按 area_id IN (区域id集合) + circuit_code=AreaID 匹配回路
        LightingCircuit circuit = circuitService.getOne(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds)
                .eq(LightingCircuit::getCircuitCode, areaIdStr));
        if(circuit == null){
            log.warn("【公区905】未找到对应的回路，AreaID(circuit_code)={}, gatewayCode={}", areaIdStr, gatewayCode);
            return;
        }
        LightingArea area = areaService.getById(circuit.getAreaId());

        // 更新通讯状态为在线（与状态一起落库，避免 updateComstat 按 area_code 匹配不到）
        circuit.setComstat(LightingCircuit.COMSTAT_ONLINE);

        // 更新回路状态（同时维护开启/关闭时间、开启总时长、通讯状态）
        circuitService.applyStatus(circuit, status);

        log.info("【公区905】回路状态更新完成：AreaID={}, circuit_code={}, areaName={}, status={}, comstat={}",
                areaIdStr, circuit.getCircuitCode(), area != null ? area.getAreaName() : null, status, circuit.getComstat());
    }

    /**
     * 特殊处理：GatewayCode=154.2 + DataType=0 的906新灯控状态消息（无 CircuitCode 字段）
     * 消息格式：{"DataType":"0","AreaID":"2","Value":"1","GatewayCode":"154.2"}
     * AreaID=区域下唯一回路的回路编码（circuit_code）；Value：1=开、2=关
     * 按 space=906 + circuit_code=AreaID 匹配回路，更新回路状态（同时维护开关时间、开启总时长）及通讯状态
     */
    private void handle906StatusMessage(JSONObject msg){
        String gatewayCode = msg.getString("GatewayCode");
        String areaIdStr = msg.getString("AreaID");
        String value = msg.getString("Value");
        if(StringUtils.isEmpty(areaIdStr) || StringUtils.isEmpty(value)){
            log.warn("【公区906】状态消息参数不完整，跳过：msg={}", msg.toJSONString());
            return;
        }
        // 解析状态：1=开、2=关（906 控制值映射：1=开、2=关）
        String status;
        try {
            int numValue = Integer.parseInt(value.trim());
            if(numValue == 1){
                status = LightingCircuit.STATUS_ON;
            }else if(numValue == 2){
                status = LightingCircuit.STATUS_OFF;
            }else{
                log.warn("【公区906】状态值异常，跳过：msg={}", msg.toJSONString());
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("【公区906】状态值异常，跳过：msg={}", msg.toJSONString());
            return;
        }

        // 用 gatewayCode(154.2) 作为 area_code 查询区域列表，拿到 id 集合
        List<LightingArea> areaList = areaService.list(new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getAreaCode, gatewayCode));
        if(areaList == null || areaList.isEmpty()){
            log.warn("【公区906】未找到 area_code={} 对应的区域，跳过：msg={}", gatewayCode, msg.toJSONString());
            return;
        }
        List<Long> areaIds = areaList.stream().map(LightingArea::getId).collect(Collectors.toList());

        // AreaID 是回路编码，按 area_id IN (区域id集合) + circuit_code=AreaID 匹配回路
        LightingCircuit circuit = circuitService.getOne(new LambdaQueryWrapper<LightingCircuit>()
                .in(LightingCircuit::getAreaId, areaIds)
                .eq(LightingCircuit::getCircuitCode, areaIdStr));
        if(circuit == null){
            log.warn("【公区906】未找到对应的回路，AreaID(circuit_code)={}, gatewayCode={}", areaIdStr, gatewayCode);
            return;
        }
        LightingArea area = areaService.getById(circuit.getAreaId());

        // 更新通讯状态为在线（与状态一起落库，避免 updateComstat 按 area_code 匹配不到）
        circuit.setComstat(LightingCircuit.COMSTAT_ONLINE);

        // 更新回路状态（同时维护开启/关闭时间、开启总时长、通讯状态）
        circuitService.applyStatus(circuit, status);

        log.info("【公区906】回路状态更新完成：AreaID={}, circuit_code={}, areaName={}, status={}, comstat={}",
                areaIdStr, circuit.getCircuitCode(), area != null ? area.getAreaName() : null, status, circuit.getComstat());
    }

    /**
     * 特殊处理：GatewayCode=54 + DataType=3 的区域整体开关状态消息（无 CircuitCode 字段）
     * 消息格式：{"DataType":3,"AreaID":102,"Value":"12","GatewayCode":"54"}
     * value：1=开，12=关；只更新区域自身状态（lighting_area.status），回路状态仍由各自的回路状态消息更新
     */
    private void handleBqAreaStatus(JSONObject msg){
        log.info("【公区904】收到状态消息：{}", msg.toJSONString(msg));
        String gatewayCode = msg.getString("GatewayCode");
        String areaIdStr = msg.getString("AreaID");
        String value = msg.getString("Value");
        if(StringUtils.isEmpty(areaIdStr) || StringUtils.isEmpty(value)){
            log.warn("【公区904】区域整体状态消息参数不完整，跳过：msg={}", msg.toJSONString());
            return;
        }
        // 解析状态：1=开，12=关
        String status;
        try {
            int numValue = Integer.parseInt(value.trim());
            if(numValue == 12){
                status = LightingCircuit.STATUS_OFF;
            }else if(numValue == 1){
                status = LightingCircuit.STATUS_ON;
            }else{
                log.warn("【公区904】区域整体状态值异常，跳过：msg={}", msg.toJSONString());
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("【公区904】区域整体状态值异常，跳过：msg={}", msg.toJSONString());
            return;
        }

        // 查询区域：优先按 area_code=AreaID 精确匹配（兼容老KNX逻辑 AreaID 即 area_code），查不到再按主键 id 匹配。
        // GatewayCode=54（904新灯控）时限定 space=904，避免 area_code 在其他空间重复导致匹配到错误区域
        LambdaQueryWrapper<LightingArea> areaWrapper = new LambdaQueryWrapper<LightingArea>()
                .eq(LightingArea::getAreaCode, areaIdStr);
        if("54".equals(gatewayCode)){
            areaWrapper.eq(LightingArea::getSpace, "904");
        }
        List<LightingArea> areaList = areaService.list(areaWrapper);
        LightingArea area = null;
        if(areaList != null && !areaList.isEmpty()){
            area = areaList.get(0);
            if(areaList.size() > 1){
                log.warn("【公区904】区域编码匹配到多条，取第一条：area_code={}, size={}, areaName={}", areaIdStr, areaList.size(), area.getAreaName());
            }
        }
        if(area == null){
            try {
                area = areaService.getById(Long.parseLong(areaIdStr));
                // 904新灯控：AreaID 是 area_code 而非主键，按主键兜底命中的必须是 904 空间，否则丢弃
                if(area != null && "54".equals(gatewayCode) && !"904".equals(area.getSpace())){
                    area = null;
                }
            } catch (NumberFormatException e) {
                area = null;
            }
        }
        if(area == null){
            log.warn("【公区904】未找到对应的区域，areaId={}, gatewayCode={}", areaIdStr, gatewayCode);
            return;
        }

        // 只更新区域自身状态，不更新回路（回路由各自的回路状态消息更新，避免覆盖回路各自的开关时间/时长累计）
        area.setStatus(status);
        areaService.updateById(area);
        log.info("【公区904】区域整体状态更新完成：gatewayCode={}, areaId={}, areaName={}, status={}",
                gatewayCode, areaIdStr, area.getAreaName(), status);
    }
}
