package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.lighting.entity.LightingConfigLog;
import org.jeecg.modules.bems.lighting.mapper.LightingConfigLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingConfigLogService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class LightingConfigLogServiceImpl extends ServiceImpl<LightingConfigLogMapper, LightingConfigLog> implements ILightingConfigLogService {

    @Override
    public IPage<LightingConfigLog> listPage(LightingConfigLog params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingConfigLog> queryWrapper = new LambdaQueryWrapper<LightingConfigLog>()
                .eq(StringUtils.isNotEmpty(params.getOperType()), LightingConfigLog::getOperType, params.getOperType())
                .eq(StringUtils.isNotEmpty(params.getOperModule()), LightingConfigLog::getOperModule, params.getOperModule())
                .like(StringUtils.isNotEmpty(params.getOperName()), LightingConfigLog::getOperName, params.getOperName())
                .like(StringUtils.isNotEmpty(params.getTargetName()), LightingConfigLog::getTargetName, params.getTargetName())
                .orderByDesc(LightingConfigLog::getOperTime);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public void saveLog(String operType, String operModule, String targetType, Long targetId, String targetName, String operContent) {
        LightingConfigLog log = new LightingConfigLog();
        log.setOperTime(LocalDateTime.now());
        log.setOperType(operType);
        log.setOperModule(operModule);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setOperContent(operContent);
        // 获取当前用户
        String username = getCurrentUsername();
        log.setOperBy(username);
        log.setOperName(username);
        // 获取IP地址
        log.setIpAddress(getIpAddr());
        super.save(log);
    }

    private String getCurrentUsername() {
        try {
            org.apache.shiro.subject.Subject subject = org.apache.shiro.SecurityUtils.getSubject();
            if (subject != null && subject.getPrincipal() != null) {
                Object principal = subject.getPrincipal();
                // 尝试获取用户名
                java.lang.reflect.Method method = principal.getClass().getMethod("getUsername");
                Object result = method.invoke(principal);
                return result != null ? result.toString() : "system";
            }
        } catch (Exception e) {
            // ignore
        }
        return "system";
    }

    private HttpServletRequest getRequest() {
        try {
            return org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes() != null ?
                    ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getIpAddr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "127.0.0.1";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
