package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.lighting.entity.LightingControlLog;
import org.jeecg.modules.bems.lighting.mapper.LightingControlLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingControlLogService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class LightingControlLogServiceImpl extends ServiceImpl<LightingControlLogMapper, LightingControlLog> implements ILightingControlLogService {

    @Override
    public IPage<LightingControlLog> listPage(LightingControlLog params, int pageNo, int pageSize) {
        LambdaQueryWrapper<LightingControlLog> queryWrapper = new LambdaQueryWrapper<LightingControlLog>()
                .eq(StringUtils.isNotEmpty(params.getControlType()), LightingControlLog::getControlType, params.getControlType())
                .eq(StringUtils.isNotEmpty(params.getOperation()), LightingControlLog::getOperation, params.getOperation())
                .eq(StringUtils.isNotEmpty(params.getOperatorType()), LightingControlLog::getOperatorType, params.getOperatorType())
                .like(StringUtils.isNotEmpty(params.getRelName()), LightingControlLog::getRelName, params.getRelName())
                .like(StringUtils.isNotEmpty(params.getOperatorName()), LightingControlLog::getOperatorName, params.getOperatorName())
                .orderByDesc(LightingControlLog::getControlTime);
        return super.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public void saveLog(String controlType, Long relId, String relName, String operation, String operatorType, String operatorBy, String result) {
        LightingControlLog log = new LightingControlLog();
        log.setControlTime(LocalDateTime.now());
        log.setControlType(controlType);
        log.setRelId(relId);
        log.setRelName(relName);
        log.setOperation(operation);
        log.setOperatorType(operatorType);
        if ("手动".equals(operatorType)) {
            String username = getCurrentUsername();
            log.setOperatorBy(username);
            log.setOperatorName(username);
            log.setIpAddress(getIpAddr());
        } else {
            log.setOperatorBy("system");
            log.setOperatorName("系统自动");
        }
        log.setResult(result);
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
