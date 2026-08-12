package org.jeecg.modules.bems.lighting.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.mq.message.LightInfoUpdateLoad;
import org.jeecg.modules.bems.lighting.mq.send.LightingSendService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class LightingService {

    private final LightingSendService sendService;

    /**
     * 区域-全开
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     */
    public void areaOpen(String space,String areaCode,String value){
        Integer areaId = parseAreaId(space, areaCode);
        if(areaId == null){
            return;
        }
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.sceneControl(space,areaId, value);
        sendService.send(msg);
    }

    /**
     * 区域-全关
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     */
    public void areaClose(String space,String areaCode,String value){
        Integer areaId = parseAreaId(space, areaCode);
        if(areaId == null){
            return;
        }
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.sceneControl(space,areaId, value);
        sendService.send(msg);
    }

    /**
     * 区域下回路-开启
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     * @param circuitCode 回路编码
     */
    public void circuitOpen(String space,String areaCode,String circuitCode){
        Integer areaId = parseAreaId(space, areaCode);
        if(areaId == null){
            return;
        }
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.circuitControl(space,areaId, circuitCode, "100");
        sendService.send(msg);
    }

    /**
     * 区域下回路-关闭
     * @param space 空间，金安桥：1；一高炉：2
     * @param areaCode 区域编码
     * @param circuitCode 回路编码
     */
    public void circuitClose(String space,String areaCode,String circuitCode){
        Integer areaId = parseAreaId(space, areaCode);
        if(areaId == null){
            return;
        }
        LightInfoUpdateLoad msg = LightInfoUpdateLoad.circuitControl(space,areaId, circuitCode, "0");
        sendService.send(msg);
    }

    /**
     * 校验区域编码必须为纯数字（老空间KNX通道），非数字（电箱设备编号等）跳过下发，避免 NumberFormatException
     */
    private Integer parseAreaId(String space, String areaCode){
        if(areaCode == null || !areaCode.matches("\\d+")){
            log.error("区域编码非纯数字，跳过KNX指令下发：space={}, areaCode={}", space, areaCode);
            return null;
        }
        return Integer.valueOf(areaCode);
    }

}
