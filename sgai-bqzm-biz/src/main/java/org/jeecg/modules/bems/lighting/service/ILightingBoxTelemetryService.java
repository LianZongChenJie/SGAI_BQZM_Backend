package org.jeecg.modules.bems.lighting.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;

import java.util.Date;
import java.util.List;

/**
 * 箱子遥测服务
 */
public interface ILightingBoxTelemetryService extends IService<LightingBoxTelemetry> {

    /**
     * 接收箱子遥测消息，更新快照表 + 插历史表
     * @param msg 单条 JSON 消息（DataType=7）
     */
    void saveTelemetry(JSONObject msg);

    /**
     * 箱子列表（最新快照）
     */
    List<LightingBoxTelemetry> listBoxes();

    /**
     * 单个箱子历史数据
     * @param gatewayCode 箱子编号
     * @param start 开始时间
     * @param end 结束时间
     */
    List<LightingBoxTelemetryHistory> history(String gatewayCode, Date start, Date end);
}