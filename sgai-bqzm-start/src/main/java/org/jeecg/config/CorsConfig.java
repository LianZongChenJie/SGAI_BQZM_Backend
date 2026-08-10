package org.jeecg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 CORS 跨域配置
 *
 * <p>允许任意来源（含本地 file:// 页面、不同端口的前端）调用本服务接口。
 * 本地测试页面（export_test.html）通过 fetch + blob 下载 Excel 时，预检请求 OPTIONS
 * 需要后端返回 Access-Control-Allow-* 头，否则浏览器拦截（CORS error）。</p>
 *
 * <p>要点：CorsFilter 用 FilterRegistrationBean 以最高优先级注册（先于 Shiro 过滤器链），
 * 保证 OPTIONS 预检请求在进入 Shiro 前就被放行并返回跨域头，避免预检被鉴权拦截。</p>
 *
 * @author bems
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 以最高优先级注册 CorsFilter，先于 Shiro 处理预检请求（OPTIONS）
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("Content-Disposition");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
