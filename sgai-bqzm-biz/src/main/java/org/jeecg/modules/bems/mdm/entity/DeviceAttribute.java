package org.jeecg.modules.bems.mdm.entity;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.entity.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备属性
 */
@TableName("device_attribute")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceAttribute extends BaseEntity {

    /**
     * 读写级别：只读
     */
    public static final String READWRITE_LEVEL_READ = "0";
    /**
     * 读写级别：读写
     */
    public static final String READWRITE_LEVEL_WRITE = "1";

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 属性名
     */
    private String attributeName;

    /**
     * 属性编码
     */
    private String attributeCode;

    /**
     * 单位
     */
    private String unit;

    /**
     * 读写等级 只读：1；读写：0
     */
    private String readwriteLevel;

    /**
     * 排序字段
     */
    private Integer sort;

    /**
     * 采集值
     */
    private String value;

    /**
     * 采集时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gatherTime;

    /**
     * 采集编码
     */
    private String acquisitionCoding;

    /**
     * 属性值类型
     */
    private String valueType;
    /**
     * 属性值配置
     */
    private String valueConfig;

    public static DeviceAttribute convert(DeviceModelAttribute data){
        DeviceAttribute res = new DeviceAttribute();
        res.setAttributeName(data.getAttributeName());
        res.setAttributeCode(data.getAttributeCode());
        res.setUnit(data.getUnit());
        res.setSort(data.getSort());
        res.setReadwriteLevel(data.getReadwriteLevel());
        res.setValueType(data.getValueType());
        res.setValueConfig(data.getValueConfig());
        return res;
    }

    public String convertValue(String value){
        try{
            if(StringUtils.isEmpty(valueConfig)){
                return value;
            }
            List<ValueConfigEntity> valueConfigEntities = JSONArray.parseArray(valueConfig, ValueConfigEntity.class);
            for(ValueConfigEntity entity : valueConfigEntities){
                if(entity.getKey().equals(value)){
                    return entity.getValue();
                }
            }
            return value;
        }catch (Exception e){
            return value;
        }
    }

    /**
     * 设备关联请求参数
     */
    @Data
    public static class ValueConfigEntity {
        private String key;
        private String value;
    }
}
