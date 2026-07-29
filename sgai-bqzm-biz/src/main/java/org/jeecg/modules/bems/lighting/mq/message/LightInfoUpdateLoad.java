package org.jeecg.modules.bems.lighting.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LightInfoUpdateLoad {

    /**
     * 场景
     */
    public static final String DATA_TYPE_Scene = "3";

    /**
     * 亮度
     */
    public static final String DATA_TYPE_Brightness = "0";

    /**
     * 网关地址。金安桥：1；一高炉：2
     */
    private String GatewayCode;

    /**
     * 亮度
     * 电
     * 电压
     * 场景
     */
    private String DataType;

    private int AreaID;

    private String CircuitCode;

    private String Value;

    /**
     * 场景控制
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaId 区域id
     * @param value 场景地址
     * @return 场景控制消息内容
     */
    public static LightInfoUpdateLoad sceneControl(String space,int areaId,String value){
        return new LightInfoUpdateLoad(space,DATA_TYPE_Scene,areaId,null,value);
    }

    /**
     * 回路控制
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaId 区域id
     * @param circuitCode 回路编号
     * @param value 亮度值
     * @return 回路亮度控制消息内容
     */
    public static LightInfoUpdateLoad circuitControl(String space,int areaId,String circuitCode,String value){
        return new LightInfoUpdateLoad(space,DATA_TYPE_Brightness,areaId,circuitCode,value);
    }
}
