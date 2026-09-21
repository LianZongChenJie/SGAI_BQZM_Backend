package org.jeecg.modules.bems.lighting.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dm.jdbc.util.StringUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;

import org.jeecg.modules.bems.constant.BusinessConfigConstant;
import org.jeecg.modules.bems.lighting.entity.BusinessConfig;
import org.jeecg.modules.bems.lighting.mapper.BusinessConfigMapper;
import org.jeecg.modules.bems.lighting.service.IBusinessConfigService;
import org.jeecg.modules.bems.lighting.service.ILightingConfigLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class BusinessConfigServiceImpl extends ServiceImpl<BusinessConfigMapper, BusinessConfig> implements IBusinessConfigService {

    /** 配置日志内容换行符（与照明配置日志口径一致：动作行 + 每字段一行） */
    private static final String LINE = "\n";

    /** 配置日志（开关类配置变更留痕） */
    private final ILightingConfigLogService lightingConfigLogService;

    @Override
    public void updateByKey(String key, String value) {
        List<BusinessConfig> list = list(new LambdaQueryWrapper<BusinessConfig>().eq(BusinessConfig::getConfigKey, key));
        if(CollectionUtils.isEmpty(list)){
            throw new JeecgBootException("未找到对应的配置项");
        }
        String oldValue = list.get(0).getConfigValue();
        update(new LambdaUpdateWrapper<BusinessConfig>().eq(BusinessConfig::getConfigKey,key).set(BusinessConfig::getConfigValue, value));
        // 影响全局的开关类配置：改动写配置日志（值没变时不记，避免噪音）
        writeAuditLog(key, list.get(0), oldValue, value);
        // 刷新缓存
    }

    /**
     * 开关类配置的变更留痕。
     * 目前只覆盖"计划持续验证开关"（plan:verify:enabled）——它影响所有计划，改错了后果大，必须可追溯；
     * 其余配置项（统计数值、场景 id 等）不记录，避免配置日志被刷屏。
     */
    private void writeAuditLog(String key, BusinessConfig config, String oldValue, String newValue) {
        if (!BusinessConfigConstant.PLAN_VERIFY_ENABLED.equals(key) || Objects.equals(oldValue, newValue)) {
            return;
        }
        String label = StringUtils.isNotEmpty(config.getName()) ? config.getName() : key;
        lightingConfigLogService.saveLog("修改", "系统配置", "业务配置", config.getId(), label,
                "修改系统配置" + LINE + label + "：" + display(oldValue) + " → " + display(newValue));
    }

    /**
     * 空值展示为"空"（与照明配置日志口径一致）
     */
    private static String display(String value) {
        return StringUtils.isBlank(value) ? "空" : value;
    }

    @Override
    public String getValueByKey(String key) {
        BusinessConfig one = getOne(new LambdaQueryWrapper<BusinessConfig>().eq(BusinessConfig::getConfigKey, key));
        return one == null ? "" : one.getConfigValue();
    }

    @Override
    public Long getLongByKey(String key) {
        String valueByKey = getValueByKey(key);
        if(StringUtils.isEmpty(valueByKey)){
            return null;
        }
        return Long.valueOf(valueByKey);
    }

    @Override
    public <T> List<T> getListByKey(String key, Class<T> clazz) {
        String value = getValueByKey(key);
        if(StringUtil.isEmpty(value)){
            return null;
        }
        return JSONArray.parseArray(value, clazz);
    }

    @Override
    public <T> T getObjectByKey(String key, Class<T> clazz) {
        String value = getValueByKey(key);
        if(StringUtil.isEmpty(value)){
            return null;
        }
        return JSONObject.parseObject(value, clazz);
    }
}
