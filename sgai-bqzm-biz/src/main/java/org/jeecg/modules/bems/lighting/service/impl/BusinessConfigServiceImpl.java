package org.jeecg.modules.bems.lighting.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dm.jdbc.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;

import org.jeecg.modules.bems.lighting.entity.BusinessConfig;
import org.jeecg.modules.bems.lighting.mapper.BusinessConfigMapper;
import org.jeecg.modules.bems.lighting.service.IBusinessConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class BusinessConfigServiceImpl extends ServiceImpl<BusinessConfigMapper, BusinessConfig> implements IBusinessConfigService {
    @Override
    public void updateByKey(String key, String value) {
        List<BusinessConfig> list = list(new LambdaQueryWrapper<BusinessConfig>().eq(BusinessConfig::getConfigKey, key));
        if(CollectionUtils.isEmpty(list)){
            throw new JeecgBootException("未找到对应的配置项");
        }
        update(new LambdaUpdateWrapper<BusinessConfig>().eq(BusinessConfig::getConfigKey,key).set(BusinessConfig::getConfigValue, value));
        // 刷新缓存
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
