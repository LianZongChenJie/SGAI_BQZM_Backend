package org.jeecg.modules.bems.lighting.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetry;
import org.jeecg.modules.bems.lighting.entity.LightingBoxTelemetryHistory;
import org.jeecg.modules.bems.lighting.vo.BoxTreeVo;

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

    /**
     * 片区-区域-箱子 三级树结构（供前端地图/列表树展示）
     * 第一层：片区；第二层：片区下的区域；第三层：区域下的箱子
     * 区域下无箱子时展示占位节点"箱1（无箱）"
     */
    List<BoxTreeVo> boxTree();
}