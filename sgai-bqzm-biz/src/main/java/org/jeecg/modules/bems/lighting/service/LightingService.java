package org.jeecg.modules.bems.lighting.service;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LightingService {

    private final LightingSendService sendService;

    /**
     * 区域-全开
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     */
    public void areaOpen(String space,String areaCode,String value){
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.sceneControl(space,Integer.valueOf(areaCode), value);
        sendService.send(msg);
    }

    /**
     * 区域-全关
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     */
    public void areaClose(String space,String areaCode,String value){
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.sceneControl(space,Integer.valueOf(areaCode), value);
        sendService.send(msg);
    }

    /**
     * 区域下回路-开启
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     * @param circuitCode 回路编码
     */
    public void circuitOpen(String space,String areaCode,String circuitCode){
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.circuitControl(space,Integer.valueOf(areaCode), circuitCode, "100");
        sendService.send(msg);
    }

    /**
     * 区域下回路-关闭
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     * @param circuitCode 回路编码
     */
    public void circuitClose(String space,String areaCode,String circuitCode){
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.circuitControl(space,Integer.valueOf(areaCode), circuitCode, "0");
        sendService.send(msg);
    }

}
