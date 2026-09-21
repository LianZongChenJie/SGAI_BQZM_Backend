package org.jeecg.modules.bems.lighting.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 照明模块日志用的客户端 IP 工具。
 * <p>
 * 原实现散在 {@code LightingConfigLogServiceImpl} 与 {@code LightingControlLogServiceImpl} 里各一份（代码重复），
 * 且只读 {@code X-Forwarded-For / Proxy-Client-IP / WL-Proxy-Client-IP} 三个头，**漏了 nginx 最常用的
 * {@code X-Real-IP}**，最终落到 {@code getRemoteAddr()}——生产是同机 nginx 反向代理，于是记下来的
 * 全是代理地址 {@code 127.0.0.1}。这里统一收口并修正。
 * <p>
 * 取值顺序：{@code X-Forwarded-For}（多级代理会串成 "客户端, 代理1, 代理2"，只取最左侧即最接近客户端的一个）
 * → {@code X-Real-IP} → {@code Proxy-Client-IP} → {@code WL-Proxy-Client-IP} → {@code getRemoteAddr()}。
 * <p>
 * ⚠️ 仅改代码不够：反向代理必须把客户端 IP 透传下来，例如 nginx：
 * <pre>
 * proxy_set_header X-Real-IP $remote_addr;
 * proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
 * </pre>
 * 否则后端只能看到代理自己的地址（与后端同机时就是 {@code 127.0.0.1}）。
 */
public final class LightingIpUtils {

    /**
     * 兜底 IP：非 HTTP 线程（MQ / 定时任务等）或所有头都取不到时使用
     */
    private static final String DEFAULT_IP = "127.0.0.1";

    private static final String UNKNOWN = "unknown";

    private LightingIpUtils() {
    }

    /**
     * 当前请求的客户端 IP；非 HTTP 线程（MQ、定时任务）返回 {@code 127.0.0.1}
     */
    public static String getIpAddr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return DEFAULT_IP;
        }
        String ip = firstValid(request.getHeader("X-Forwarded-For"));
        if (ip == null) {
            ip = firstValid(request.getHeader("X-Real-IP"));
        }
        if (ip == null) {
            ip = firstValid(request.getHeader("Proxy-Client-IP"));
        }
        if (ip == null) {
            ip = firstValid(request.getHeader("WL-Proxy-Client-IP"));
        }
        if (ip == null) {
            ip = firstValid(request.getRemoteAddr());
        }
        return ip == null ? DEFAULT_IP : ip;
    }

    /**
     * 取第一个有效 IP：跳过空白与 "unknown"，代理链只留最左侧一个
     */
    private static String firstValid(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String first = value.split(",")[0].trim();
        if (StringUtils.isBlank(first) || UNKNOWN.equalsIgnoreCase(first)) {
            return null;
        }
        return first;
    }

    /**
     * 当前线程绑定的请求；非 Web 线程返回 null（不抛异常）
     */
    private static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
