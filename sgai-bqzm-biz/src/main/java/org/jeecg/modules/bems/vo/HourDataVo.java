package org.jeecg.modules.bems.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.entity.HourData;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class HourDataVo extends HourData {

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 修正值
     */
    private BigDecimal updValue;
    private String updValueStr;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    public static HourDataVo convert(HourData data){
        HourDataVo res = new HourDataVo();
        res.setId(data.getId());
        res.setDeviceId(data.getDeviceId());
        res.setTime(data.getTime());
        res.setValue(data.getValue());
        res.setStartValue(data.getStartValue());
        res.setEndValue(data.getEndValue());
        res.setUpdateTime(data.getUpdateTime());
        res.setUpdateBy(data.getUpdateBy());
        res.setComputeValue(data.getComputeValue());
        res.setStartTime(data.getTime());
        res.setEndTime(data.getTime().plusHours(1));
        res.setUpdValue(getUpdValue(data.getComputeValue(), data.getValue()));
        if(res.getUpdValue() == null || res.getUpdValue().compareTo(BigDecimal.ZERO) == 0){
            res.setUpdValueStr("-");
        }else{
            res.setUpdValueStr(res.getUpdValue().toPlainString());
        }
        return res;
    }

    private static BigDecimal getUpdValue(BigDecimal computeValue, BigDecimal value){
        // 计算差值,两个值可能为null
        BigDecimal a = computeValue == null ? BigDecimal.ZERO : computeValue;
        BigDecimal b = value == null ? BigDecimal.ZERO : value;
        return b.subtract(a);
    }

}
