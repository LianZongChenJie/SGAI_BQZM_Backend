package org.jeecg.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * 全局 Jackson 序列化配置
 *
 * <p>解决雪花ID（19位Long）超出 JS Number.MAX_SAFE_INTEGER（2^53）导致前端精度丢失的问题：
 * 将包装类型 Long / BigInteger 统一序列化为字符串。</p>
 *
 * <p>注意：只注册包装类型 Long.class（实体ID、关联ID），不注册基本类型 long.class，
 * 因此 MyBatis-Plus 分页的 total/current/size/pages、Result.timestamp 等
 * 基本类型字段仍输出数字，不影响前端分页等逻辑。</p>
 *
 * @author bems
 * @date 2026-07-31
 */
@Configuration
public class JacksonConfig {

    /**
     * 包装类型 Long / BigInteger 序列化为字符串，避免雪花ID精度丢失
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(BigInteger.class, ToStringSerializer.instance);
    }
}
