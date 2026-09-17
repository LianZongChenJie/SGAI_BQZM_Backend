package org.jeecg.modules.bems.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.bems.lighting.entity.LightingConfigLog;
import org.jeecg.modules.bems.lighting.mapper.LightingConfigLogMapper;
import org.jeecg.modules.bems.lighting.service.ILightingConfigLogService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
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

    /**
     * 记录配置日志。
     * <p>
     * 只做"尽力而为"落库：内部吞掉所有异常，**日志失败绝不影响调用方的业务流程**。
     * 调用方（如场景新增/编辑/删除）通常在业务事务内，若这里抛异常逃逸，
     * 会导致业务回滚或接口报错——日志是旁路记录，不能拖垮主流程。
     */
    @Override
    public void saveLog(String operType, String operModule, String targetType, Long targetId, String targetName, String operContent) {
        try {
            LightingConfigLog configLog = new LightingConfigLog();
            configLog.setOperTime(LocalDateTime.now());
            configLog.setOperType(truncate(operType, 50));
            configLog.setOperModule(truncate(operModule, 50));
            configLog.setTargetType(truncate(targetType, 50));
            configLog.setTargetId(targetId);
            configLog.setTargetName(truncate(targetName, 200));
            configLog.setOperContent(operContent);
            // 获取当前用户
            String username = getCurrentUsername();
            configLog.setOperBy(truncate(username, 50));
            configLog.setOperName(truncate(username, 50));
            // 获取IP地址
            configLog.setIpAddress(truncate(getIpAddr(), 50));
            super.save(configLog);
        } catch (Exception e) {
            // 不抛出：保证业务不受影响，仅记录错误日志便于排查
            log.error("记录配置操作日志失败（不影响业务流程）：operType={}, operModule={}, targetType={}, targetId={}, targetName={}",
                    operType, operModule, targetType, targetId, targetName, e);
        }
    }

    /**
     * 按目标列长度截断。
     * 本日志在业务事务中被调用，字段超长会抛异常并连带回滚业务操作，故做防御性截断。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
