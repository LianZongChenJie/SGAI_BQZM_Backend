package org.jeecg.modules.bems.lighting.mq.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 泛光电箱数据
 * 四高炉灯控专用：接收小程序同步过来的泛光电箱状态
 */
@Data
public class PowerBoxData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备编号
     */
    private String deviceSn;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备回路名称，用@分隔
     */
    private String rdName;

    /**
     * 设备状态（2离线，1开，0关）
     */
    private Integer devicestate;
}
